package com.vaultapp.securevault.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.vaultapp.securevault.security.CryptoManager
import java.io.IOException
import java.io.InputStream
import java.io.File
import javax.crypto.CipherInputStream
import javax.crypto.spec.GCMParameterSpec

class EncryptedVideoDataSource(
    private val cryptoManager: CryptoManager,
    listener: TransferListener? = null
) : BaseDataSource(/* isNetwork = */ false, listener) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var uri: Uri? = null

    class Factory(
        private val cryptoManager: CryptoManager
    ) : androidx.media3.datasource.DataSource.Factory {

        private var transferListener: TransferListener? = null

        fun setTransferListener(listener: TransferListener?): Factory {
            this.transferListener = listener
            return this
        }

        override fun createDataSource(): EncryptedVideoDataSource {
            return EncryptedVideoDataSource(cryptoManager, transferListener)
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri

        val path = dataSpec.uri.path ?: throw IOException("Uri path is null")
        val file = File(path)

        if (!file.exists()) {
            throw IOException("Encrypted file not found: $path")
        }

        val ivPath = "$path.iv"
        val ivFile = File(ivPath)

        if (!ivFile.exists()) {
            throw IOException("IV file not found: $ivPath")
        }

        val iv = ivFile.readBytes()
        val encryptedBytes = file.readBytes()

        val decryptedBytes = try {
            cryptoManager.decrypt(encryptedBytes, iv)
        } catch (e: Exception) {
            throw IOException("Decryption failed", e)
        }

        inputStream = java.io.ByteArrayInputStream(decryptedBytes)

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            decryptedBytes.size.toLong() - dataSpec.position
        }

        if (dataSpec.position > 0) {
            inputStream?.skip(dataSpec.position)
        }

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }

        val bytesRead = inputStream?.read(buffer, offset, bytesToRead) ?: C.RESULT_END_OF_INPUT

        if (bytesRead > 0) {
            bytesRemaining -= bytesRead
            bytesTransferred(bytesRead)
        }

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            bytesRemaining = 0
        }

        return bytesRead
    }

    override fun getUri(): Uri? = uri

    @Throws(IOException::class)
    override fun close() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            throw e
        } finally {
            inputStream = null
            if (uri != null) {
                transferEnded()
                uri = null
            }
        }
    }
}
