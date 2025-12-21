package com.example.ta_sales_outlet.ui.auth

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ta_sales_outlet.data.api.Province
import com.example.ta_sales_outlet.data.api.WilayahRepository
import com.example.ta_sales_outlet.data.api.UploadApi
import com.example.ta_sales_outlet.data.repository.AuthRepository
import com.example.ta_sales_outlet.data.repository.City
import com.example.ta_sales_outlet.data.repository.RegionRepository
import com.example.ta_sales_outlet.ui.components.LocationPicker
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// Import fungsi uriToFile dari jawaban sebelumnya (pastikan ada di file ini atau utils)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletRegisterScreen(navController: NavController) {
    val context = LocalContext.current

    // State Form Dasar
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var noTelp by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }

    // STATE FOTO
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // STATE WILAYAH (API & DB)
    var provinceList by remember { mutableStateOf<List<Province>>(emptyList()) }
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var cityList by remember { mutableStateOf<List<City>>(emptyList()) }
    var selectedCity by remember { mutableStateOf<City?>(null) }

    // Dropdown Expanded States
    var isProvExpanded by remember { mutableStateOf(false) }
    var isCityExpanded by remember { mutableStateOf(false) }
    var isPaymentExpanded by remember { mutableStateOf(false) }
    var isTimeExpanded by remember { mutableStateOf(false) }

    // STATE OPSIONAL LAIN
    val paymentTypes = listOf("CREDIT", "KONSINYASI")
    var selectedPayment by remember { mutableStateOf(paymentTypes[0]) }

    val timePrefs = listOf("PAGI", "SIANG")
    var selectedTimePref by remember { mutableStateOf(timePrefs[0]) }

    var isLoading by remember { mutableStateOf(false) }

    // 1. LOAD PROVINSI SAAT MULAI
    LaunchedEffect(Unit) {
        WilayahRepository.getProvinces { list ->
            provinceList = list
        }
    }

    // 2. LOAD KOTA SAAT PROVINSI DIPILIH
    LaunchedEffect(selectedProvince) {
        if (selectedProvince != null) {
            // Reset Kota
            selectedCity = null
            cityList = emptyList()

            // Ambil Kota dari DB (Background Thread)
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {

                // --- PERBAIKAN DI SINI ---
                // Kirim .name (Nama), BUKAN .code
                val cities = RegionRepository.getCitiesByProvinceName(selectedProvince!!.name)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    cityList = cities
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Registrasi Outlet") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (BAGIAN FOTO PROFIL - KODE SAMA SEPERTI SEBELUMNYA) ...
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.LightGray).clickable {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.AddAPhoto, null, tint = Color.DarkGray)
            }

            // ... (BAGIAN MAP - KODE SAMA SEPERTI SEBELUMNYA) ...
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Lokasi Toko", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                LocationPicker(
                    modifier = Modifier.fillMaxWidth(),
                    onLocationSelected = { la, lo, add -> lat = la; lng = lo; if (alamat.isEmpty()) alamat = add }
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // ... (INPUT NAMA, EMAIL, PASS, NO TELP - KODE SAMA) ...
                    OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = noTelp, onValueChange = { if (it.all { c -> c.isDigit() }) noTelp = it }, label = { Text("No. Telepon") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Detail & Wilayah", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // DROPDOWN PROVINSI (API)
                    ExposedDropdownMenuBox(expanded = isProvExpanded, onExpandedChange = { isProvExpanded = !isProvExpanded }) {
                        OutlinedTextField(
                            value = selectedProvince?.name ?: "Pilih Provinsi",
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProvExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Provinsi") }
                        )
                        ExposedDropdownMenu(expanded = isProvExpanded, onDismissRequest = { isProvExpanded = false }) {
                            provinceList.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov.name) },
                                    onClick = { selectedProvince = prov; isProvExpanded = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // DROPDOWN KOTA (DB - Filtered by Provinsi)
                    ExposedDropdownMenuBox(expanded = isCityExpanded, onExpandedChange = { isCityExpanded = !isCityExpanded }) {
                        OutlinedTextField(
                            value = selectedCity?.name ?: "Pilih Kota/Kabupaten",
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Kota/Kabupaten") },
                            enabled = selectedProvince != null // Disable jika belum pilih provinsi
                        )
                        ExposedDropdownMenu(expanded = isCityExpanded, onDismissRequest = { isCityExpanded = false }) {
                            if (cityList.isEmpty()) {
                                DropdownMenuItem(text = { Text("Tidak ada data / Pilih Provinsi dulu") }, onClick = {})
                            } else {
                                cityList.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city.name) },
                                        onClick = { selectedCity = city; isCityExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DROPDOWN TIPE PEMBAYARAN
                    ExposedDropdownMenuBox(expanded = isPaymentExpanded, onExpandedChange = { isPaymentExpanded = !isPaymentExpanded }) {
                        OutlinedTextField(
                            value = selectedPayment, onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Tipe Pembayaran") }
                        )
                        ExposedDropdownMenu(expanded = isPaymentExpanded, onDismissRequest = { isPaymentExpanded = false }) {
                            paymentTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { selectedPayment = type; isPaymentExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // DROPDOWN PREF WAKTU
                    ExposedDropdownMenuBox(expanded = isTimeExpanded, onExpandedChange = { isTimeExpanded = !isTimeExpanded }) {
                        OutlinedTextField(
                            value = selectedTimePref, onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTimeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Preferensi Waktu Kunjungan") }
                        )
                        ExposedDropdownMenu(expanded = isTimeExpanded, onDismissRequest = { isTimeExpanded = false }) {
                            timePrefs.forEach { time ->
                                DropdownMenuItem(text = { Text(time) }, onClick = { selectedTimePref = time; isTimeExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = alamat, onValueChange = { alamat = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (nama.isBlank() || email.isBlank() || password.isBlank() || lat == 0.0) {
                                Toast.makeText(context, "Lengkapi data wajib!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedCity == null) {
                                Toast.makeText(context, "Pilih Kota!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            Thread {
                                var photoPath: String? = null
                                if (selectedImageUri != null) {
                                    // ... (Kode upload foto sama seperti sebelumnya) ...
                                    // Panggil uriToFile dan UploadApi
                                    try {
                                        val file = uriToFile(context, selectedImageUri!!)
                                        if (file != null) {
                                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                                            val response = UploadApi.create().uploadVisitPhoto(body).execute()
                                            if (response.isSuccessful && response.body()?.status == "success") {
                                                photoPath = response.body()!!.path
                                            }
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }

                                val result = AuthRepository.registerOutlet(
                                    nama, email, password, noTelp, alamat, lat, lng, photoPath,
                                    selectedCity!!.id, // Kirim ID Kota
                                    selectedPayment,
                                    selectedTimePref
                                )

                                (context as android.app.Activity).runOnUiThread {
                                    isLoading = false
                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                    if (result.first) navController.popBackStack()
                                }
                            }.start()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Daftar Outlet")
                    }
                }
            }
        }
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}