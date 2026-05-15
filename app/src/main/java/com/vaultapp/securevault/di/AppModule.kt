package com.vaultapp.securevault.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vaultapp.securevault.data.database.VaultDatabase
import com.vaultapp.securevault.data.database.VideoDao
import com.vaultapp.securevault.media.EncryptedVideoDataSource
import com.vaultapp.securevault.security.CryptoManager
import com.vaultapp.securevault.security.EnvironmentChecker
import com.vaultapp.securevault.security.SecurityAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }

    @Provides
    @Singleton
    fun provideSecurityAuthManager(@ApplicationContext context: Context): SecurityAuthManager {
        return SecurityAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideEnvironmentChecker(@ApplicationContext context: Context): EnvironmentChecker {
        return EnvironmentChecker(context)
    }

    @Provides
    @Singleton
    fun provideVaultDatabase(
        @ApplicationContext context: Context
    ): VaultDatabase {
        return Room.databaseBuilder(
            context,
            VaultDatabase::class.java,
            "secure_vault.db"
        )
            .openHelperFactory(
                net.sqlcipher.database.SupportFactory(
                    net.sqlcipher.database.SQLiteDatabase.getBytes("vault-master-key-2024-secure".toCharArray())
                )
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("PRAGMA cipher_default_kdf_iter = 256000")
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: VaultDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    @Singleton
    fun provideEncryptedVideoDataSourceFactory(
        cryptoManager: CryptoManager
    ): EncryptedVideoDataSource.Factory {
        return EncryptedVideoDataSource.Factory(cryptoManager)
    }
}
