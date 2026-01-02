package com.cyberscope.reports.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyberscope.reports.ui.screens.ReportsScreen
import com.cyberscope.reports.ui.screens.ScansScreen
import com.cyberscope.reports.ui.viewmodel.ReportViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Scans : Screen("scans", "Scans", Icons.Filled.Description)
    object Reports : Screen("reports", "Reports", Icons.Filled.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberScopeApp() {
    val navController = rememberNavController()
    val viewModel: ReportViewModel = viewModel()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CyberScope Reports") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scans.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Scans.route) {
                ScansScreen(viewModel = viewModel)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Scans, Screen.Reports)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
