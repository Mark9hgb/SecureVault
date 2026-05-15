package com.vaultapp.securevault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vaultapp.securevault.security.SecurityAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    securityAuthManager: SecurityAuthManager,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var authState by remember { mutableStateOf<AuthState>(AuthState.Idle) }
    val activity = androidx.compose.ui.platform.LocalContext.current as FragmentActivity
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    fun startAuth() {
        authState = AuthState.Authenticating
        scope.launch {
            try {
                val result = securityAuthManager.authenticate(
                    activity = activity,
                    title = "Unlock Secure Vault",
                    subtitle = "Authenticate to access your videos"
                )
                when (result) {
                    is SecurityAuthManager.AuthResult.Success -> {
                        authState = AuthState.Success
                        onAuthSuccess()
                    }
                    is SecurityAuthManager.AuthResult.Error -> {
                        authState = AuthState.Error(result.message)
                    }
                    SecurityAuthManager.AuthResult.Failed -> {
                        authState = AuthState.Failed
                    }
                }
            } catch (e: Exception) {
                authState = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Vault",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure Vault",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your videos are encrypted and protected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            when (val state = authState) {
                is AuthState.Idle -> {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { startAuth() }) {
                        Text("Authenticate")
                    }
                }
                is AuthState.Authenticating -> {
                    Text(
                        text = "Authenticating...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AuthState.Success -> {
                    Text(
                        text = "Authentication Successful",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                AuthState.Failed -> {
                    Text(
                        text = "Authentication Failed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { startAuth() }) {
                        Text("Retry")
                    }
                }
                is AuthState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { startAuth() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Authenticating : AuthState()
    data object Success : AuthState()
    data object Failed : AuthState()
    data class Error(val message: String) : AuthState()
}
