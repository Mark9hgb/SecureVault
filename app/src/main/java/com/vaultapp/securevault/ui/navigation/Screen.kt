package com.vaultapp.securevault.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Vault : Screen("vault")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: Long) = "player/$videoId"
    }
}
