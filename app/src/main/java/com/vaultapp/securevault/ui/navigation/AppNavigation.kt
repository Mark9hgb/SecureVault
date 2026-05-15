package com.vaultapp.securevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaultapp.securevault.data.SecureVideoRepository
import com.vaultapp.securevault.media.EncryptedVideoDataSource
import com.vaultapp.securevault.security.SecurityAuthManager
import com.vaultapp.securevault.ui.screens.AuthScreen
import com.vaultapp.securevault.ui.screens.VaultDashboardScreen
import com.vaultapp.securevault.ui.screens.VideoPlayerScreen
import com.vaultapp.securevault.ui.viewmodel.PlayerViewModel
import com.vaultapp.securevault.ui.viewmodel.VaultViewModel
import kotlinx.coroutines.runBlocking

@Composable
fun AppNavigation(
    securityAuthManager: SecurityAuthManager,
    dataSourceFactory: EncryptedVideoDataSource.Factory,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var isAuthenticated by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        AuthScreen(
            securityAuthManager = securityAuthManager,
            onAuthSuccess = { isAuthenticated = true }
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = Screen.Vault.route,
            modifier = modifier
        ) {
            composable(Screen.Vault.route) {
                val vaultViewModel: VaultViewModel = hiltViewModel()
                VaultDashboardScreen(
                    viewModel = vaultViewModel,
                    onVideoClick = { video ->
                        navController.navigate(Screen.Player.createRoute(video.id))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
                val playerViewModel: PlayerViewModel = hiltViewModel()
                val vaultViewModel: VaultViewModel = hiltViewModel()
                val videos by vaultViewModel.videos.collectAsState()
                val video = videos.find { it.id == videoId }

                if (video != null) {
                    VideoPlayerScreen(
                        video = video,
                        dataSourceFactory = dataSourceFactory,
                        viewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
