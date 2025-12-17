package com.example.ta_sales_outlet.ui.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ta_sales_outlet.data.model.Route
import com.example.ta_sales_outlet.data.model.RouteStop
import com.example.ta_sales_outlet.data.pref.UserPreferences
import com.example.ta_sales_outlet.data.repository.RouteRepository
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import com.example.ta_sales_outlet.utils.MapsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHomeScreen(
    navController: NavController, // <--- Parameter Baru
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    val userName by userPreferences.userName.collectAsState(initial = "Sales")
    val userId by userPreferences.userId.collectAsState(initial = 0)

    var routeList by remember { mutableStateOf<List<Route>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Warna Tema Gradient
    val brandColor = Color(0xFF1976D2)
    val brandLight = Color(0xFF42A5F5)
    val bgSurface = Color(0xFFF8F9FA)

    LaunchedEffect(userId) {
        if (userId != 0 && userId != null) {
            Thread {
                val data = RouteRepository.getAllRoutesSorted(userId!!)
                routeList = data
                isLoading = false
            }.start()
        }
    }

    Scaffold(
        containerColor = bgSurface,
        topBar = {
            // Header Custom dengan Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(brandColor, brandLight)
                        )
                    )
                    .padding(bottom = 24.dp) // Memberi ruang untuk lengkungan (opsional)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Halo, ${userName?.split(" ")?.firstOrNull() ?: "Sales"}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Siap untuk kunjungan hari ini?",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = brandColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (routeList.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    // --- BAGIAN 1: TOMBOL MULAI (HEADER) ---
                    // Kita taruh di dalam 'item' (tunggal) sebelum list rute
                    item {
                        // Ambil koordinat start dari rute pertama (jika ada)
                        // Atau hardcode dulu jika di database belum ada kolom start_lat
                        val startLat = -7.2891
                        val startLng = 112.7343

                        Button(
                            onClick = { MapsHelper.openNavigation(context, startLat, startLng) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Perjalanan (Ke Gudang)")
                        }
                    }

                    // --- BAGIAN 2: DAFTAR RUTE (BODY) ---
                    // Ini kode lama Anda, menampilkan kartu-kartu rute
                    items(routeList) { route ->
                        val isFirst = (route == routeList.first())
                        PremiumRouteCard(
                            route = route,
                            brandColor = brandColor,
                            defaultExpanded = isFirst,
                            onVisitClick = { stopId ->
                                navController.navigate("visit_detail/$stopId")
                            }
                        )
                    }

                    // --- BAGIAN 3: TOMBOL SELESAI (FOOTER) ---
                    // Kita taruh di dalam 'item' (tunggal) setelah list rute selesai
                    item {
                        val endLat = -7.2891
                        val endLng = 112.7343

                        OutlinedButton(
                            onClick = { MapsHelper.openNavigation(context, endLat, endLng) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Selesai & Pulang (Ke Depo)")
                        }

                        // Spacer tambahan biar tombol tidak mepet banget sama navigasi HP bawah
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Belum ada jadwal", fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("Hubungi admin untuk plotting rute", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun PremiumRouteCard(
    route: Route,
    brandColor: Color,
    defaultExpanded: Boolean,
    onVisitClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    // Animasi putaran panah
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp), // Shadow lebih tegas
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // HEADER KARTU
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon Kalender di dalam Kotak Kecil
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(brandColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = brandColor)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        val dateLabel = try {
                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))
                            formatter.format(parser.parse(route.date ?: "") ?: Date())
                        } catch (e: Exception) { route.date ?: "-" }

                        Text(
                            text = dateLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )

                        // Badge Status & Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text(
                                text = " ${route.totalStops} Toko  •  ${route.distanceKm} km",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Icon Panah Animasi
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotationState),
                    tint = Color.Gray
                )
            }

            // GARIS PEMBATAS HALUS
            if (expanded) {
                Divider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)
            }

            // ISI KARTU (TIMELINE)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val stops = route.stops ?: emptyList()
                    stops.forEachIndexed { index, stop ->
                        TimelineItem(
                            stop = stop,
                            brandColor = brandColor,
                            isLast = index == stops.lastIndex,
                            onVisitClick = onVisitClick
                        )
                    }
                }
            }
        }
    }
}

fun formatTime(dateTimeStr: String?): String {
    if (dateTimeStr.isNullOrEmpty()) return "-"
    return try {
        // Format DB: 2025-12-17 10:30:00
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = parser.parse(dateTimeStr)
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        "-"
    }
}

@Composable
fun TimelineItem(stop: RouteStop, brandColor: Color, isLast: Boolean, onVisitClick: (Int) -> Unit) {
    val isVisited = stop.status == "VISITED"

    // Warna garis/dot: Jika Visited = Hijau, Belum = Biru
    val statusColor = if (isVisited) Color(0xFF4CAF50) else brandColor

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // --- KOLOM KIRI (JAM & GARIS) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp) // Lebarkan sedikit buat jam
        ) {
            // Tampilkan Jam Rencana (Hanya Jam Awal)
            Text(
                text = formatTime(stop.plannedArrival),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Titik (Dot)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (isVisited) statusColor else Color.White,
                        shape = CircleShape
                    )
                    .then(
                        if (!isVisited) Modifier.border(2.dp, statusColor, CircleShape) else Modifier
                    )
            )

            // Garis (Line)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // --- KOLOM KANAN (KONTEN) ---
        Column(modifier = Modifier.padding(bottom = 24.dp).weight(1f)) {

            // Baris Nama Toko & Status Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.outlet?.name ?: "Unknown Outlet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isVisited) Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // Icon Centang tetap ada sebagai indikator visual
                if (isVisited) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                }
            }

            // Alamat
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stop.outlet?.address ?: "-",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            // Tampilkan Rentang Waktu (Optional, biar user tau durasi)
            if (stop.plannedArrival != null && stop.plannedDeparture != null) {
                Text(
                    text = "Estimasi: ${formatTime(stop.plannedArrival)} - ${formatTime(stop.plannedDeparture)}",
                    fontSize = 11.sp,
                    color = brandColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- TOMBOL AKSI (REVISI: SELALU MUNCUL) ---
            // Tombol tidak lagi di-hide, tapi warnanya dibedakan
            Button(
                onClick = { onVisitClick(stop.id) }, // Tetap bisa diklik meski sudah visited
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    // Jika visited: Abu-abu (Kesan read-only/selesai), Jika belum: Biru
                    containerColor = if (isVisited) Color.LightGray else brandColor,
                    contentColor = if (isVisited) Color.Black else Color.White
                )
            ) {
                Text(
                    text = if (isVisited) "Lihat Detail" else "Kunjungi",
                    fontSize = 12.sp
                )
            }
        }
    }
}