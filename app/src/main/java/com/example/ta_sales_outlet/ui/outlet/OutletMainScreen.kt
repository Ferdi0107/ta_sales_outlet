package com.example.ta_sales_outlet.ui.outlet

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ta_sales_outlet.ui.outlet.home.OutletHomeScreen
// Nanti kita buat History dan Profile, sementara pakai placeholder atau kosongkan dulu

sealed class OutletBottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : OutletBottomNavItem("outlet_home_tab", Icons.Default.Home, "Beranda")
    object History : OutletBottomNavItem("outlet_history_tab", Icons.Default.History, "Riwayat")
    object Profile : OutletBottomNavItem("outlet_profile_tab", Icons.Default.Person, "Profil")
}

@Composable
fun OutletMainScreen(rootNavController: NavController) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        OutletBottomNavItem.Home,
        OutletBottomNavItem.History,
        OutletBottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2),
                            selectedTextColor = Color(0xFF1976D2),
                            indicatorColor = Color(0xFFE3F2FD)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = OutletBottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // TAB 1: BERANDA
            composable(OutletBottomNavItem.Home.route) {
                OutletHomeScreen(rootNavController)
            }

            // TAB 2: RIWAYAT (Placeholder dulu)
            composable(OutletBottomNavItem.History.route) {
                // Nanti kita buat OutletHistoryScreen
                Text("Halaman Riwayat (Coming Soon)", modifier = Modifier.padding(16.dp))
            }

            // TAB 3: PROFIL (Placeholder dulu)
            composable(OutletBottomNavItem.Profile.route) {
                // Nanti kita buat OutletProfileScreen
                Text("Halaman Profil (Coming Soon)", modifier = Modifier.padding(16.dp))
            }
        }
    }
}