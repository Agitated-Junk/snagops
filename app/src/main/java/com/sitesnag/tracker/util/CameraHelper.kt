package com.sitesnag.tracker.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraHelper {

    /**
     * Creates a temporary image file in cacheDir/camera and returns a FileProvider content Uri
     * safe for ActivityResultContracts.TakePicture().
     */
    fun createTempCameraUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply {
            if (!exists()) mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
        val tempFile = File(cameraDir, "IMG_$timeStamp.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Cleans up temporary camera files in cacheDir/camera older than 24 hours.
     */
    fun cleanTempCameraFiles(context: Context) {
        try {
            val cameraDir = File(context.cacheDir, "camera")
            if (cameraDir.exists()) {
                val dayAgo = System.currentTimeMillis() - 24 * 3600 * 1000
                cameraDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < dayAgo) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Non-fatal cleanup
        }
    }
}
