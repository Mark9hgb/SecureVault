package com.vaultapp.securevault.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.vaultapp.securevault.data.database.VideoDao
import com.vaultapp.securevault.data.database.VideoEntity
import com.vaultapp.securevault.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureVideoRepository @Inject constructor(
    private val videoDao: VideoDao,
    private val cryptoManager: CryptoManager,
    @ApplicationContext private val context: Context
) {

    private val encryptedDir: File by lazy {
        File(context.filesDir, "encrypted_videos").apply { mkdirs() }
    }

    private val thumbnailDir: File by lazy {
        File(context.filesDir, "thumbnails").apply { mkdirs() }
    }

    fun getAllVideos(): Flow<List<VideoEntity>> = videoDao.getAllVideos()

    suspend fun getVideoById(id: Long): VideoEntity? = videoDao.getVideoById(id)

    suspend fun importVideo(
        uri: Uri,
        originalFileName: String
    ): Result<VideoEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open input stream"))

            val sourceBytes = inputStream.use { it.readBytes() }
            inputStream.close()

            val encryptedResult = cryptoManager.encrypt(sourceBytes)
            if (encryptedResult.isFailure) {
                return@withContext Result.failure(encryptedResult.exceptionOrNull() ?: Exception("Encryption failed"))
            }
            val encryptedData = encryptedResult.getOrThrow()

            val encryptedFile = File(encryptedDir, "${System.currentTimeMillis()}_encrypted.bin")
            FileOutputStream(encryptedFile).use { it.write(encryptedData.ciphertext) }

            val thumbnailFile = File(thumbnailDir, "${System.currentTimeMillis()}_thumb.jpg")
            val thumbnailPath = thumbnailFile.absolutePath

            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(context, uri)

            val durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0

            val frame = metadataRetriever.getFrameAtTime(
                (durationMs / 2) * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            if (frame != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(frame, 320, 180, true)
                FileOutputStream(thumbnailFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    scaledBitmap.recycle()
                }
                frame.recycle()
            } else {
                File(thumbnailPath).createNewFile()
            }

            metadataRetriever.release()

            val videoEntity = VideoEntity(
                encryptedFilePath = encryptedFile.absolutePath,
                thumbnailPath = thumbnailPath,
                iv = encryptedData.iv,
                originalFileName = originalFileName,
                durationMs = durationMs,
                width = width,
                height = height,
                fileSizeBytes = encryptedFile.length()
            )

            val insertedId = videoDao.insertVideo(videoEntity)
            val savedEntity = videoDao.getVideoById(insertedId)
                ?: return@withContext Result.failure(IllegalStateException("Failed to retrieve inserted video"))

            Result.success(savedEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(video: VideoEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val encryptedFile = File(video.encryptedFilePath)
            val thumbnailFile = File(video.thumbnailPath)

            videoDao.deleteVideo(video)

            if (encryptedFile.exists()) {
                encryptedFile.delete()
            }
            if (thumbnailFile.exists()) {
                thumbnailFile.delete()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDecryptedInputStream(video: VideoEntity): InputStream? {
        return try {
            val encryptedFile = File(video.encryptedFilePath)
            if (!encryptedFile.exists()) return null

            val encryptedBytes = encryptedFile.readBytes()
            val result = cryptoManager.decrypt(encryptedBytes, video.iv)
            if (result.isFailure) return null
            java.io.ByteArrayInputStream(result.getOrThrow())
        } catch (e: Exception) {
            null
        }
    }
}
