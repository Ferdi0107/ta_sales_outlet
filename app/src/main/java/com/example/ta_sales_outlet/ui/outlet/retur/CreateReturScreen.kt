package com.example.ta_sales_outlet.ui.outlet.retur

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ta_sales_outlet.data.api.UploadApi
import com.example.ta_sales_outlet.data.model.ReturItem
import com.example.ta_sales_outlet.data.model.ReturReason
import com.example.ta_sales_outlet.data.repository.OrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReturScreen(orderId: Int, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var itemsList by remember { mutableStateOf<List<ReturItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    // State Form
    var selectedReason by remember { mutableStateOf(ReturReason.DAMAGED) }
    var description by remember { mutableStateOf("") }

    // STATE FOTO (DIPISAH AGAR UI REFRESH)
    // 1. photoFile: Digunakan untuk tampil di UI (AsyncImage)
    var photoFile by remember { mutableStateOf<File?>(null) }
    // 2. tempPhotoFile: Tempat penampungan sementara saat kamera terbuka
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    var isReasonDropdownExpanded by remember { mutableStateOf(false) }

    // 1. LOAD DATA BARANG
    LaunchedEffect(orderId) {
        scope.launch(Dispatchers.IO) {
            val data = OrderRepository.getReturItems(orderId)
            withContext(Dispatchers.Main) {
                itemsList = data
                isLoading = false
            }
        }
    }

    // --- LOGIC KAMERA ---
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // PENTING: Pindahkan dari Temp ke Utama agar UI tahu ada gambar baru
            photoFile = tempPhotoFile
        }
    }

    fun openCamera() {
        try {
            // Buat file temporary baru yang unik
            val file = File.createTempFile("RETUR_${System.currentTimeMillis()}_", ".jpg", context.getExternalFilesDir(null))
            tempPhotoFile = file

            // Dapatkan URI dari FileProvider
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajukan Retur", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // TOMBOL KIRIM
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        // Validasi
                        if (itemsList.none { it.isSelected && it.inputQty > 0 }) {
                            Toast.makeText(context, "Pilih minimal 1 barang", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (description.isBlank()) {
                            Toast.makeText(context, "Mohon isi keterangan", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        scope.launch(Dispatchers.IO) {
                            var serverPhotoPath: String? = null

                            // 1. UPLOAD FOTO (Jika ada)
                            if (photoFile != null) {
                                try {
                                    val reqFile = photoFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("photo", photoFile!!.name, reqFile)

                                    // API UPLOAD BARU (Retur)
                                    val response = UploadApi.create().uploadReturPhoto(body).execute()

                                    if (response.isSuccessful) {
                                        serverPhotoPath = response.body()?.path
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // 2. SIMPAN KE DATABASE
                            val success = OrderRepository.createRetur(
                                orderId = orderId,
                                reason = selectedReason,
                                description = description,
                                photoPath = serverPhotoPath,
                                items = itemsList
                            )

                            withContext(Dispatchers.Main) {
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "Berhasil diajukan!", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Gagal. Cek koneksi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isSubmitting && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White) else Text("AJUKAN RETUR", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)
            ) {
                // SECTION 1: PILIH BARANG
                item {
                    Text("Pilih Barang:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(itemsList) { item ->
                    ReturItemRow(
                        item = item,
                        onCheckedChange = { isChecked ->
                            itemsList = itemsList.map {
                                if (it.orderDetailId == item.orderDetailId) {
                                    it.copy(isSelected = isChecked, inputQty = if (isChecked) 1 else 0)
                                } else it
                            }
                        },
                        onQtyChange = { newQty ->
                            itemsList = itemsList.map {
                                if (it.orderDetailId == item.orderDetailId) it.copy(inputQty = newQty) else it
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: ALASAN & FOTO
                    Text("Detail Masalah:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // DROPDOWN ALASAN
                    ExposedDropdownMenuBox(
                        expanded = isReasonDropdownExpanded,
                        onExpandedChange = { isReasonDropdownExpanded = !isReasonDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedReason.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alasan Pengembalian") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isReasonDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isReasonDropdownExpanded,
                            onDismissRequest = { isReasonDropdownExpanded = false }
                        ) {
                            ReturReason.values().forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason.label) },
                                    onClick = {
                                        selectedReason = reason
                                        isReasonDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INPUT DESKRIPSI
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PREVIEW FOTO
                    Text("Foto Bukti:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (photoFile != null) {
                        // Gunakan ImageRequest agar Coil merefresh gambar jika file berubah
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Bukti Foto",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray)
                                .clickable { openCamera() },
                            contentScale = ContentScale.Crop
                        )
                        TextButton(onClick = { openCamera() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ganti Foto")
                        }
                    } else {
                        Button(
                            onClick = { openCamera() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ambil Foto (Wajib jika rusak)", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// Reuse Komponen Row
@Composable
fun ReturItemRow(
    item: ReturItem,
    onCheckedChange: (Boolean) -> Unit,
    onQtyChange: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (item.isSelected) Color(0xFFE3F2FD) else Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isSelected, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Varian: ${item.variantName}", fontSize = 12.sp, color = Color.Gray)
                Text("Beli: ${item.maxQty} pcs", fontSize = 11.sp, color = Color.DarkGray)
            }
            if (item.isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (item.inputQty > 1) onQtyChange(item.inputQty - 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(item.inputQty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (item.inputQty < item.maxQty) onQtyChange(item.inputQty + 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        }
    }
}