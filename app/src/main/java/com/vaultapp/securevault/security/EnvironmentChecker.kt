package com.vaultapp.securevault.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentChecker @Inject constructor(
    private val context: Context
) {

    data class EnvironmentStatus(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggable: Boolean,
        val warnings: List<String>
    )

    fun checkEnvironment(): EnvironmentStatus {
        val warnings = mutableListOf<String>()
        val isRooted = checkRoot()
        val isEmulator = checkEmulator()
        val isDebuggable = checkDebuggable()

        if (isRooted) {
            warnings.add("Device appears to be rooted. Security may be compromised.")
        }
        if (isEmulator) {
            warnings.add("Running on an emulator. Production use is not recommended.")
        }
        if (isDebuggable) {
            warnings.add("Application is debuggable. This is not secure.")
        }

        return EnvironmentStatus(
            isRooted = isRooted,
            isEmulator = isEmulator,
            isDebuggable = isDebuggable,
            warnings = warnings
        )
    }

    private fun checkRoot(): Boolean {
        val rootBinaries = listOf(
            "su", "busybox", "supersu", "Superuser.apk", "KingoUser.apk",
            "SuperSu.apk", "magisk", "magiskhide"
        )

        val systemPaths = listOf(
            "/system/app/",
            "/system/bin/",
            "/system/sbin/",
            "/system/xbin/",
            "/vendor/bin/",
            "/sbin/",
            "/su/bin/"
        )

        for (binary in rootBinaries) {
            for (path in systemPaths) {
                if (File("$path$binary").exists()) {
                    return true
                }
            }
        }

        val buildTags = Build.TAGS ?: ""
        if (buildTags.contains("test-keys")) {
            return true
        }

        return false
    }

    private fun checkEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val brand = Build.BRAND ?: ""
        val hardware = Build.HARDWARE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.contains("generic") ||
            fingerprint.contains("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK") ||
            manufacturer.contains("Genymotion") ||
            manufacturer.equals("unknown", ignoreCase = true) ||
            brand.startsWith("generic") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            product.contains("sdk") ||
            product.contains("sdk_x86") ||
            product.contains("vbox86p") ||
            product.contains("emulator") ||
            product.contains("simulator")
    }

    private fun checkDebuggable(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }
}
