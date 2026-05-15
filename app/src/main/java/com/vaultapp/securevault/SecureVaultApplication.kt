package com.vaultapp.securevault

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SupportFactory
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class SecureVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
    }
}
