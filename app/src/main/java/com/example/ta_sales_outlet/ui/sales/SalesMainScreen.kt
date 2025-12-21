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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ta_sales_outlet.ui.sales.catalog.ProductCatalogScreen
import com.example.ta_sales_outlet.ui.sales.history.TransactionHistoryScreen
import com.example.ta_sales_outlet.ui.sales.profile.SalesProfileScreen
import com.example.ta_sales_outlet.ui.sales.visit.VisitDetailScreen
import com.example.ta_sales_outlet.ui.sales.history.TransactionDetailScreen
import com.example.ta_sales_outlet.ui.sales.cart.CartScreen

// Definisi Menu Navigasi
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("sales_home_tab", Icons.Default.Home, "Beranda")
    object Catalog : BottomNavItem("sales_catalog_tab", Icons.Default.ShoppingBag, "Katalog")
    object History : BottomNavItem("sales_history", Icons.Default.ReceiptLong, "Riwayat")
    object Profile : BottomNavItem("sales_profile_tab", Icons.Default.Person, "Profil")
}

@Composable
fun SalesMainScreen(
    onLogoutRoot: () -> Unit
) {
    val bottomNavController = rememberNavController()

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
                    // Logic agar icon menyala sesuai halaman aktif
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                // Agar tidak menumpuk stack saat back
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

        // --- NAV HOST: PETA UTAMA SALES ---
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // 1. TAB HOME (BERANDA)
            composable(BottomNavItem.Home.route) {
                SalesHomeScreen(
                    navController = bottomNavController, // Kirim nav controller agar bisa pindah ke Detail
                    onLogout = { /* Tidak dipakai di sini lagi */ }
                )
            }

            // 2. TAB CATALOG (PRODUK) -> Inilah yang tadi error (Missing)
            composable(BottomNavItem.Catalog.route) {
                // Masukkan parameter navController = bottomNavController
                ProductCatalogScreen(navController = bottomNavController)
            }

            composable("cart_checkout") {
                CartScreen(navController = bottomNavController)
            }

            // 3. TAB HISTORY (RIWAYAT)
            composable(BottomNavItem.History.route) {
                TransactionHistoryScreen(navController = bottomNavController)
            }

            // 4. TAB PROFILE
            composable(BottomNavItem.Profile.route) {
                SalesProfileScreen(onLogout = onLogoutRoot)
            }

            // 5. HALAMAN TAMBAHAN: DETAIL KUNJUNGAN
            // (Tidak ada di Bottom Bar, tapi bisa diakses dari Home)
            composable(
                route = "visit_detail/{stopId}",
                arguments = listOf(navArgument("stopId") { type = NavType.IntType })
            ) { backStackEntry ->
                val stopId = backStackEntry.arguments?.getInt("stopId") ?: 0
                VisitDetailScreen(stopId = stopId, navController = bottomNavController)
            }

            composable(
                route = "history_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                TransactionDetailScreen(orderId = orderId, navController = bottomNavController)
            }
        }
    }
}