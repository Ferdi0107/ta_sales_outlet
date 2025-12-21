package com.example.ta_sales_outlet.ui.outlet.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.model.ReturHistory
import com.example.ta_sales_outlet.data.repository.HistoryRepository
import com.example.ta_sales_outlet.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletHistoryScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    // STATE TAB (0 = Pesanan, 1 = Retur)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pesanan", "Retur")

    // STATE DATA
    var orderList by remember { mutableStateOf<List<OrderHistory>>(emptyList()) }
    var returList by remember { mutableStateOf<List<ReturHistory>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // LOAD DATA (Panggil keduanya saat awal buka)
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val userId = SessionManager.userId

            // Ambil data Pesanan & Retur sekaligus (parallel)
            val orders = HistoryRepository.getOutletHistory(userId)
            val returs = HistoryRepository.getOutletReturs(userId)

            withContext(Dispatchers.Main) {
                orderList = orders
                returList = returs
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = { Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )

                // --- TAB ROW ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF1976D2)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // SWITCH CONTENT BERDASARKAN TAB
            when (selectedTab) {
                0 -> OrderListContent(padding, orderList, formatRp, navController)
                1 -> ReturListContent(padding, returList, navController)
            }
        }
    }
}

// --- KONTEN TAB 1: LIST PESANAN ---
@Composable
fun OrderListContent(
    padding: PaddingValues,
    list: List<OrderHistory>,
    formatRp: NumberFormat,
    navController: NavController
) {
    if (list.isEmpty()) {
        EmptyState(padding, "Belum ada pesanan.")
    } else {
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(list) { order ->
                OrderCard(order, formatRp) {
                    navController.navigate("order_detail/${order.id}")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// --- KONTEN TAB 2: LIST RETUR ---
@Composable
fun ReturListContent(
    padding: PaddingValues,
    list: List<ReturHistory>,
    navController: NavController
) {
    if (list.isEmpty()) {
        EmptyState(padding, "Belum ada pengajuan retur.")
    } else {
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            items(list) { retur ->
                ReturCard(retur) {
                    // Saat retur diklik, sementara kita arahkan ke Order Detail aslinya
                    // (Nanti bisa dibuat layar ReturDetailScreen terpisah jika mau)
                    navController.navigate("order_detail/${retur.orderId}")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// --- KOMPONEN KARTU PESANAN (ORDER) ---
@Composable
fun OrderCard(order: OrderHistory, formatRp: NumberFormat, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(order.kode, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(order.date, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Total Belanja", fontSize = 11.sp, color = Color.Gray)
                    Text(formatRp.format(order.total), color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                StatusBadge(order.status)
            }
        }
    }
}

// --- KOMPONEN KARTU RETUR (BARU) ---
@Composable
fun ReturCard(retur: ReturHistory, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(retur.kodeRetur, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                // Badge Status Retur
                Surface(
                    color = getReturStatusColor(retur.status),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(retur.status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Dari Pesanan: ${retur.orderKode}", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Alasan: ", fontSize = 12.sp, color = Color.Gray)
                Text(retur.reason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Tanggal: ${retur.date}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
        }
    }
}

@Composable
fun EmptyState(padding: PaddingValues, message: String) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}

@Composable
fun StatusBadge(status: String) {
    Surface(
        color = getStatusColor(status),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// Helper Warna
fun getStatusColor(status: String): Color {
    return when(status) {
        "PACKING" -> Color(0xFFFFA000)
        "SHIPPED", "IN_TRANSIT" -> Color(0xFF1976D2)
        "RECEIVED", "COMPLETED", "DELIVERED", "VERIFIED" -> Color(0xFF43A047)
        else -> Color.Gray
    }
}

fun getReturStatusColor(status: String): Color {
    return when(status) {
        "REQUESTED" -> Color(0xFFFFA000) // Orange
        "APPROVED", "REFUNDED" -> Color(0xFF43A047) // Hijau
        "REJECTED" -> Color(0xFFD32F2F) // Merah
        else -> Color.Gray
    }
}