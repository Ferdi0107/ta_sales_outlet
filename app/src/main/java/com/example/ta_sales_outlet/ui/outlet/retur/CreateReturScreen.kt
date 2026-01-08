package com.example.ta_sales_outlet.ui.outlet.retur

import android.net.Uri
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
// IMPORT PENTING
import com.example.ta_sales_outlet.utils.UrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

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
    var isReasonDropdownExpanded by remember { mutableStateOf(false) }

    // STATE FOTO
    var photoFile by remember { mutableStateOf<File?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    // Dialog pilihan sumber foto
    var showSourceDialog by remember { mutableStateOf(false) }

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

    // --- LOGIC IMAGE PICKER (KAMERA & GALERI) ---

    // A. Launcher Kamera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoFile = tempPhotoFile
        }
    }

    // B. Launcher Galeri
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // Konversi URI Galeri ke File fisik agar bisa diupload
            val file = File(context.cacheDir, "RETUR_GALERI_${System.currentTimeMillis()}.jpg")
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                photoFile = file
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi helper untuk membuka kamera
    fun openCamera() {
        try {
            val file = File.createTempFile("RETUR_CAM_${System.currentTimeMillis()}_", ".jpg", context.getExternalFilesDir(null))
            tempPhotoFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    // UI DIALOG SUMBER GAMBAR
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Pilih Sumber Foto") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Kamera") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            openCamera()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Galeri") },
                        leadingContent = { Icon(Icons.Default.Image, null) },
                        modifier = Modifier.clickable {
                            showSourceDialog = false
                            galleryLauncher.launch("image/*") // Buka Galeri
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Batal") }
            }
        )
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
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        // VALIDASI
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

                            // UPLOAD FOTO
                            if (photoFile != null) {
                                try {
                                    val reqFile = photoFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("photo", photoFile!!.name, reqFile)
                                    val response = UploadApi.create().uploadReturPhoto(body).execute()
                                    if (response.isSuccessful) {
                                        serverPhotoPath = response.body()?.path
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            // SIMPAN
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
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                // SECTION 1
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

                    Text("Detail Masalah:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

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
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                        )
                        ExposedDropdownMenu(
                            expanded = isReasonDropdownExpanded,
                            onDismissRequest = { isReasonDropdownExpanded = false }
                        ) {
                            ReturReason.values().forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason.label) },
                                    onClick = { selectedReason = reason; isReasonDropdownExpanded = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PREVIEW FOTO + TOMBOL UPLOAD
                    Text("Foto Bukti:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (photoFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(photoFile).build(),
                            contentDescription = "Bukti",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray)
                                .clickable { showSourceDialog = true }, // Klik untuk ganti
                            contentScale = ContentScale.Crop
                        )
                        TextButton(onClick = { showSourceDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ganti Foto")
                        }
                    } else {
                        // TOMBOL UPLOAD (KAMERA / GALERI)
                        OutlinedButton(
                            onClick = { showSourceDialog = true }, // Munculkan Dialog
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Foto Bukti")
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// UPDATE: ROW ITEM DENGAN GAMBAR PRODUK (THUMBNAIL)
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

            // --- TAMBAHKAN INI: FOTO PRODUK ---
            if (!item.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(UrlHelper.getFullImageUrl(item.photoUrl)) // Pakai UrlHelper
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            // ----------------------------------

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