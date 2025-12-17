package com.example.ta_sales_outlet.ui.sales.visit

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // PENTING: Import ini untuk remember, mutableStateOf, dll
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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(stopId: Int, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- VARIABEL DUMMY UI (Tambahkan ini agar tidak merah) ---
    val shopName = "Toko Berkah Jaya"
    val shopAddress = "Jl. Ahmad Yani No. 12, Surabaya"
    val brandColor = Color(0xFF1976D2)
    // ---------------------------------------------------------

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    // FUNGSI MEMBUAT FILE KOSONG (WADAH FOTO)
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir ?: context.cacheDir
        return File.createTempFile("VISIT_${timeStamp}_", ".jpg", storageDir)
    }

    // LAUNCHER KAMERA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoFile != null) {
            // JIKA FOTO BERHASIL DIAMBIL -> LANGSUNG UPLOAD
            Toast.makeText(context, "Sedang mengupload foto...", Toast.LENGTH_SHORT).show()

            scope.launch {
                // Gunakan Thread agar Network tidak freeze UI
                Thread {
                    try {
                        // Siapkan File untuk Upload
                        val file = tempPhotoFile!!

                        // Optional: Kompres file jika perlu
                        // file.compressToJpg()

                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

                        // 1. Upload ke Laravel
                        val response = UploadApi.create().uploadVisitPhoto(body).execute()

                        if (response.isSuccessful && response.body()?.status == "success") {
                            val serverPath = response.body()!!.path

                            // 2. Simpan ke MySQL (Status VISITED)
                            val isSaved = MySQLHelper.insertVisitCheckIn(
                                stopId = stopId,
                                photoPath = serverPath,
                                lat = -7.2575, // Masih Dummy (Nanti ganti GPS)
                                lng = 112.7521,
                                notes = "Check-in via Kamera"
                            )

                            (context as? android.app.Activity)?.runOnUiThread {
                                if (isSaved) {
                                    Toast.makeText(context, "Check-in Berhasil!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack() // Kembali ke Dashboard
                                } else {
                                    Toast.makeText(context, "Gagal simpan DB", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            (context as? android.app.Activity)?.runOnUiThread {
                                Toast.makeText(context, "Gagal Upload Server", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        (context as? android.app.Activity)?.runOnUiThread {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Kunjungan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            // 1. HEADER INFO TOKO
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(brandColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = brandColor)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(shopName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Stop ID: #$stopId", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(Icons.Default.LocationOn, shopAddress)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(Icons.Default.Phone, "08123456789")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Menu Aktivitas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // 2. GRID MENU AKSI
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tombol Check-In
                ActionCard(
                    icon = Icons.Default.PinDrop,
                    title = "Check-In",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        try {
                            // 1. Buat file kosong
                            val file = createImageFile()
                            tempPhotoFile = file

                            // 2. Minta URI aman dari FileProvider
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            tempPhotoUri = uri

                            // 3. Buka Kamera
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal buka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                )
                // Tombol Order
                ActionCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "Buat Order",
                    color = brandColor,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Buka Katalog Spesifik Toko Ini */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ... Tombol baris kedua ...
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(Icons.Default.Receipt, "Tagihan", Color(0xFFFF9800), Modifier.weight(1f)) {}
                ActionCard(Icons.Default.EditNote, "Catatan", Color(0xFF9C27B0), Modifier.weight(1f)) {}
            }
        }
    }
}

// ... (Fungsi InfoRow dan ActionCard tetap sama seperti sebelumnya) ...
@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun ActionCard(icon: ImageVector, title: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        }
    }
}