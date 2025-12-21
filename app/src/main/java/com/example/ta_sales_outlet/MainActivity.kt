package com.example.ta_sales_outlet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ta_sales_outlet.data.pref.UserPreferences
import com.example.ta_sales_outlet.ui.auth.LoginScreen
import com.example.ta_sales_outlet.ui.auth.OutletRegisterScreen
import com.example.ta_sales_outlet.ui.outlet.OutletMainScreen
import com.example.ta_sales_outlet.ui.outlet.OutletHomeScreen
import com.example.ta_sales_outlet.ui.outlet.product.OutletProductCatalogScreen
import com.example.ta_sales_outlet.ui.sales.SalesHomeScreen
import com.example.ta_sales_outlet.ui.sales.SalesMainScreen
import com.example.ta_sales_outlet.ui.theme.Ta_salesoutletTheme
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Ta_salesoutletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    // State untuk menunggu proses restore session selesai
    var isSessionRestored by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf("login") }

    // --- LOGIKA UTAMA: RESTORE SESSION ---
    LaunchedEffect(Unit) {
        // Ambil data lengkap session dari DataStore
        userPreferences.getSession().collect { user ->
            if (user.isLogin) {
                // 1. ISI ULANG SESSION MANAGER (PENTING!)
                // Agar Repository bisa baca User ID
                com.example.ta_sales_outlet.utils.SessionManager.userId = user.userId
                com.example.ta_sales_outlet.utils.SessionManager.userName = user.name
                com.example.ta_sales_outlet.utils.SessionManager.userRole = user.role

                // Debug Log
                android.util.Log.d("MAIN_ACTIVITY", "Session Restored: ID=${user.userId}")

                // 2. Tentukan tujuan berdasarkan role
                startDestination = if (user.role == "SALESPERSON") "sales_home" else "outlet_home"
            } else {
                startDestination = "login"
            }
            // 3. Tandai selesai, UI boleh muncul
            isSessionRestored = true
        }
    }

    // Tampilkan Loading jika session belum siap
    if (!isSessionRestored) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else {
        // Jika sudah siap, Tampilkan NavHost
        NavHost(navController = navController, startDestination = startDestination) {

            // Halaman 1: LOGIN
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { role ->
                        val dest = if (role == "SALESPERSON") "sales_home" else "outlet_home"
                        navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                    },
                    navController = navController
                )
            }

            composable("register_outlet") {
                OutletRegisterScreen(navController = navController)
            }

            // Halaman 2: DASHBOARD SALES
            composable("sales_home") {
                SalesMainScreen(
                    onLogoutRoot = {
                        scope.launch {
                            userPreferences.clearSession() // Hapus session di HP
                            // Reset SessionManager
                            com.example.ta_sales_outlet.utils.SessionManager.userId = 0

                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                )
            }

            // Halaman 3: DASHBOARD OUTLET
            composable("outlet_home") {
                OutletMainScreen(
                    rootNavController = navController
                )
            }

            composable("outlet_catalog") {
                // Import yang benar ke file UI di atas
                OutletProductCatalogScreen(navController = navController)
            }

            composable("outlet_cart") {
                // Panggil file UI baru tadi
                com.example.ta_sales_outlet.ui.outlet.cart.OutletCartScreen(navController = navController)
            }

            composable(
                route = "create_retur/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                com.example.ta_sales_outlet.ui.outlet.retur.CreateReturScreen(orderId, navController)
            }

            composable(
                "order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.IntType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                com.example.ta_sales_outlet.ui.outlet.order.OrderDetailScreen(orderId, navController)
            }
        }
    }
}