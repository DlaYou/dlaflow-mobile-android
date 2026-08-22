package pl.dlaflow.mobile

import android.graphics.Bitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Properties
import java.util.concurrent.FutureTask
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_MEMORY_MAX_BYTES = 6L * 1024L * 1024L
private const val DEFAULT_MEMORY_MAX_ENTRIES = 64
private const val DEFAULT_DISK_MAX_BYTES = 24L * 1024L * 1024L
private const val DEFAULT_DISK_MAX_ENTRIES = 128
private const val DEFAULT_MEDIA_TTL_MILLIS = 60L * 60L * 1_000L
private const val MAX_CACHE_ENTRY_BYTES = 8L * 1024L * 1024L

internal data class MobileImageCacheStats(
    val hits: Long,
    val misses: Long,
    val revalidations: Long,
    val failures: Long,
)

internal class MobileImageCache(
    cacheDir: File,
    sessionScope: String,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val memoryMaxBytes: Long = DEFAULT_MEMORY_MAX_BYTES,
    private val memoryMaxEntries: Int = DEFAULT_MEMORY_MAX_ENTRIES,
    private val diskMaxBytes: Long = DEFAULT_DISK_MAX_BYTES,
    private val diskMaxEntries: Int = DEFAULT_DISK_MAX_ENTRIES,
) {
    private data class Entry(
        val bytes: ByteArray,
        val etag: String?,
        val expiresAtMillis: Long,
    )

    private val lock = Any()
    private val memory = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private val inFlight = mutableMapOf<String, FutureTask<ByteArray?>>()
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val revalidations = AtomicLong(0)
    private val failures = AtomicLong(0)
    private var memoryBytes = 0L
    private var generation = 0L
    private val sessionDirectory = File(cacheDir, "mobile-image-cache/${sha256Hex(sessionScope)}")

    init {
        require(memoryMaxBytes > 0L) { "Memory cache byte limit must be positive." }
        require(memoryMaxEntries > 0) { "Memory cache entry limit must be positive." }
        require(diskMaxBytes >= 0L) { "Disk cache byte limit cannot be negative." }
        require(diskMaxEntries >= 0) { "Disk cache entry limit cannot be negative." }
        sessionDirectory.parentFile?.let { root ->
            root.listFiles()?.filter { it.isDirectory && it != sessionDirectory }?.forEach { it.deleteRecursively() }
        }
    }

    fun get(
        pathWithQuery: String,
        fetch: (ifNoneMatch: String?) -> MobileMediaResponse?,
    ): ByteArray? = get(pathWithQuery, persistToDisk = true, fetch = fetch)

    fun get(
        pathWithQuery: String,
        persistToDisk: Boolean,
        fetch: (ifNoneMatch: String?) -> MobileMediaResponse?,
    ): ByteArray? {
        require(pathWithQuery.isNotBlank()) { "Media cache key must not be blank." }
        val task: FutureTask<ByteArray?>
        var owner = false

        synchronized(lock) {
            memory[pathWithQuery]?.takeIf { isFresh(it, nowMillis()) }?.let { entry ->
                hits.incrementAndGet()
                return entry.bytes.copyOf()
            }

            task = inFlight[pathWithQuery] ?: FutureTask {
                load(pathWithQuery, fetch, persistToDisk)
            }.also {
                inFlight[pathWithQuery] = it
                misses.incrementAndGet()
                owner = true
            }
        }

        if (owner) {
            task.run()
        }

        return try {
            task.get()?.copyOf()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (_: ExecutionException) {
            failures.incrementAndGet()
            null
        } finally {
            if (owner) {
                synchronized(lock) {
                    if (inFlight[pathWithQuery] === task) {
                        inFlight.remove(pathWithQuery)
                    }
                }
            }
        }
    }

    fun stats(): MobileImageCacheStats = MobileImageCacheStats(
        hits = hits.get(),
        misses = misses.get(),
        revalidations = revalidations.get(),
        failures = failures.get(),
    )

    fun clear() {
        synchronized(lock) {
            generation += 1L
            memory.clear()
            memoryBytes = 0L
        }
        if (sessionDirectory.exists()) {
            sessionDirectory.deleteRecursively()
        }
    }

    fun invalidate(pathWithQuery: String) {
        synchronized(lock) {
            memory.remove(pathWithQuery)?.let { entry -> memoryBytes -= entry.bytes.size.toLong() }
        }
        if (diskEnabled()) {
            val key = diskKey(pathWithQuery)
            File(sessionDirectory, "$key.bin").delete()
            File(sessionDirectory, "$key.properties").delete()
        }
    }

    companion object {
        fun clearAll(cacheDir: File) {
            File(cacheDir, "mobile-image-cache").deleteRecursively()
        }
    }

    private fun load(
        pathWithQuery: String,
        fetch: (ifNoneMatch: String?) -> MobileMediaResponse?,
        persistToDisk: Boolean,
    ): ByteArray? {
        val startGeneration = synchronized(lock) { generation }
        val now = nowMillis()
        val memoryEntry = synchronized(lock) { memory[pathWithQuery] }
        val diskEntry = if (diskEnabled() && persistToDisk) readDiskEntry(pathWithQuery) else null
        val staleEntry = diskEntry ?: memoryEntry
        if (staleEntry != null && isFresh(staleEntry, now)) {
            putMemory(pathWithQuery, staleEntry)
            hits.incrementAndGet()
            return staleEntry.bytes
        }

        val response = runCatching { fetch(staleEntry?.etag) }.getOrNull()
        if (response == null) {
            failures.incrementAndGet()
            return null
        }

        if (response.notModified && staleEntry != null) {
            val refreshed = staleEntry.copy(
                etag = response.etag ?: staleEntry.etag,
                expiresAtMillis = expiresAt(now, response.maxAgeMillis),
            )
            store(pathWithQuery, refreshed, startGeneration, persistToDisk)
            revalidations.incrementAndGet()
            return refreshed.bytes
        }

        val bytes = response.bytes
        if (bytes == null || bytes.isEmpty() || bytes.size.toLong() > MAX_CACHE_ENTRY_BYTES) {
            failures.incrementAndGet()
            return null
        }

        val entry = Entry(
            bytes = bytes.copyOf(),
            etag = response.etag,
            expiresAtMillis = expiresAt(now, response.maxAgeMillis),
        )
        store(pathWithQuery, entry, startGeneration, persistToDisk)
        return entry.bytes
    }

    private fun store(pathWithQuery: String, entry: Entry, expectedGeneration: Long, persistToDisk: Boolean) {
        synchronized(lock) {
            if (generation != expectedGeneration) {
                return
            }
            putMemory(pathWithQuery, entry)
            if (diskEnabled() && persistToDisk) {
                writeDiskEntry(pathWithQuery, entry)
            }
        }
    }

    private fun putMemory(pathWithQuery: String, entry: Entry) {
        memory.remove(pathWithQuery)?.let { previous -> memoryBytes -= previous.bytes.size.toLong() }
        memory[pathWithQuery] = entry
        memoryBytes += entry.bytes.size.toLong()
        while (memory.size > memoryMaxEntries || memoryBytes > memoryMaxBytes) {
            val eldest = memory.entries.firstOrNull() ?: break
            memory.remove(eldest.key)
            memoryBytes -= eldest.value.bytes.size.toLong()
        }
    }

    private fun readDiskEntry(pathWithQuery: String): Entry? {
        val key = diskKey(pathWithQuery)
        val bytesFile = File(sessionDirectory, "$key.bin")
        val metaFile = File(sessionDirectory, "$key.properties")
        if (!bytesFile.isFile || !metaFile.isFile || bytesFile.length() <= 0L || bytesFile.length() > MAX_CACHE_ENTRY_BYTES) {
            bytesFile.delete()
            metaFile.delete()
            return null
        }

        val properties = runCatching {
            Properties().also { loaded ->
                FileInputStream(metaFile).use(loaded::load)
            }
        }.getOrNull() ?: run {
            bytesFile.delete()
            metaFile.delete()
            return null
        }
        val expiresAt = properties.getProperty("expiresAtMillis")?.toLongOrNull() ?: run {
            bytesFile.delete()
            metaFile.delete()
            return null
        }
        val bytes = runCatching { bytesFile.readBytes() }.getOrNull() ?: run {
            bytesFile.delete()
            metaFile.delete()
            return null
        }
        if (bytes.isEmpty() || bytes.size.toLong() > MAX_CACHE_ENTRY_BYTES) {
            bytesFile.delete()
            metaFile.delete()
            return null
        }
        bytesFile.setLastModified(nowMillis())
        return Entry(
            bytes = bytes,
            etag = properties.getProperty("etag")?.takeIf { it.isNotBlank() },
            expiresAtMillis = expiresAt,
        )
    }

    private fun writeDiskEntry(pathWithQuery: String, entry: Entry) {
        if (!sessionDirectory.exists() && !sessionDirectory.mkdirs()) {
            return
        }
        val key = diskKey(pathWithQuery)
        val bytesFile = File(sessionDirectory, "$key.bin")
        val metaFile = File(sessionDirectory, "$key.properties")
        val tempBytesFile = File(sessionDirectory, "$key.bin.tmp")
        val tempMetaFile = File(sessionDirectory, "$key.properties.tmp")
        runCatching {
            FileOutputStream(tempBytesFile).use { output -> output.write(entry.bytes) }
            val properties = Properties().apply {
                setProperty("expiresAtMillis", entry.expiresAtMillis.toString())
                entry.etag?.let { setProperty("etag", it) }
            }
            FileOutputStream(tempMetaFile).use { output -> properties.store(output, null) }
            if (!tempBytesFile.renameTo(bytesFile)) {
                tempBytesFile.copyTo(bytesFile, overwrite = true)
                tempBytesFile.delete()
            }
            if (!tempMetaFile.renameTo(metaFile)) {
                tempMetaFile.copyTo(metaFile, overwrite = true)
                tempMetaFile.delete()
            }
            evictDiskEntries()
        }.onFailure {
            tempBytesFile.delete()
            tempMetaFile.delete()
        }
    }

    private fun evictDiskEntries() {
        if (!diskEnabled()) return
        val files = sessionDirectory.listFiles { file -> file.extension == "bin" }.orEmpty()
        var totalBytes = files.sumOf { it.length() }
        var remaining = files.toMutableList()
        while (remaining.size > diskMaxEntries || totalBytes > diskMaxBytes) {
            val eldest = remaining.minByOrNull { it.lastModified() } ?: break
            totalBytes -= eldest.length()
            File(sessionDirectory, "${eldest.nameWithoutExtension}.properties").delete()
            eldest.delete()
            remaining = remaining.filterNot { it == eldest }.toMutableList()
        }
    }

    private fun diskEnabled(): Boolean = diskMaxBytes > 0L && diskMaxEntries > 0

    private fun isFresh(entry: Entry, now: Long): Boolean = entry.expiresAtMillis > now

    private fun expiresAt(now: Long, maxAgeMillis: Long?): Long {
        val ttl = (maxAgeMillis ?: DEFAULT_MEDIA_TTL_MILLIS).coerceAtLeast(0L)
        return now + ttl.coerceAtMost(Long.MAX_VALUE - now)
    }

    private fun diskKey(pathWithQuery: String): String = sha256Hex(pathWithQuery)
}

private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }

