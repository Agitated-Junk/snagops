package com.sitesnag.tracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageCompressor {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 75

    /**
     * Resizes and compresses an image from Uri or File into app-scoped storage.
     * Max longest edge: 1024px, JPEG quality: 75 (~100-150 KB).
     * Automatically handles EXIF rotation.
     */
    suspend fun compressAndSaveImage(
        context: Context,
        sourceUri: Uri,
        fileNamePrefix: String = "photo"
    ): String = withContext(Dispatchers.IO) {
        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        val targetFile = File(photosDir, "${fileNamePrefix}_${UUID.randomUUID()}.jpg")

        // 1. Decode bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        val origWidth = options.outWidth
        val origHeight = options.outHeight

        // 2. Compute sample size
        var sampleSize = 1
        val maxDim = maxOf(origWidth, origHeight)
        if (maxDim > MAX_DIMENSION) {
            sampleSize = Math.round(maxDim.toFloat() / MAX_DIMENSION.toFloat())
        }

        // 3. Decode bitmap with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap: Bitmap? = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode bitmap from source uri: $sourceUri")

        // 4. Handle EXIF rotation
        val rotationAngle = getExifRotation(context, sourceUri)
        if (rotationAngle != 0) {
            val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        // 5. Scale precisely to max 1024 if still larger
        val curMaxDim = maxOf(bitmap!!.width, bitmap.height)
        if (curMaxDim > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / curMaxDim.toFloat()
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            if (scaled != bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        // 6. Write to app-scoped file with JPEG quality 75
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
        }
        bitmap.recycle()

        targetFile.absolutePath
    }

    /**
     * Compresses a direct Bitmap into app-scoped storage.
     */
    suspend fun compressBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        fileNamePrefix: String = "photo"
    ): String = withContext(Dispatchers.IO) {
        val photosDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
        val targetFile = File(photosDir, "${fileNamePrefix}_${UUID.randomUUID()}.jpg")

        var bitmap = sourceBitmap
        val curMaxDim = maxOf(bitmap.width, bitmap.height)
        if (curMaxDim > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / curMaxDim.toFloat()
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        }

        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.flush()
        }

        targetFile.absolutePath
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
