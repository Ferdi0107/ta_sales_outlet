package com.example.ta_sales_outlet.ui.sales.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.repository.HistoryRepository
import com.example.ta_sales_outlet.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(navController: NavController) {
    // STATE DATA ASLI (DARI DB)
    var originalList by remember { mutableStateOf<List<OrderHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // STATE FILTERING
    var searchQuery by remember { mutableStateOf("") }
    // Opsi: "ALL", "PAID", "UNPAID"
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    val scope = rememberCoroutineScope()

    // LOAD DATA SEKALI SAJA DI AWAL
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val data = HistoryRepository.getSalesHistory(SessionManager.userId)
            withContext(Dispatchers.Main) {
                originalList = data
                isLoading = false
            }
        }
    }

    // LOGIKA FILTERING (REAKTIF)
    // List ini akan berubah otomatis saat searchQuery atau selectedStatusFilter berubah
    val filteredList = remember(originalList, searchQuery, selectedStatusFilter) {
        originalList.filter { order ->
            // 1. Filter Text (Outlet Name ATAU Order Code)
            val matchSearch = if (searchQuery.isBlank()) true else {
                order.outletName.contains(searchQuery, ignoreCase = true) ||
                        order.kode.contains(searchQuery, ignoreCase = true)
            }

            // 2. Filter Status Pembayaran
            val matchStatus = when (selectedStatusFilter) {
                "ALL" -> true
                "PAID" -> order.paymentStatus == "PAID"
                "UNPAID" -> order.paymentStatus != "PAID" // Termasuk NOT_DUE, AWAITING, OVERDUE
                else -> true
            }

            matchSearch && matchStatus
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Riwayat Transaksi", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // AREA FILTER & SEARCH
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                    // 1. SEARCH BAR (By Outlet / Kode)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari Outlet atau Kode Order...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. FILTER CHIPS (Status Pembayaran)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChipItem(
                            label = "Semua",
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" }
                        )
                        FilterChipItem(
                            label = "Lunas",
                            selected = selectedStatusFilter == "PAID",
                            onClick = { selectedStatusFilter = "PAID" }
                        )
                        FilterChipItem(
                            label = "Belum Lunas",
                            selected = selectedStatusFilter == "UNPAID",
                            onClick = { selectedStatusFilter = "UNPAID" }
                        )
                    }
                }
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredList.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FilterList, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak ada data sesuai filter", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(paddingValues)
            ) {
                // Info Jumlah Data
                item {
                    Text(
                        "Menampilkan ${filteredList.size} transaksi",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(filteredList) { order ->
                    HistoryItemCard(order, onClick = {
                        // Pastikan rute detail sudah didaftarkan di NavHost
                        navController.navigate("history_detail/${order.id}")
                    })
                }
            }
        }
    }
}

// Custom Chip Component agar kodenya rapi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFE3F2FD),
            selectedLabelColor = Color(0xFF1976D2),
            selectedLeadingIconColor = Color(0xFF1976D2)
        )
    )
}

// ... (HistoryItemCard tetap SAMA seperti kode sebelumnya) ...
@Composable
fun HistoryItemCard(order: OrderHistory, onClick: () -> Unit) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    val statusColor = when (order.status) {
        "PACKING" -> Color(0xFFFFA000)
        "SHIPPED" -> Color(0xFF1976D2)
        "DELIVERED", "VERIFIED" -> Color(0xFF43A047)
        "CANCELLED" -> Color.Red
        else -> Color.Gray
    }

    val payColor = when (order.paymentStatus) {
        "PAID" -> Color(0xFF43A047)
        "NOT_DUE" -> Color(0xFF1976D2)
        "OVERDUE" -> Color.Red
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.date, fontSize = 12.sp, color = Color.Gray)
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body
            Text(order.outletName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("#${order.kode}", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.paymentMethod, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(${order.paymentStatus})", fontSize = 11.sp, color = payColor, fontWeight = FontWeight.Medium)
                }
                Text(formatRp.format(order.total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1976D2))
            }
        }
    }
}