package com.vaultapp.securevault

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureVaultApplication : Application() {

    companion object {
        private const val TAG = "SecureVault"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            net.sqlcipher.database.SQLiteDatabase.loadLibs(this)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load SQLCipher native libraries", e)
        }
    }
}
