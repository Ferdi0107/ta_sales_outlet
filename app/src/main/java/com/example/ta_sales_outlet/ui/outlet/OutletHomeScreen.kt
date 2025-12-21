package com.example.ta_sales_outlet.ui.outlet.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ta_sales_outlet.utils.SessionManager
import java.text.NumberFormat
import java.util.*

@Composable
fun OutletHomeScreen(navController: NavController) {
    val userName = SessionManager.userName ?: "Outlet Partner"

    // Nanti data ini diambil dari API/Database
    val limitKredit = 5000000.0
    val tagihanBelumLunas = 1200000.0
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Halo, Selamat Datang!",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { /* TODO: Notifikasi */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notif", tint = Color.White)
                }
            }
        }

        // --- BODY ---
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. KARTU INFO KEUANGAN (Penting untuk Outlet Credit)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().offset(y = (-30).dp) // Efek menumpuk ke header
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Info Keuangan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Kolom Limit
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sisa Limit Kredit", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                formatRp.format(limitKredit - tagihanBelumLunas),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF43A047), // Hijau
                                fontSize = 16.sp
                            )
                        }

                        Divider(modifier = Modifier.height(40.dp).width(1.dp))

                        // Kolom Tagihan
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Tagihan Berjalan", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                formatRp.format(tagihanBelumLunas),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100), // Orange
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // 2. MENU UTAMA
            Text("Mau apa hari ini?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Menu Belanja
                MenuCard(
                    title = "Belanja Stok",
                    icon = Icons.Default.AddShoppingCart,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // Nanti diarahkan ke Katalog Produk
                        // navController.navigate("outlet_product_catalog")
                    }
                )

                // Menu Cek Pesanan
                MenuCard(
                    title = "Pesanan Saya",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFFFFA000),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // Pindah ke tab history (atau screen khusus)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Menu Tagihan
            MenuCard(
                title = "Bayar Tagihan",
                icon = Icons.Default.AccountBalanceWallet,
                color = Color(0xFF43A047),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // Fitur Pembayaran (Upload Bukti Transfer dll)
                }
            )
        }
    }
}

@Composable
fun MenuCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}