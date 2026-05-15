package com.vaultapp.securevault.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class SecurityAuthManager @Inject constructor(
    private val context: Context
) {

    private val biometricManager: BiometricManager = BiometricManager.from(context)

    fun getAuthenticationStatus(): Int {
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }

    fun canAuthenticate(): Boolean {
        return getAuthenticationStatus() == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getErrorDescription(status: Int): String {
        return when (status) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "No biometric hardware available. Device PIN/pattern will be used."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Biometric hardware is currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No biometric credentials enrolled. Please set up a device PIN or biometric."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Security update required for biometric authentication."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
                "Biometric authentication is not supported on this device."
            else -> "Unknown authentication error."
        }
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate to Unlock Vault",
        subtitle: String = "Use your device credentials"
    ): AuthResult = suspendCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(AuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    continuation.resume(AuthResult.Error(errorCode, errString.toString()))
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    continuation.resume(AuthResult.Failed)
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val errorCode: Int, val message: String) : AuthResult()
        data object Failed : AuthResult()
    }
}
