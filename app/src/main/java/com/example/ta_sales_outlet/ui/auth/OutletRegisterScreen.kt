package com.example.ta_sales_outlet.ui.auth

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletRegisterScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current

    // --- STATE FORM ---
    var namaToko by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") } // BARU
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var noTelp by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }

    // STATE JAM & HARI
    var jamBuka by remember { mutableStateOf("08:00") }
    var jamTutup by remember { mutableStateOf("17:00") }

    // Checkbox Hari
    val daysOptions = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    // Default senin-sabtu terpilih
    val selectedDays = remember { mutableStateListOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu") }

    // STATE FOTO & WILAYAH (SAMA SEPERTI SEBELUMNYA)
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var provinceList by remember { mutableStateOf<List<Province>>(emptyList()) }
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var cityList by remember { mutableStateOf<List<City>>(emptyList()) }
    var selectedCity by remember { mutableStateOf<City?>(null) }

    // Expanded States
    var isProvExpanded by remember { mutableStateOf(false) }
    var isCityExpanded by remember { mutableStateOf(false) }
    var isPaymentExpanded by remember { mutableStateOf(false) }
    var isTimeExpanded by remember { mutableStateOf(false) }

    // State Opsional
    val paymentTypes = listOf("CREDIT", "KONSINYASI")
    var selectedPayment by remember { mutableStateOf(paymentTypes[0]) }
    val timePrefs = listOf("PAGI", "SIANG")
    var selectedTimePref by remember { mutableStateOf(timePrefs[0]) }

    var isLoading by remember { mutableStateOf(false) }

    // ... (Kode Load Provinsi & Kota SAMA SEPERTI SEBELUMNYA, tidak perlu diubah) ...
    // Copy Paste bagian LaunchedEffect WilayahRepository disini
    LaunchedEffect(Unit) {
        WilayahRepository.getProvinces { list -> provinceList = list }
    }
    LaunchedEffect(selectedProvince) {
        if (selectedProvince != null) {
            selectedCity = null
            cityList = emptyList()
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val cities = RegionRepository.getCitiesByProvinceName(selectedProvince!!.name)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { cityList = cities }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    // FUNGSI HELPER TIME PICKER
    fun showTimePicker(initialTime: String, onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val parts = initialTime.split(":")
        val hour = if (parts.size == 2) parts[0].toInt() else cal.get(Calendar.HOUR_OF_DAY)
        val minute = if (parts.size == 2) parts[1].toInt() else cal.get(Calendar.MINUTE)

        TimePickerDialog(context, { _, h, m ->
            val formatted = String.format("%02d:%02d", h, m)
            onTimeSelected(formatted)
        }, hour, minute, true).show()
    }

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
            // ... (BAGIAN FOTO & MAP SAMA) ...
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

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Lokasi Toko", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                LocationPicker(
                    modifier = Modifier.fillMaxWidth(),
                    onLocationSelected = { la, lo, add -> lat = la; lng = lo; if (alamat.isEmpty()) alamat = add }
                )

                Column(modifier = Modifier.padding(16.dp)) {

                    Text("Informasi Dasar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 1. NAMA TOKO
                    OutlinedTextField(value = namaToko, onValueChange = { namaToko = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. CONTACT PERSON (BARU)
                    OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Nama Pemilik / Contact Person") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = noTelp, onValueChange = { if (it.all { c -> c.isDigit() }) noTelp = it }, label = { Text("No. Telepon") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Detail & Wilayah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // PROVINSI & KOTA (SAMA)
                    // ... (Kode Dropdown Provinsi & Kota SAMA seperti sebelumnya) ...
                    ExposedDropdownMenuBox(expanded = isProvExpanded, onExpandedChange = { isProvExpanded = !isProvExpanded }) {
                        OutlinedTextField(value = selectedProvince?.name ?: "Pilih Provinsi", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProvExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Provinsi") })
                        ExposedDropdownMenu(expanded = isProvExpanded, onDismissRequest = { isProvExpanded = false }) {
                            provinceList.forEach { prov -> DropdownMenuItem(text = { Text(prov.name) }, onClick = { selectedProvince = prov; isProvExpanded = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = isCityExpanded, onExpandedChange = { isCityExpanded = !isCityExpanded }) {
                        OutlinedTextField(value = selectedCity?.name ?: "Pilih Kota/Kabupaten", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCityExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Kota/Kabupaten") }, enabled = selectedProvince != null)
                        ExposedDropdownMenu(expanded = isCityExpanded, onDismissRequest = { isCityExpanded = false }) {
                            cityList.forEach { city -> DropdownMenuItem(text = { Text(city.name) }, onClick = { selectedCity = city; isCityExpanded = false }) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Operasional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // JAM BUKA & TUTUP
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedTextField(
                            value = jamBuka, onValueChange = {}, readOnly = true,
                            label = { Text("Jam Buka") },
                            modifier = Modifier.weight(1f).clickable { showTimePicker(jamBuka) { jamBuka = it } },
                            trailingIcon = { IconButton(onClick = { showTimePicker(jamBuka) { jamBuka = it } }) { Icon(Icons.Default.AccessTime, null) } },
                            enabled = false, // Biar klik tembus ke parent clickable
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black, disabledTrailingIconColor = Color.Black)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = jamTutup, onValueChange = {}, readOnly = true,
                            label = { Text("Jam Tutup") },
                            modifier = Modifier.weight(1f).clickable { showTimePicker(jamTutup) { jamTutup = it } },
                            trailingIcon = { IconButton(onClick = { showTimePicker(jamTutup) { jamTutup = it } }) { Icon(Icons.Default.AccessTime, null) } },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledLabelColor = Color.Black, disabledTrailingIconColor = Color.Black)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Hari Buka:", fontSize = 14.sp)
                    // HARI BUKA (FlowRow Checkboxes)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        daysOptions.forEach { day ->
                            FilterChip(
                                selected = selectedDays.contains(day),
                                onClick = {
                                    if (selectedDays.contains(day)) selectedDays.remove(day)
                                    else selectedDays.add(day)
                                },
                                label = { Text(day) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // PEMBAYARAN & WAKTU PREF (SAMA)
                    ExposedDropdownMenuBox(expanded = isPaymentExpanded, onExpandedChange = { isPaymentExpanded = !isPaymentExpanded }) {
                        OutlinedTextField(value = selectedPayment, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Tipe Pembayaran") })
                        ExposedDropdownMenu(expanded = isPaymentExpanded, onDismissRequest = { isPaymentExpanded = false }) {
                            paymentTypes.forEach { type -> DropdownMenuItem(text = { Text(type) }, onClick = { selectedPayment = type; isPaymentExpanded = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = isTimeExpanded, onExpandedChange = { isTimeExpanded = !isTimeExpanded }) {
                        OutlinedTextField(value = selectedTimePref, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTimeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Preferensi Waktu Kunjungan") })
                        ExposedDropdownMenu(expanded = isTimeExpanded, onDismissRequest = { isTimeExpanded = false }) {
                            timePrefs.forEach { time -> DropdownMenuItem(text = { Text(time) }, onClick = { selectedTimePref = time; isTimeExpanded = false }) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = alamat, onValueChange = { alamat = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (namaToko.isBlank() || contactPerson.isBlank() || email.isBlank() || selectedDays.isEmpty()) {
                                Toast.makeText(context, "Data belum lengkap (cek hari buka/nama)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedCity == null) {
                                Toast.makeText(context, "Pilih Kota!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            Thread {
                                var photoPath: String? = null
                                // ... (UPLOAD PHOTO LOGIC SAMA) ...
                                if (selectedImageUri != null) {
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
                                    namaToko = namaToko,
                                    contactPerson = contactPerson,
                                    email = email,
                                    password = password,
                                    noTelp = noTelp,
                                    alamat = alamat,
                                    lat = lat,
                                    lng = lng,
                                    photoPath = photoPath,
                                    cityId = selectedCity!!.id,
                                    paymentType = selectedPayment,
                                    timePref = selectedTimePref,
                                    // PARAMETER BARU
                                    jamBuka = jamBuka,
                                    jamTutup = jamTutup,
                                    hariBuka = selectedDays.joinToString(",") // Ubah list jadi string "Senin,Selasa"
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

// Letakkan di paling bawah file, diluar fungsi Composable
fun uriToFile(context: Context, uri: android.net.Uri): java.io.File? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        // Membuat file temporary di cache directory aplikasi
        val tempFile = java.io.File.createTempFile("image_upload", ".jpg", context.cacheDir)
        val outputStream = java.io.FileOutputStream(tempFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}