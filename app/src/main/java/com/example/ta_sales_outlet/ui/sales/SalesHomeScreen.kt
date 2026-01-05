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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ta_sales_outlet.data.model.Route
import com.example.ta_sales_outlet.data.model.RouteStop
import com.example.ta_sales_outlet.data.pref.UserPreferences
import com.example.ta_sales_outlet.data.repository.RouteRepository
import com.example.ta_sales_outlet.utils.MapsHelper
import java.text.SimpleDateFormat
import com.example.ta_sales_outlet.utils.SessionManager
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHomeScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    val userName by userPreferences.userName.collectAsState(initial = "Sales")
    val userId by userPreferences.userId.collectAsState(initial = 0)

    var routeList by remember { mutableStateOf<List<Route>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Warna Tema
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
            // Header Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(brandColor, brandLight)
                        )
                    )
                    .padding(bottom = 24.dp)
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
                    item { EmptyStateCard() }
                } else {
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
                    item { Spacer(modifier = Modifier.height(30.dp)) }
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
    onVisitClick: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow")
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
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
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotationState),
                    tint = Color.Gray
                )
            }

            if (expanded) {
                Divider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)
            }

            // ISI KARTU (TIMELINE)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val stops = route.stops ?: emptyList()

                    // ==========================================
                    // 1. TITIK HIJAU (START)
                    // ==========================================
                    TimelineStartEndItem(
                        title = "TITIK AWAL (START)",
                        locationName = route.startPoint ?: "Kantor Pusat",
                        time = "08:00", // Bisa ambil dari route.plannedDeparture jika ada
                        isStart = true, // HIJAU
                        onClick = {
                            // Buka Maps ke Titik Start
                            // Pastikan Anda sudah punya properti startLat/Lng di Model Route
                            // Jika belum, beri nilai default 0.0 dulu agar tidak error
                            /* MapsHelper.openNavigation(context, route.startLat, route.startLng) */
                            // Contoh sementara Hardcode jika belum ada variabelnya:
                            MapsHelper.openNavigation(context, -7.2575, 112.7521)
                        }
                    )

                    // ==========================================
                    // 2. LOOP KUNJUNGAN (STOPS)
                    // ==========================================
                    if (stops.isNotEmpty()) {
                        stops.forEach { stop ->
                            TimelineItem(
                                stop = stop,
                                brandColor = brandColor,
                                isLast = false,
                                onVisitClick = { stopId ->
                                    onVisitClick(stopId)
                                }
                            )
                        }
                    } else {
                        // Garis dummy jika tidak ada stop
                        Box(modifier = Modifier.padding(start = 24.dp).height(20.dp).width(2.dp).background(Color.LightGray.copy(alpha=0.5f)))
                    }

                    // ==========================================
                    // 3. TITIK MERAH (END)
                    // ==========================================
                    TimelineStartEndItem(
                        title = "TITIK AKHIR (END)",
                        locationName = route.endPoint ?: "Kembali ke Depo",
                        time = "17:00",
                        isStart = false, // MERAH
                        onClick = {
                            /* MapsHelper.openNavigation(context, route.endLat, route.endLng) */
                            MapsHelper.openNavigation(context, -7.2575, 112.7521)
                        }
                    )
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---

fun formatTime(dateTimeStr: String?): String {
    if (dateTimeStr.isNullOrEmpty()) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = parser.parse(dateTimeStr)
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        "-"
    }
}

@Composable
fun TimelineStartEndItem(
    title: String,
    locationName: String?,
    time: String?,
    isStart: Boolean,    // True = Hijau, False = Merah
    onClick: () -> Unit // Callback klik untuk navigasi
) {
    val pointColor = if (isStart) Color(0xFF4CAF50) else Color(0xFFE53935)
    val icon = if (isStart) Icons.Default.PlayArrow else Icons.Default.Flag

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // KOLOM KIRI (Garis & Dot) - Lebar Tetap 50dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = time ?: "--:--",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Lingkaran Berwarna
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(pointColor, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Garis Penghubung (Hanya Start yang punya garis ke bawah)
            if (isStart) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // KOLOM KANAN (Info Teks)
        Column(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .clickable { onClick() } // Bisa diklik untuk Maps
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = pointColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = locationName ?: "Lokasi Tidak Diketahui",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Directions, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buka Peta", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TimelineItem(stop: RouteStop, brandColor: Color, isLast: Boolean, onVisitClick: (Int) -> Unit) {
    // 1. Tentukan Status & Tampilan Visual
    val status = stop.status ?: "DRAFT"

    // Konfigurasi Warna & Teks berdasarkan Status
    val (statusColor, statusText, buttonText, isButtonEnabled) = when (status) {
        "VISITED" -> Quadruple(Color(0xFF43A047), "Selesai", "Lihat Detail", true) // Hijau
        "CONFIRMED" -> Quadruple(brandColor, "Siap Dikunjungi", "Kunjungi", true) // Biru (ACC Outlet)
        "PUBLISHED" -> Quadruple(Color(0xFFFB8C00), "Menunggu Konfirmasi Outlet", "Detail", true) // Oranye
        "REJECTED", "CANCELLED" -> Quadruple(Color(0xFFE53935), "Ditolak / Batal", "Cek", true) // Merah
        else -> Quadruple(Color.Gray, "Draft", "Kunjungi", true) // Abu-abu
    }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // KOLOM KIRI (Garis & Dot) - Lebar Tetap 50dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(
                text = formatTime(stop.plannedArrival),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Dot Indikator Status
            Box(
                modifier = Modifier
                    .size(16.dp) // Sedikit lebih besar
                    .background(Color.White, CircleShape)
                    .border(3.dp, statusColor, CircleShape) // Border warna status
            ) {
                // Jika Visited, isi full warnanya
                if (status == "VISITED") {
                    Box(modifier = Modifier.matchParentSize().background(statusColor, CircleShape))
                }
            }

            // Garis Penghubung
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

        // KOLOM KANAN (Konten Toko)
        Column(modifier = Modifier.padding(bottom = 24.dp).weight(1f)) {

            // Nama Toko & Icon Check
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.outlet?.name ?: "Unknown Outlet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (status == "VISITED") Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                if (status == "VISITED") {
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

            Spacer(modifier = Modifier.height(6.dp))

            // LABEL STATUS (BADGE KECIL)
            // Ini yang menampilkan "Menunggu Konfirmasi", "Siap Dikunjungi", dll
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = statusText.uppercase(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Estimasi Waktu
            if (stop.plannedArrival != null && stop.plannedDeparture != null) {
                Text(
                    text = "Jadwal: ${formatTime(stop.plannedArrival)} - ${formatTime(stop.plannedDeparture)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TOMBOL AKSI
            Button(
                onClick = { onVisitClick(stop.id) },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    // Jika Published (Menunggu), warnanya Orange pudar atau outline
                    // Jika Confirmed, warnanya Brand Color (Biru)
                    containerColor = if (status == "VISITED" || status == "REJECTED") Color.LightGray else statusColor,
                    contentColor = if (status == "VISITED" || status == "REJECTED") Color.Black else Color.White
                ),
                enabled = isButtonEnabled
            ) {
                Text(text = buttonText, fontSize = 12.sp)
            }
        }
    }
}

// Helper Class sederhana agar kode lebih rapi (Taruh di paling bawah file atau di luar fungsi)
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)