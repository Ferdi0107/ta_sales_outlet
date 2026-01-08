package com.example.ta_sales_outlet.ui.outlet.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.model.OrderDetailItem
import com.example.ta_sales_outlet.data.repository.HistoryRepository
import com.example.ta_sales_outlet.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import android.widget.Toast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.platform.LocalContext
import com.example.ta_sales_outlet.utils.UrlHelper
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.filled.BrokenImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var headerData by remember { mutableStateOf<OrderHistory?>(null) }
    var itemsData by remember { mutableStateOf<List<OrderDetailItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // State loading khusus tombol konfirmasi
    var isVerifying by remember { mutableStateOf(false) }

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val header = HistoryRepository.getOrderHeader(orderId)
            val items = HistoryRepository.getOrderItems(orderId)
            withContext(Dispatchers.Main) {
                headerData = header
                itemsData = items
                isLoading = false
            }
        }
    }

    // --- 2. LAUNCHED EFFECT DIGANTI DENGAN KODE BARU ANDA ---
    LaunchedEffect(orderId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pesanan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (headerData != null) {
                val status = headerData!!.status
                val payment = headerData!!.paymentMethod.uppercase() // CASH / CREDIT

                // Hanya tampilkan Bottom Bar jika statusnya relevan
                if (status == "SHIPPED" || status == "DELIVERED" || status == "RECEIVED" || status == "RETURN_REQUESTED" || status == "VERIFIED") {

                    Surface(shadowElevation = 16.dp, color = Color.White) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // --- KASUS 1: BARANG SAMPAI (BELUM DIPUTUSKAN) ---
                            // User harus memilih: RETUR atau KONFIRMASI
                            if (status == "SHIPPED" || status == "DELIVERED" || status == "RECEIVED") {

                                Text(
                                    text = "Silakan cek barang sebelum konfirmasi.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Jarak antar tombol
                                ) {
                                    // A. TOMBOL RETUR (KIRI - MERAH)
                                    // Hanya muncul jika pembayaran CASH/CREDIT
                                    if (payment == "CASH" || payment == "CREDIT") {
                                        OutlinedButton(
                                            onClick = { navController.navigate("create_retur/$orderId") },
                                            modifier = Modifier
                                                .weight(1f) // Bagi lebar 50:50
                                                .height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                        ) {
                                            Text("RETUR", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // B. TOMBOL KONFIRMASI (KANAN - HIJAU)
                                    Button(
                                        onClick = {
                                            isVerifying = true
                                            scope.launch(Dispatchers.IO) {
                                                val success = OrderRepository.verifyOrder(orderId)
                                                withContext(Dispatchers.Main) {
                                                    isVerifying = false
                                                    if (success) {
                                                        Toast.makeText(context, "Pesanan Diterima & Selesai!", Toast.LENGTH_SHORT).show()
                                                        loadData() // Refresh layar jadi VERIFIED
                                                    } else {
                                                        Toast.makeText(context, "Gagal verifikasi.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f) // Bagi lebar 50:50
                                            .height(50.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)), // Hijau
                                        enabled = !isVerifying
                                    ) {
                                        if (isVerifying) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("TERIMA", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // --- KASUS 2: SUDAH MENGAJUKAN RETUR ---
                            else if (status == "RETURN_REQUESTED") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // Kuning
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Color(0xFFF57C00))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Pengajuan Retur Terkirim", fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                                            Text("Mohon tunggu konfirmasi admin.", fontSize = 12.sp, color = Color(0xFFF57C00))
                                        }
                                    }
                                }
                            }

                            // --- KASUS 3: SUDAH DIVERIFIKASI (SELESAI) ---
                            else if (status == "VERIFIED") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Hijau Muda
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pesanan Selesai", fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (headerData == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pesanan tidak ditemukan") }
        } else {
            // Ambil data dari variabel state baru agar kode di bawah lebih rapi
            val header = headerData!!
            val items = itemsData

            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
            ) {
                // 1. HEADER INFO
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("ID: ${header.kode}", fontWeight = FontWeight.Bold)
                                Surface(
                                    color = getStatusColor(header.status),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(header.status, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp, 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(header.date, color = Color.Gray, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(header.outletName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    // Handle null safety untuk address
                                    Text(header.address ?: "-", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            if (!header.notes.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row {
                                    Icon(Icons.Default.Note, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Catatan: ${header.notes}", color = Color.DarkGray, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. ITEMS
                item { Text("Rincian Barang", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)) }

                items(items) { item ->
                    OrderItemRow(item, formatRp)
                    Spacer(Modifier.height(8.dp))
                }

                // 3. FOOTER TOTAL
                item {
                    Spacer(Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Metode Bayar", color = Color.Gray)
                                Text(header.paymentMethod, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Status Bayar", color = Color.Gray)
                                Text(header.paymentStatus, color = if(header.paymentStatus=="PAID") Color(0xFF43A047) else Color(0xFFF57C00))
                            }
                            Divider(Modifier.padding(vertical = 12.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(formatRp.format(header.total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1976D2))
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

// UI Item Row (Tetap sama)
@Composable
fun OrderItemRow(item: OrderDetailItem, formatRp: NumberFormat) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (item.photoUrl != null) {
                AsyncImage(
                    // GUNAKAN UrlHelper AGAR JADI LINK LENGKAP
                    // Contoh: http://192.168.1.8:8000/storage/products/foto.png
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(UrlHelper.getFullImageUrl(item.photoUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            } else {
                Box(Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(item.variantName, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${item.qty} x ${formatRp.format(item.price)}", fontSize = 12.sp, color = Color.Gray)
                    Text(formatRp.format(item.subTotal), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Helper Warna Status (Tetap sama)
fun getStatusColor(status: String): Color {
    return when(status) {
        "PACKING" -> Color(0xFFFFA000)
        "SHIPPED", "IN_TRANSIT" -> Color(0xFF1976D2)
        "RECEIVED", "COMPLETED", "DELIVERED" -> Color(0xFF43A047)
        "CANCELLED", "REJECTED" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }
}