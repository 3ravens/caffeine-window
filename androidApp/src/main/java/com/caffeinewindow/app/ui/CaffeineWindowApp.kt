package com.caffeinewindow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.caffeinewindow.app.ui.home.HomeScreen
import com.caffeinewindow.app.ui.settings.SettingsScreen

private sealed class TopLevelDestination(
  val route: String,
  val label: String,
) {
  data object Home : TopLevelDestination("home", "Home")
  data object Settings : TopLevelDestination("settings", "Settings")
}

@Composable
fun CaffeineWindowApp() {
  val navController = rememberNavController()
  val destinations = listOf(TopLevelDestination.Home, TopLevelDestination.Settings)
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  Scaffold(
    bottomBar = {
      NavigationBar {
        destinations.forEach { destination ->
          val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
          NavigationBarItem(
            selected = selected,
            onClick = {
              navController.navigate(destination.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            icon = {
              when (destination) {
                TopLevelDestination.Home -> Icon(Icons.Default.Home, contentDescription = destination.label)
                TopLevelDestination.Settings -> Icon(Icons.Default.Settings, contentDescription = destination.label)
              }
            },
            label = { Text(destination.label) },
          )
        }
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = TopLevelDestination.Home.route,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable(TopLevelDestination.Home.route) {
        HomeScreen(
          onOpenSettings = {
            navController.navigate(TopLevelDestination.Settings.route) {
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
        )
      }
      composable(TopLevelDestination.Settings.route) {
        SettingsScreen()
      }
    }
  }
}
