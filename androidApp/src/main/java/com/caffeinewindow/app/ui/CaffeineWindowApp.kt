package com.caffeinewindow.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caffeinewindow.app.ui.home.HomeScreen
import com.caffeinewindow.app.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

/**
 * Home is the root; Settings is pushed on the stack from the gear in Home's top bar.
 * A bottom bar comes back only when there are three or more real peer destinations.
 */
@Composable
fun CaffeineWindowApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(tween(durationMillis = 250)) +
                slideInHorizontally(tween(durationMillis = 250)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(durationMillis = 200)) +
                slideOutHorizontally(tween(durationMillis = 200)) { -it / 8 }
        },
        popEnterTransition = {
            fadeIn(tween(durationMillis = 250)) +
                slideInHorizontally(tween(durationMillis = 250)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(durationMillis = 200)) +
                slideOutHorizontally(tween(durationMillis = 200)) { it / 8 }
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
