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
                    items(routeList) { route ->
                        val isFirst = (route == routeList.first())
                        PremiumRouteCard(
                            route = route,
                            brandColor = brandColor,
                            defaultExpanded = isFirst,
                            onVisitClick = { stopId ->
                                // LOGIKA NAVIGASI SAAT TOMBOL DIKLIK
                                navController.navigate("visit_detail/$stopId")
                            }
                        )
                    }
                    // Spacer bawah agar list paling bawah tidak terpotong navbar HP
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

@Composable
fun TimelineItem(
    stop: RouteStop,
    brandColor: Color,
    isLast: Boolean,
    onVisitClick: (Int) -> Unit // <--- Parameter Baru
) {
    val isVisited = stop.status == "VISITED"
    val statusColor = if (isVisited) Color(0xFF4CAF50) else brandColor

    Row(modifier = Modifier.height(IntrinsicSize.Min)) { // Penting agar garis tingginya pas
        // KOLOM KIRI (GARIS & TITIK)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(30.dp)
        ) {
            // Titik (Dot)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (isVisited) statusColor else Color.White,
                        shape = CircleShape
                    )
                    .then(
                        // Border jika belum visited
                        if (!isVisited) Modifier.border(2.dp, statusColor, CircleShape) else Modifier
                    )
            )

            // Garis (Line) - Jangan gambar jika item terakhir
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight() // Mengisi tinggi sisa sampai item berikutnya
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // KOLOM KANAN (KONTEN)
        Column(modifier = Modifier.padding(bottom = 24.dp)) { // Padding bottom memberi jarak antar item
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.outlet?.name ?: "Unknown Outlet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isVisited) Color.Gray else Color.Black
                )
                if (isVisited) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stop.outlet?.address ?: "-",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Aksi (Hanya jika belum visited)
            if (!isVisited) {
                Button(
                    onClick = { onVisitClick(stop.id) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                ) {
                    Text("Kunjungi", fontSize = 12.sp)
                }
            }
        }
    }
}