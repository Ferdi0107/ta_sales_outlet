package com.example.ta_sales_outlet.ui.sales.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
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
import com.example.ta_sales_outlet.data.model.OrderDetailItem
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.repository.HistoryRepository
import com.example.ta_sales_outlet.utils.UrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(orderId: Int, navController: NavController) {
    val scope = rememberCoroutineScope()
    var orderHeader by remember { mutableStateOf<OrderHistory?>(null) }
    var itemList by remember { mutableStateOf<List<OrderDetailItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // Load Data
    LaunchedEffect(orderId) {
        scope.launch(Dispatchers.IO) {
            val header = HistoryRepository.getOrderHeader(orderId)
            val items = HistoryRepository.getOrderItems(orderId)

            withContext(Dispatchers.Main) {
                orderHeader = header
                itemList = items
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orderHeader == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Data tidak ditemukan")
            }
        } else {
            val header = orderHeader!!

            Column(modifier = Modifier.padding(paddingValues)) {

                // ISI SCROLLABLE
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. INFO HEADER (Status & Toko)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Order ID", color = Color.Gray, fontSize = 12.sp)
                                    Text(header.date, color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(header.kode, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Outlet Tujuan", fontSize = 11.sp, color = Color.Gray)
                                        Text(header.outletName, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Chips Status
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusChip(header.status, Color(0xFFFFA000))
                                    StatusChip(header.paymentStatus, Color(0xFF43A047))
                                    StatusChip(header.paymentMethod, Color.Gray)
                                }
                            }
                        }
                    }

                    // 2. LABEL DAFTAR ITEM
                    item {
                        Text("Rincian Barang", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                    }

                    // 3. LIST BARANG
                    items(itemList) { item ->
                        DetailItemRow(item)
                    }
                }

                // FOOTER TOTAL
                Surface(shadowElevation = 16.dp, color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total", fontSize = 16.sp, color = Color.Gray)
                        Text(
                            formatRp.format(header.total),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DetailItemRow(item: OrderDetailItem) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FOTO
            AsyncImage(
                model = UrlHelper.getFullImageUrl(item.photoUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // INFO
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Varian: ${item.variantName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "${item.qty} x ${formatRp.format(item.price)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // SUBTOTAL
            Text(
                formatRp.format(item.subTotal),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}