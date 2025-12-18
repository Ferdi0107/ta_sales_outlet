package com.example.ta_sales_outlet

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ta_sales_outlet.data.pref.UserPreferences
import com.example.ta_sales_outlet.ui.auth.LoginScreen
import com.example.ta_sales_outlet.ui.outlet.OutletHomeScreen
import com.example.ta_sales_outlet.ui.sales.SalesHomeScreen
import com.example.ta_sales_outlet.ui.sales.SalesMainScreen
import com.example.ta_sales_outlet.ui.theme.Ta_salesoutletTheme
import com.example.ta_sales_outlet.utils.SessionManager
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

    // 1. AMBIL SEMUA DATA LOKAL
    val isLogin by userPreferences.isLogin.collectAsState(initial = null)
    val userRole by userPreferences.userRole.collectAsState(initial = "")

    // Kita baca ID dan Name langsung dari Preferences (Tidak perlu ke DB lagi)
    val storedUserId by userPreferences.userId.collectAsState(initial = 0)
    val storedUserName by userPreferences.userName.collectAsState(initial = "")

    var isSessionReady by remember { mutableStateOf(false) }

    // 2. LOGIKA SESSION (VERSI RINGAN & CEPAT)
    LaunchedEffect(isLogin, storedUserId) {
        // Jika DataStore belum siap (masih loading awal) -> Skip
        if (isLogin == null) return@LaunchedEffect

        // Jika Belum Login -> Selesai, tampilkan Login Screen
        if (isLogin == false) {
            isSessionReady = true
            return@LaunchedEffect
        }

        // Jika Sudah Login (True)
        if (isLogin == true) {
            // Cek apakah ID tersimpan dengan benar?
            if (storedUserId != 0) {
                // SUKSES! Salin data dari HP ke Session Manager (RAM)
                SessionManager.userId = storedUserId
                SessionManager.userName = storedUserName
                SessionManager.userRole = userRole

                Log.d("APP_NAV", "Session Loaded from Local: $storedUserName ($storedUserId)")
                isSessionReady = true
            } else {
                // Login true tapi ID 0? Berarti data korup. Logout.
                Log.e("APP_NAV", "Data korup (Login true tapi ID 0). Logout.")
                userPreferences.clearSession()
                isSessionReady = true
            }
        }
    }

    // 3. NAVIGASI UI
    if (isLogin == null || (isLogin == true && !isSessionReady)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val startDest = if (isLogin == true) {
            if (userRole == "SALESPERSON") "sales_home" else "outlet_home"
        } else {
            "login"
        }

        NavHost(navController = navController, startDestination = startDest) {

            composable("login") {
                LoginScreen(onLoginSuccess = { role ->
                    val dest = if (role == "SALESPERSON") "sales_home" else "outlet_home"
                    navController.navigate(dest) { popUpTo("login") { inclusive = true } }
                })
            }

            composable("sales_home") {
                SalesMainScreen(onLogoutRoot = {
                    scope.launch {
                        // Bersihkan Session
                        SessionManager.clearSession()
                        userPreferences.clearSession()
                        navController.navigate("login") { popUpTo(0) }
                    }
                })
            }

            composable("outlet_home") {
                OutletHomeScreen(onLogout = {
                    scope.launch {
                        SessionManager.clearSession()
                        userPreferences.clearSession()
                        navController.navigate("login") { popUpTo(0) }
                    }
                })
            }
        }
    }
}