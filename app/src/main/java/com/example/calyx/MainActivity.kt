package com.example.calyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItemDefaults.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.calyx.ui.theme.CalyxTheme

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
                    .getInstance(application)
            )

            CalyxTheme {
                CalyxApp(viewModel)
            }
        }
    }
}

@Composable
fun CalyxApp(viewModel: MainViewModel) {
    val state         = viewModel.state.collectAsState().value
    val navController = rememberNavController()

    CalyxTheme(darkTheme = state.isDarkMode) {
        val navItems = listOf(
            NavItem("dashboard", "Dashboard", Icons.Filled.Dashboard,    Icons.Outlined.Dashboard),
            NavItem("alerts",    "Alerts",    Icons.Filled.Notifications, Icons.Outlined.Notifications),
            NavItem("settings",  "Settings",  Icons.Filled.Settings,      Icons.Outlined.Settings)
        )

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {

                    colors(
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                        indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label  = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Color(0xFF1B5E20),
                                selectedTextColor   = Color(0xFF1B5E20),
                                indicatorColor      = Color(0xFFA5D6A7),
                                unselectedIconColor = Color(0xFF78909C),
                                unselectedTextColor = Color(0xFF78909C)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = "dashboard",
                modifier         = Modifier.padding(innerPadding),
                enterTransition  = { fadeIn(tween(300)) + slideInVertically { it / 20 } },
                exitTransition   = { fadeOut(tween(200)) }
            ) {
                composable("dashboard") { DashboardScreen(viewModel) }
                composable("alerts")    { AlertsScreen(viewModel) }
                composable("settings")  { SettingsScreen(viewModel) }
            }
        }
    }
}