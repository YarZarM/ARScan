package com.example.rendinxr.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageDir: File
        get() = File(context.filesDir, "defect_images").apply { mkdirs() }

    private val thumbnailDir: File
        get() = File(context.filesDir, "defect_thumbnails").apply { mkdirs() }

    suspend fun saveImage(bitmap: Bitmap): ImageSaveResult = withContext(Dispatchers.IO) {
        val filename = "${UUID.randomUUID()}.jpg"

        val imageFile = File(imageDir, filename)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }

        val thumbnailFile = File(thumbnailDir, filename)
        val thumbnail = createThumbnail(bitmap)
        FileOutputStream(thumbnailFile).use { out ->
            thumbnail.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        }
        thumbnail.recycle()

        ImageSaveResult(
            imagePath = imageFile.absolutePath,
            thumbnailPath = thumbnailFile.absolutePath
        )
    }

    suspend fun deleteImage(imagePath: String, thumbnailPath: String?) = withContext(Dispatchers.IO) {
        File(imagePath).delete()
        thumbnailPath?.let { File(it).delete() }
    }

    suspend fun deleteAllImages() = withContext(Dispatchers.IO) {
        imageDir.listFiles()?.forEach { it.delete() }
        thumbnailDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun loadImage(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    private fun createThumbnail(bitmap: Bitmap, maxSize: Int = 200): Bitmap {
        val ratio = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}

data class ImageSaveResult(
    val imagePath: String,
    val thumbnailPath: String
)