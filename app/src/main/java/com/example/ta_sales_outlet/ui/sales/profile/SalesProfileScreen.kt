package com.example.ta_sales_outlet.ui.sales.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SalesProfileScreen(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Halaman Profil Sales")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout")
            }
        }
    }
}