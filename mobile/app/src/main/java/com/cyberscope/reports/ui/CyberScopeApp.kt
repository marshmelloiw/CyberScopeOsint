package com.cyberscope.reports.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cyberscope.reports.ui.screens.LoginScreen
import com.cyberscope.reports.ui.screens.NotificationsScreen
import com.cyberscope.reports.ui.screens.ReportsScreen
import com.cyberscope.reports.ui.viewmodel.AuthViewModel
import com.cyberscope.reports.ui.viewmodel.ReportViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Reports : Screen("reports", "Reports", Icons.Filled.Assessment)
    object Notifications : Screen("notifications", "Notifications", Icons.Filled.Notifications)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberScopeApp() {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(context.applicationContext as Application) as T
            }
        }
    )
    
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    
    if (!isAuthenticated) {
        // Show Login Screen
        LoginScreen(
            onLoginSuccess = {
                // Navigation will be handled automatically by state change
            },
            viewModel = authViewModel
        )
    } else {
        // Show Main App
        MainAppContent(authViewModel = authViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val reportViewModel: ReportViewModel = viewModel()
    val notificationViewModel: com.cyberscope.reports.ui.viewmodel.NotificationViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.cyberscope.reports.ui.viewmodel.NotificationViewModel(
                    context.applicationContext as Application
                ) as T
            }
        }
    )
    
    val userId by authViewModel.userId.collectAsState()
    
    LaunchedEffect(userId) {
        userId?.let { notificationViewModel.setUserId(it) }
    }
    
    var selectedScanId by remember { mutableStateOf<String?>(null) }
    
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("CyberScope Reports")
                        val unreadCount by notificationViewModel.unreadCount.collectAsState()
                        if (unreadCount > 0) {
                            Badge {
                                Text(unreadCount.toString())
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Reports.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportViewModel,
                    notificationViewModel = notificationViewModel,
                    modifier = Modifier
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    notificationViewModel = notificationViewModel,
                    reportViewModel = reportViewModel,
                    onNotificationClick = { scanId ->
                        selectedScanId = scanId
                        navController.navigate(Screen.Reports.route) {
                            popUpTo(Screen.Reports.route) {
                                inclusive = false
                            }
                        }
                    },
                    modifier = Modifier
                )
            }
        }
        
        // Show report details when scanId is selected from notification
        selectedScanId?.let { scanId ->
            LaunchedEffect(scanId) {
                reportViewModel.loadScanDetails(scanId)
                // Wait a bit for scan to load, then show it
                kotlinx.coroutines.delay(500)
                val scan = reportViewModel.selectedScan.value
                if (scan != null && scan.scanId == scanId) {
                    reportViewModel.selectScan(scan)
                }
                selectedScanId = null
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Reports, Screen.Notifications)
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
