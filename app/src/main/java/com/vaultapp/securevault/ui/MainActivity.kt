package com.vaultapp.securevault.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vaultapp.securevault.media.EncryptedVideoDataSource
import com.vaultapp.securevault.security.SecurityAuthManager
import com.vaultapp.securevault.ui.navigation.AppNavigation
import com.vaultapp.securevault.ui.theme.SecureVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securityAuthManager: SecurityAuthManager

    @Inject
    lateinit var dataSourceFactory: EncryptedVideoDataSource.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        securityAuthManager = securityAuthManager,
                        dataSourceFactory = dataSourceFactory
                    )
                }
            }
        }
    }
}
