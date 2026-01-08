package com.example.ta_sales_outlet.ui.outlet.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ta_sales_outlet.data.local.CartManager
import com.example.ta_sales_outlet.data.model.Outlet
import com.example.ta_sales_outlet.data.repository.OrderRepository
import com.example.ta_sales_outlet.data.repository.OutletRepository
import com.example.ta_sales_outlet.utils.SessionManager
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ta_sales_outlet.utils.UrlHelper
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletCartScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Data Cart
    val cartItems = CartManager.items.values.toList()
    val totalPrice = CartManager.getTotalPrice()
    val cartSize = CartManager.items.size
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // --- STATE UTAMA ---
    var notes by remember { mutableStateOf("") }

    // List Outlet Milik User Ini
    var myOutlets by remember { mutableStateOf<List<Outlet>>(emptyList()) }
    var selectedOutlet by remember { mutableStateOf<Outlet?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) } // Untuk kalau outlet > 1

    // State Pembayaran
    val paymentOptions = listOf("CASH", "CREDIT", "KONSINYASI")
    var selectedPayment by remember { mutableStateOf(paymentOptions[0]) }
    var creditTerm by remember { mutableStateOf("90") }

    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingOutlet by remember { mutableStateOf(true) }

    // --- 1. LOAD DATA OUTLET BERDASARKAN USER ID ---
    LaunchedEffect(Unit) {
        val userId = SessionManager.userId

        scope.launch(Dispatchers.IO) {
            // Ambil semua cabang milik user ini
            val outlets = OutletRepository.getOutletsByUser(userId)

            withContext(Dispatchers.Main) {
                myOutlets = outlets
                isLoadingOutlet = false

                // LOGIKA CERDAS:
                if (outlets.isNotEmpty()) {
                    // Jika cuma 1, langsung pilih
                    if (outlets.size == 1) {
                        selectedOutlet = outlets.first()
                    }
                    // Jika > 1, biarkan selectedOutlet null dulu (user harus pilih),
                    // atau bisa juga default ke yang pertama:
                    // selectedOutlet = outlets.first()
                }
            }
        }
    }

    // --- 2. SINKRONISASI PAYMENT SAAT OUTLET BERUBAH ---
    LaunchedEffect(selectedOutlet) {
        if (selectedOutlet != null) {
            val dbPayment = selectedOutlet!!.paymentType?.uppercase()
            if (dbPayment != null && paymentOptions.contains(dbPayment)) {
                selectedPayment = dbPayment
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keranjang Belanja", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(shadowElevation = 16.dp, color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Pembayaran", color = Color.Gray)
                            Text(formatRp.format(totalPrice), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1976D2))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (!isSubmitting) {
                                    if (selectedOutlet == null) {
                                        Toast.makeText(context, "Mohon pilih Toko/Cabang tujuan.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val termDays = if (selectedPayment == "CREDIT") creditTerm.toIntOrNull() ?: 90 else 0

                                    isSubmitting = true
                                    Thread {
                                        val success = OrderRepository.createOrder(
                                            userId = SessionManager.userId,
                                            channel = "SELF_ORDER",
                                            outletId = selectedOutlet!!.id,
                                            paymentMethod = selectedPayment,
                                            notes = notes,
                                            items = cartItems,
                                            totalPrice = totalPrice,
                                            creditTermInDays = termDays
                                        )

                                        (context as android.app.Activity).runOnUiThread {
                                            isSubmitting = false
                                            if (success) {
                                                Toast.makeText(context, "Pesanan Berhasil Dikirim!", Toast.LENGTH_LONG).show()
                                                CartManager.clearCart()
                                                navController.navigate("outlet_home") {
                                                    popUpTo("outlet_home") { inclusive = true }
                                                }
                                            } else {
                                                Toast.makeText(context, "Gagal order. Cek koneksi.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }.start()
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Buat Pesanan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Keranjang Kosong", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Kembali Belanja")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                // ==========================================
                // BAGIAN PILIH OUTLET / CABANG
                // ==========================================
                Text("Dikirim ke Cabang:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingOutlet) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (myOutlets.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Text("Tidak ada outlet terhubung ke akun ini.", color = Color.Red, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    // LOGIKA TAMPILAN:
                    // Jika > 1 Outlet -> Pakai Dropdown
                    // Jika 1 Outlet -> Tampilkan Kartu Static

                    if (myOutlets.size > 1) {
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedOutlet?.name ?: "Pilih Cabang...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.Store, null) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                            )
                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                myOutlets.forEach { outlet ->
                                    DropdownMenuItem(
                                        text = { Text(outlet.name) },
                                        onClick = {
                                            selectedOutlet = outlet
                                            isDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Cuma 1 Outlet (Tampilan Static yang Cantik)
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(Color(0xFFE3F2FD), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Store, null, tint = Color(0xFF1976D2))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(selectedOutlet?.name ?: "Loading...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(selectedOutlet?.address ?: "-", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Tampilkan Alamat Detail jika sudah ada yang dipilih (Baik auto maupun manual)
                    if (selectedOutlet != null && myOutlets.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedOutlet?.address ?: "-", fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ... (SISA KODE SAMA: LIST BARANG, PAYMENT, CATATAN) ...
                // 2. LIST ITEM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daftar Barang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Lagi")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                cartItems.forEach { item ->
                    CartItemRowOutlet(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // 3. PEMBAYARAN
                Text("Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column {
                        paymentOptions.forEach { method ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (method == selectedPayment),
                                            onClick = { selectedPayment = method }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = (method == selectedPayment), onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(method)
                                }
                                if (method == "CREDIT" && selectedPayment == "CREDIT") {
                                    Row(
                                        modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tempo:", fontSize = 14.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedTextField(
                                            value = creditTerm,
                                            onValueChange = { if (it.all { char -> char.isDigit() }) creditTerm = it },
                                            modifier = Modifier.width(120.dp),
                                            singleLine = true,
                                            suffix = { Text("Hari") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                        )
                                    }
                                }
                                if (method != paymentOptions.last()) Divider(modifier = Modifier.padding(start = 56.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // CATATAN
                Text("Catatan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pesan khusus untuk admin...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// Reuse Komponen Row (Sama seperti sebelumnya)
@Composable
fun CartItemRowOutlet(item: CartManager.CartItem) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val currentQty = CartManager.items[item.variant.id]?.qty ?: 0

    if (currentQty > 0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!item.product.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        // PERBAIKAN 1: Gunakan UrlHelper untuk dapat link lengkap
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(UrlHelper.getFullImageUrl(item.product.photoUrl))
                            .crossfade(true) // Biar munculnya halus
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,

                        // PERBAIKAN 2: Tambahkan indikator error biar ketahuan kalau gagal
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Default.BrokenImage)
                    )
                } else {
                    Box(Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Text("Varian: ${item.variant.size} - ${item.variant.color}", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatRp.format(item.product.price), color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { CartManager.removeVariant(item.variant.id) }, modifier = Modifier.size(30.dp)) {
                        Icon(if (currentQty > 1) Icons.Default.Remove else Icons.Default.Delete, "Kurang", tint = Color.Gray)
                    }
                    Text(currentQty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { CartManager.addVariant(item.product, item.variant) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, "Tambah", tint = Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}