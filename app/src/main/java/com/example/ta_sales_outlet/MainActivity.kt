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
import com.example.ta_sales_outlet.ui.outlet.OutletHomeScreen
import com.example.ta_sales_outlet.ui.sales.SalesHomeScreen
import com.example.ta_sales_outlet.ui.sales.SalesMainScreen
import com.example.ta_sales_outlet.ui.theme.Ta_salesoutletTheme
import kotlinx.coroutines.launch

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

    // 1. Cek Status Login dari DataStore
    // collectAsState akan memantau perubahan data secara real-time
    val isLogin by userPreferences.isLogin.collectAsState(initial = false)
    val userRole by userPreferences.userRole.collectAsState(initial = "")

    // Tentukan halaman awal: Jika login -> Cek Role, Jika belum -> Login
    val startDestination = if (isLogin) {
        if (userRole == "SALESPERSON") "sales_home" else "outlet_home"
    } else {
        "login"
    }

    // NavHost adalah wadah untuk gonta-ganti halaman
    NavHost(navController = navController, startDestination = startDestination) {

        // Halaman 1: LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = if (role == "SALESPERSON") "sales_home" else "outlet_home"
                    navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                },
                navController = navController // <--- TAMBAHKAN INI
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
                        userPreferences.clearSession()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }

        // Halaman 3: DASHBOARD OUTLET
        composable("outlet_home") {
            OutletHomeScreen(
                onLogout = {
                    scope.launch {
                        userPreferences.clearSession()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}