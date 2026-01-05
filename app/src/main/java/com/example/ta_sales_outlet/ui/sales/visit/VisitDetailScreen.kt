package com.example.ta_sales_outlet.ui.sales.visit

import android.net.Uri
import android.os.Environment
import android.util.Log // Import Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.api.UploadApi
import com.example.ta_sales_outlet.utils.MapsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.ta_sales_outlet.data.model.RouteStop
import com.example.ta_sales_outlet.data.repository.RouteRepository
import com.example.ta_sales_outlet.utils.SessionManager
import com.example.ta_sales_outlet.ui.sales.BottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(stopId: Int, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE DATA ---
    var stopData by remember { mutableStateOf<RouteStop?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // State UI Status & Waktu
    var visitStatus by remember { mutableStateOf("PLANNED") }
    var visitTime by remember { mutableStateOf("-") }

    // --- LOAD DATA (DARI DATABASE) ---
    LaunchedEffect(stopId) {
        scope.launch(Dispatchers.IO) {
            Log.d("DEBUG_VISIT", "Fetching detail for stopId: $stopId") // LOG

            // Ambil data di Background Thread
            val data = RouteRepository.getVisitDetail(stopId)

            // Update UI di Main Thread
            withContext(Dispatchers.Main) {
                if (data != null) {
                    stopData = data
                    // Pastikan status tidak null, default ke PLANNED/DRAFT
                    visitStatus = data.status ?: "PLANNED"

                    Log.d("DEBUG_VISIT", "Data found. Status from DB: ${data.status}") // LOG

                    // Logika sederhana penentuan waktu
                    if (visitStatus == "VISITED") {
                        visitTime = "Selesai"
                    }
                } else {
                    Log.e("DEBUG_VISIT", "Data is NULL for stopId: $stopId") // LOG ERROR
                }
                isLoading = false
            }
        }
    }

    // --- LOGIC KAMERA ---
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("VISIT_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Mengupload bukti...", Toast.LENGTH_SHORT).show()
                }

                try {
                    val file = tempPhotoFile!!
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

                    // 1. Upload ke Server
                    val response = UploadApi.create().uploadVisitPhoto(body).execute()

                    if (response.isSuccessful && response.body()?.status == "success") {
                        val serverPath = response.body()!!.path

                        // 2. Simpan ke Database Lokal
                        val currentLat = stopData?.outlet?.latitude ?: -7.2575
                        val currentLng = stopData?.outlet?.longitude ?: 112.7521

                        val isSaved = MySQLHelper.insertVisitCheckIn(
                            stopId = stopId,
                            photoPath = serverPath,
                            lat = currentLat,
                            lng = currentLng,
                            notes = "Check-in via Camera App"
                        )

                        withContext(Dispatchers.Main) {
                            if (isSaved) {
                                // --- UPDATE VISUAL ---
                                visitStatus = "VISITED"
                                visitTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                                // --- PENTING: SIMPAN SESSION OUTLET ---
                                if (stopData != null && stopData!!.outletId != null) {
                                    SessionManager.currentOutletId = stopData!!.outletId!!
                                    SessionManager.currentOutletName = stopData!!.outlet?.name ?: "Outlet"
                                }

                                Toast.makeText(context, "Check-in Berhasil!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Gagal Upload ke Server", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error Upload: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // --- WARNA TEMA ---
    val primaryColor = Color(0xFF1976D2)
    val successColor = Color(0xFF43A047)
    val warningColor = Color(0xFFFB8C00)
    val bgColor = Color(0xFFF0F2F5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Kunjungan", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->

        // --- KONTEN UTAMA ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (stopData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Data Kunjungan Tidak Ditemukan", color = Color.Gray)
            }
        } else {
            val outlet = stopData!!.outlet

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ==========================================
                // 1. HEADER TOKO
                // ==========================================
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = primaryColor, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = outlet?.name ?: "Tanpa Nama",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = outlet?.address ?: "-",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                MapsHelper.openNavigation(
                                    context,
                                    outlet?.latitude ?: -7.2575,
                                    outlet?.longitude ?: 112.7521
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE3F2FD), CircleShape)
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = "Maps", tint = primaryColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // 2. STATUS KUNJUNGAN (Check-In Area)
                // ==========================================
                Text("Status Kunjungan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // --- LOGIKA STATUS YANG DIPERBAIKI (MENGGUNAKAN WHEN) ---
                when (visitStatus.uppercase()) {
                    "VISITED" -> {
                        // TAMPILAN JIKA SUDAH CHECK-IN
                        Card(
                            colors = CardDefaults.cardColors(containerColor = successColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("SUDAH DIKUNJUNGI", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Status: Selesai", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        // TAMPILAN JIKA DRAFT / PUBLISHED / CONFIRMED (BELUM CHECK-IN)
                        Button(
                            onClick = {
                                val file = createImageFile()
                                if (file != null) {
                                    tempPhotoFile = file
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    cameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(context, "Gagal membuat file foto", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Ambil Foto Check-In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Wajib foto di depan toko", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==========================================
                // 3. MENU AKTIVITAS
                // ==========================================
                Text("Aktivitas Toko", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // Tombol ORDER
                MenuCardBig(
                    icon = Icons.Default.ShoppingCart,
                    title = "Buat Pesanan Baru",
                    subtitle = "Input order pembelian barang",
                    // Warna abu-abu jika belum visited, Biru jika sudah
                    color = if (visitStatus == "VISITED") primaryColor else Color.Gray,
                    onClick = {
                        if (visitStatus == "VISITED") {
                            navController.navigate(BottomNavItem.Catalog.route)
                        } else {
                            Toast.makeText(context, "Silakan Check-In foto dulu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        MenuCardSmall(
                            icon = Icons.Default.ReceiptLong,
                            title = "Tagihan",
                            color = warningColor,
                            onClick = { Toast.makeText(context, "Fitur Tagihan", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        MenuCardSmall(
                            icon = Icons.Default.NoteAlt,
                            title = "Catatan",
                            color = Color(0xFF8E24AA),
                            onClick = { Toast.makeText(context, "Fitur Catatan", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        }
    }
}

// --- KOMPONEN UI CUSTOM (DIPERBAIKI) ---
@Composable
fun MenuCardBig(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    // FIX: Menggunakan Modifier.clickable() agar kompatibel dengan Card versi Material3 default
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Tambahkan clickable di sini
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun MenuCardSmall(icon: ImageVector, title: String, color: Color, onClick: () -> Unit) {
    // FIX: Menggunakan Modifier.clickable()
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() } // Tambahkan clickable di sini
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}