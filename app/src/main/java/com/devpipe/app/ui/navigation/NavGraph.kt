package com.devpipe.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.devpipe.app.ui.screen.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Sessions : Screen("sessions", "Sessions", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Logs : Screen("logs", "Logs", Icons.Default.Description)
}

private val bottomNavScreens = listOf(Screen.Dashboard, Screen.Sessions, Screen.Settings, Screen.Logs)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Sessions.route) {
                SessionsScreen(
                    onSessionClick = { id ->
                        if (id.isNotBlank()) {
                            try {
                                navController.navigate("session_detail?sessionId=${Uri.encode(id)}") {
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                // ignore navigation errors
                            }
                        }
                    },
                    onCreateClick = {
                        navController.navigate("create_session")
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Logs.route) {
                LogsScreen()
            }
            composable(
                route = "session_detail?sessionId={sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType; nullable = true })
            ) {
                SessionDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("create_session") {
                CreateSessionScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() }
                )
            }
        }
    }
}