internal class MobileImageLoader(
    private val apiUrl: String,
    private val mobileMediaClient: MobileApiClient,
    private val mobileToken: String,
    private val cache: MobileImageCache,
) {
    suspend fun load(url: String, targetMaxDimension: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank() || targetMaxDimension <= 0) {
            return@withContext null
        }
        val canonicalUrl = resolveMobileMediaPath(apiUrl, url) ?: return@withContext null
        val bytes = cache.get(canonicalUrl, fetch = { etag ->
            mobileMediaClient.getMobileMediaResponse(
                token = mobileToken,
                pathWithQuery = canonicalUrl,
                ifNoneMatch = etag,
            )
        }, persistToDisk = canonicalUrl.contains("variant=thumb", ignoreCase = true)) ?: return@withContext null
        decodeMobileImageBitmap(bytes, targetMaxDimension) ?: run {
            cache.invalidate(canonicalUrl)
            null
        }
    }

    suspend fun prefetch(urls: List<String>) {
        withContext(Dispatchers.IO) {
            mobileThumbnailPrefetchUrls(urls).forEach { url ->
                load(url, targetMaxDimension = 192)?.recycle()
            }
        }
    }

}

internal fun mobileThumbnailPrefetchUrls(urls: List<String>, maxItems: Int = 6): List<String> {
    if (maxItems <= 0) return emptyList()
    return urls
        .asSequence()
        .filter { url -> url.contains("variant=thumb", ignoreCase = true) }
        .filter { it.isNotBlank() }
        .distinct()
        .take(maxItems)
        .toList()
}

internal fun mobileImageCacheSessionScope(baseUrl: String, deviceId: String, token: String): String =
    listOf(baseUrl.trim(), deviceId.trim(), sha256Hex(token)).joinToString("\n")
