package com.example.ta_sales_outlet.ui.sales.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    stopId: Int, // ID Kunjungan yang dikirim dari Dashboard
    navController: NavController
) {
    // Nanti kita ambil data REAL dari Database berdasarkan stopId
    // Sekarang kita hardcode dulu untuk tes UI
    val shopName = "Toko Berkah Jaya"
    val shopAddress = "Jl. Ahmad Yani No. 12, Surabaya"
    val status = "PLANNED" // atau VISITED

    val brandColor = Color(0xFF1976D2)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Kunjungan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            // 1. HEADER INFO TOKO
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(brandColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = brandColor)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(shopName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Stop ID: #$stopId", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(Icons.Default.LocationOn, shopAddress)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(Icons.Default.Phone, "08123456789")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Menu Aktivitas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // 2. GRID MENU AKSI
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tombol Check-In
                ActionCard(
                    icon = Icons.Default.PinDrop,
                    title = "Check-In",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: GPS Checkin */ }
                )
                // Tombol Order
                ActionCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "Buat Order",
                    color = brandColor,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Buka Katalog Spesifik Toko Ini */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tombol Tagihan
                ActionCard(
                    icon = Icons.Default.Receipt,
                    title = "Tagihan",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ }
                )
                // Tombol Catatan/Laporan
                ActionCard(
                    icon = Icons.Default.EditNote,
                    title = "Catatan",
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun ActionCard(icon: ImageVector, title: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        }
    }
}