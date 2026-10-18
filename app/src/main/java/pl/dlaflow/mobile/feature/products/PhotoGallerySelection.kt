package pl.dlaflow.mobile.feature.products

import android.content.Intent
import android.net.Uri

internal fun galleryUrisFromIntent(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    val clipUriStrings = buildList {
        intent.clipData?.let { clipData ->
            repeat(clipData.itemCount) { index ->
                clipData.getItemAt(index).uri?.toString()?.let(::add)
            }
        }
    }
    return galleryUriStrings(intent.data?.toString(), clipUriStrings).map(Uri::parse)
}

internal fun createGalleryIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "image/*"
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
}

internal fun galleryUriStrings(primary: String?, clipUris: List<String>): List<String> =
    (if (clipUris.isNotEmpty()) clipUris else listOfNotNull(primary)).distinct()

internal fun <T> limitGalleryUris(uris: List<T>, remainingSlots: Int): List<T> =
    if (remainingSlots <= 0) emptyList() else uris.distinct().take(remainingSlots)
