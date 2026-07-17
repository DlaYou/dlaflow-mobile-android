package pl.dlaflow.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory

private const val MOBILE_IMAGE_HARD_MAX_DIMENSION = 16_384
private const val MOBILE_IMAGE_HARD_MAX_PIXELS = 64L * 1024L * 1024L
private const val MOBILE_IMAGE_DECODE_MAX_DIMENSION = 1_024
private const val MOBILE_IMAGE_DECODE_MAX_PIXELS = 1_024L * 1_024L

internal data class MobileImageDecodePlan(
    val inSampleSize: Int,
)

internal fun mobileImageDecodePlan(width: Int, height: Int): MobileImageDecodePlan? {
    if (width <= 0 || height <= 0) {
        return null
    }
    if (width > MOBILE_IMAGE_HARD_MAX_DIMENSION || height > MOBILE_IMAGE_HARD_MAX_DIMENSION) {
        return null
    }
    if (width.toLong() * height.toLong() > MOBILE_IMAGE_HARD_MAX_PIXELS) {
        return null
    }

    var sampleSize = 1
    while (true) {
        val sampledWidth = ceilDiv(width, sampleSize)
        val sampledHeight = ceilDiv(height, sampleSize)
        val sampledPixels = sampledWidth.toLong() * sampledHeight.toLong()
        if (
            sampledWidth <= MOBILE_IMAGE_DECODE_MAX_DIMENSION &&
            sampledHeight <= MOBILE_IMAGE_DECODE_MAX_DIMENSION &&
            sampledPixels <= MOBILE_IMAGE_DECODE_MAX_PIXELS
        ) {
            return MobileImageDecodePlan(inSampleSize = sampleSize)
        }
        sampleSize *= 2
    }
}

internal fun decodeMobileImageBitmap(bytes: ByteArray): Bitmap? {
    if (bytes.isEmpty()) {
        return null
    }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val plan = mobileImageDecodePlan(bounds.outWidth, bounds.outHeight) ?: return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = plan.inSampleSize
    }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    val decodedPixels = bitmap.width.toLong() * bitmap.height.toLong()
    if (
        bitmap.width > MOBILE_IMAGE_DECODE_MAX_DIMENSION ||
        bitmap.height > MOBILE_IMAGE_DECODE_MAX_DIMENSION ||
        decodedPixels > MOBILE_IMAGE_DECODE_MAX_PIXELS
    ) {
        bitmap.recycle()
        return null
    }

    return bitmap
}

private fun ceilDiv(value: Int, divisor: Int): Int {
    return ((value.toLong() + divisor.toLong() - 1L) / divisor.toLong()).toInt()
}
