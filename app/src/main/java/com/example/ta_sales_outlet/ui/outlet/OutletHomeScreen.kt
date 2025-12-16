package com.example.ta_sales_outlet.ui.outlet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletHomeScreen(
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dashboard Outlet") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Halo, Pelanggan!", style = MaterialTheme.typography.headlineMedium)
            Text("Di sini nanti ada Katalog Produk & Belanja")

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onLogout) {
                Text("Keluar (Logout)")
            }
        }
    }
}