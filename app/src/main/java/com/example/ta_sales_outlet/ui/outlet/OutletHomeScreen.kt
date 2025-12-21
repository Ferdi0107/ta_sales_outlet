package com.example.ta_sales_outlet.ui.outlet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ta_sales_outlet.data.model.BillSummary
import com.example.ta_sales_outlet.data.repository.IncomingVisit
import com.example.ta_sales_outlet.data.repository.OutletRepository
import com.example.ta_sales_outlet.data.repository.OutletVisitRepository
import com.example.ta_sales_outlet.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletHomeScreen(navController: NavController, onGoToCatalog: () -> Unit, onGoToHistory: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userName = SessionManager.userName ?: "Outlet Partner"

    // --- STATE DATA ---
    var visitList by remember { mutableStateOf<List<IncomingVisit>>(emptyList()) }
    var billData by remember { mutableStateOf<BillSummary?>(null) }
    var isLoadingBill by remember { mutableStateOf(true) }

    // --- STATE REFRESH ---
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    // --- FUNGSI LOAD DATA UTAMA ---
    fun loadData() {
        scope.launch(Dispatchers.IO) {
            // Set refreshing true agar loading spinner muncul (jika ditarik)
            isRefreshing = true

            val currentUserId = SessionManager.userId

            // Ambil data kunjungan & tagihan
            val visits = OutletVisitRepository.getIncomingVisits(currentUserId)
            val summary = OutletRepository.getBillSummary(currentUserId)

            withContext(Dispatchers.Main) {
                visitList = visits
                billData = summary

                // Matikan loading state
                isLoadingBill = false
                isRefreshing = false
            }
        }
    }

    // Load Data saat pertama kali dibuka
    LaunchedEffect(Unit) {
        loadData()
    }

    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val scrollState = rememberScrollState()

    // --- ROOT UI DENGAN PULL TO REFRESH ---
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { loadData() }, // Panggil fungsi loadData saat ditarik
        state = pullRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(scrollState)
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
                    .padding(bottom = 48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Halo, Selamat Datang!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Notifications, "Notif", tint = Color.White)
                    }
                }
            }

            // --- BODY START ---
            Column(modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-32).dp)) {

                // 1. INFO KEUANGAN
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Logic: Tampilkan loading spinner card HANYA JIKA awal buka (bukan saat refresh)
                    if (isLoadingBill && !isRefreshing) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Kartu
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Status Tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                Spacer(modifier = Modifier.weight(1f))

                                // Badge Jumlah Nota
                                if (billData != null && billData!!.countUnpaid > 0) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "${billData!!.countUnpaid} Nota Aktif",
                                            fontSize = 11.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Total Tagihan Besar
                            Text("Total Belum Lunas", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = if (billData != null) formatRp.format(billData!!.totalUnpaid) else "Rp 0",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                fontSize = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Info Jatuh Tempo Terdekat
                            if (billData != null && billData!!.nearestDueDate != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFFEBEE), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Event, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Jatuh Tempo Terdekat", fontSize = 11.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = billData!!.nearestDueDate ?: "-",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(${formatRp.format(billData!!.nearestBillAmount)})",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Jika tidak ada tagihan
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tidak ada tagihan aktif. Aman!", fontSize = 14.sp, color = Color(0xFF43A047))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. MENU UTAMA
                Text("Aktivitas Toko", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MenuCard(
                        title = "Belanja Stok",
                        icon = Icons.Default.AddShoppingCart,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onGoToCatalog()
                        }
                    )
                    MenuCard(
                        title = "Pesanan",
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFFFFA000),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onGoToHistory()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. DAFTAR KUNJUNGAN
                if (visitList.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Jadwal Kunjungan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Geser >", fontSize = 12.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(visitList) { visit ->
                            VisitApprovalCard(
                                visit = visit,
                                modifier = Modifier.width(300.dp),
                                onConfirm = {
                                    scope.launch(Dispatchers.IO) {
                                        val success = OutletVisitRepository.confirmVisit(visit.routeStopId)
                                        withContext(Dispatchers.Main) {
                                            if (success) {
                                                Toast.makeText(context, "Dikonfirmasi!", Toast.LENGTH_SHORT).show()
                                                // Refresh data setelah confirm
                                                loadData()
                                            }
                                        }
                                    }
                                },
                                onReject = { /* Logic reject */ }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// --- KOMPONEN PENDUKUNG ---

@Composable
fun VisitApprovalCard(
    visit: IncomingVisit,
    modifier: Modifier = Modifier,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val isConfirmed = visit.status == "CONFIRMED"
    val cardColor = if (isConfirmed) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
    val contentColor = if (isConfirmed) Color(0xFF2E7D32) else Color(0xFF1976D2)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER: LABEL & JAM ARRIVAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = contentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isConfirmed) "TERJADWAL" else "PERMINTAAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${visit.arrivalTime} WIB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TANGGAL
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(visit.date, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // SALES NAME
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sales: ${visit.salesName}", fontSize = 14.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TOMBOL AKSI
            if (!isConfirmed) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("Tolak", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Text("Terima", fontSize = 12.sp)
                    }
                }
            } else {
                Text(
                    "Menunggu kedatangan sales.",
                    fontSize = 12.sp,
                    color = contentColor,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
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