package com.example.ta_sales_outlet.ui.outlet

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ta_sales_outlet.ui.outlet.OutletHomeScreen
import com.example.ta_sales_outlet.ui.outlet.product.OutletProductCatalogScreen
import com.example.ta_sales_outlet.ui.outlet.profile.OutletProfileScreen
// Import screen history yang akan kita buat
import com.example.ta_sales_outlet.ui.outlet.history.OutletHistoryScreen

// 1. DEFINISI ITEM NAVIGASI
sealed class OutletBottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : OutletBottomNavItem("outlet_home_tab", Icons.Default.Home, "Beranda")
    object Catalog : OutletBottomNavItem("outlet_catalog_tab", Icons.Default.ShoppingCart, "Belanja")
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
        OutletBottomNavItem.Catalog,
        OutletBottomNavItem.History,
        OutletBottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
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
                            indicatorColor = Color(0xFFE3F2FD),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
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
            // --- TAB 1: BERANDA ---
            composable(OutletBottomNavItem.Home.route) {
                OutletHomeScreen(
                    navController = rootNavController,
                    onGoToCatalog = {
                        bottomNavController.navigate(OutletBottomNavItem.Catalog.route) {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoToHistory = {
                        bottomNavController.navigate(OutletBottomNavItem.History.route) {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // --- TAB 2: BELANJA (KATALOG) ---
            composable(OutletBottomNavItem.Catalog.route) {
                OutletProductCatalogScreen(navController = rootNavController)
            }

            // --- TAB 3: RIWAYAT (SUDAH DIPERBAIKI) ---
            composable(OutletBottomNavItem.History.route) {
                // Panggil Screen History disini (Code ada di bawah)
                OutletHistoryScreen(navController = rootNavController)
            }

            // --- TAB 4: PROFIL ---
            composable(OutletBottomNavItem.Profile.route) {
                OutletProfileScreen(rootNavController = rootNavController)
            }
        }
    }
}