package com.vaultapp.securevault.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedFilePath: String,
    val thumbnailPath: String,
    val iv: ByteArray,
    val originalFileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val importedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VideoEntity
        return id == other.id &&
            encryptedFilePath == other.encryptedFilePath &&
            thumbnailPath == other.thumbnailPath &&
            iv.contentEquals(other.iv) &&
            originalFileName == other.originalFileName &&
            durationMs == other.durationMs &&
            width == other.width &&
            height == other.height &&
            fileSizeBytes == other.fileSizeBytes &&
            importedAt == other.importedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + encryptedFilePath.hashCode()
        result = 31 * result + thumbnailPath.hashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + originalFileName.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + fileSizeBytes.hashCode()
        result = 31 * result + importedAt.hashCode()
        return result
    }
}
