package com.vaultapp.securevault.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "secure_vault_master_key"
private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
private const val TAG_LENGTH = 128

@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    init {
        if (!keyExists()) {
            generateKey()
        }
    }

    private fun keyExists(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plaintext: ByteArray): Result<EncryptedData> {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val ciphertext = cipher.doFinal(plaintext)
            Result.success(EncryptedData(ciphertext, cipher.iv))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decrypt(ciphertext: ByteArray, iv: ByteArray): Result<ByteArray> {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            Result.success(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class EncryptedData(
        val ciphertext: ByteArray,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedData
            return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }
}
