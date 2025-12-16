package com.example.ta_sales_outlet.ui.sales

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ta_sales_outlet.ui.sales.catalog.ProductCatalogScreen
import com.example.ta_sales_outlet.ui.sales.history.SalesHistoryScreen
import com.example.ta_sales_outlet.ui.sales.profile.SalesProfileScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.ta_sales_outlet.ui.sales.visit.VisitDetailScreen

// Definisi Menu Navigasi
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("sales_home_tab", Icons.Default.Home, "Beranda")
    object Catalog : BottomNavItem("sales_catalog_tab", Icons.Default.ShoppingBag, "Katalog")
    object History : BottomNavItem("sales_history_tab", Icons.Default.ReceiptLong, "Riwayat")
    object Profile : BottomNavItem("sales_profile_tab", Icons.Default.Person, "Profil")
}

@Composable
fun SalesMainScreen(
    onLogoutRoot: () -> Unit // Callback logout diteruskan dari MainActivity
) {
    val bottomNavController = rememberNavController()

    // Daftar Menu
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Catalog,
        BottomNavItem.History,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                // Agar saat back tidak menumpuk stack, kembali ke Home dulu
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1976D2),
                            indicatorColor = Color(0xFF1976D2).copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // NavHost KHUSUS SALES (Nested Navigation)
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab HOME (Ubah sedikit untuk kirim navController)
            composable(BottomNavItem.Home.route) {
                // Kita perlu pass navController ke SalesHomeScreen agar dia bisa pindah halaman
                SalesHomeScreen(
                    navController = bottomNavController, // <--- TAMBAHKAN INI
                    onLogout = { /* ... */ }
                )
            }

            // ... Tab Catalog, History, Profile tetap sama ...

            // HALAMAN BARU: DETAIL KUNJUNGAN
            // Menerima argumen 'stopId'
            composable(
                route = "visit_detail/{stopId}",
                arguments = listOf(navArgument("stopId") { type = NavType.IntType })
            ) { backStackEntry ->
                // Ambil ID yang dikirim
                val stopId = backStackEntry.arguments?.getInt("stopId") ?: 0
                VisitDetailScreen(stopId = stopId, navController = bottomNavController)
            }
        }
    }
}