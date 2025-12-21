package com.example.ta_sales_outlet.ui.sales.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import android.util.Log
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController) {
    val context = LocalContext.current
    val cartItems = CartManager.items.values.toList()
    val totalPrice = CartManager.getTotalPrice()
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // --- STATE FORM ---
    var notes by remember { mutableStateOf("") }

    // STATE OUTLET (SEARCHABLE DROPDOWN)
    var outletList by remember { mutableStateOf<List<Outlet>>(emptyList()) }
    var selectedOutlet by remember { mutableStateOf<Outlet?>(null) }
    var isOutletDropdownExpanded by remember { mutableStateOf(false) }

    // State baru untuk Text Pencarian
    var searchText by remember { mutableStateOf("") }

    // Logic Filter List berdasarkan ketikan user
    val filteredOutlets = remember(outletList, searchText) {
        if (searchText.isBlank()) {
            outletList // Jika kosong, tampilkan semua
        } else {
            outletList.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        (it.address?.contains(searchText, ignoreCase = true) == true)
            }
        }
    }

    val paymentOptions = listOf("CASH", "CREDIT", "KONSINYASI")
    var selectedPayment by remember { mutableStateOf(paymentOptions[0]) }

    // STATE UNTUK TEMPO KREDIT
    var creditTerm by remember { mutableStateOf("90") }

    var isSubmitting by remember { mutableStateOf(false) }

    // 1. LOAD DATA OUTLET & SET DEFAULT DARI SESSION
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val allOutlets = OutletRepository.getAllOutletsSimple()

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                outletList = allOutlets

                val sessionOutletId = SessionManager.currentOutletId
                if (sessionOutletId != 0) {
                    val preSelected = allOutlets.find { it.id == sessionOutletId }
                    if (preSelected != null) {
                        selectedOutlet = preSelected
                    }
                }
            }
        }
    }

    // 2. SYNC TEXTFIELD: Jika Outlet terpilih (dari Session/Klik), update teksnya
    LaunchedEffect(selectedOutlet) {
        if (selectedOutlet != null) {
            searchText = selectedOutlet!!.name // Tampilkan nama outlet di kotak ketik

            // Update Payment Method otomatis
            val defaultType = selectedOutlet?.paymentType?.uppercase() ?: "CASH"
            if (paymentOptions.contains(defaultType)) {
                selectedPayment = defaultType
            } else {
                selectedPayment = "CASH"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Pesanan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                                        Toast.makeText(context, "Mohon pilih Outlet tujuan dulu!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val termDays = if (selectedPayment == "CREDIT" || selectedPayment == "KREDIT") {
                                        creditTerm.toIntOrNull() ?: 90
                                    } else {
                                        0
                                    }

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
                                                Toast.makeText(context, "Order Berhasil Dibuat!", Toast.LENGTH_LONG).show()
                                                CartManager.clearCart()
                                                navController.popBackStack("visit_detail/${selectedOutlet!!.id}", inclusive = false)
                                                navController.popBackStack()
                                            } else {
                                                Toast.makeText(context, "Gagal membuat order. Cek koneksi.", Toast.LENGTH_SHORT).show()
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
                                Text("Kirim Pesanan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

                // ============================================
                // 1. SEARCHABLE OUTLET DROPDOWN (DIPERBAIKI)
                // ============================================
                Text("Outlet Tujuan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isOutletDropdownExpanded,
                    onExpandedChange = {
                        // Jangan langsung toggle, tapi force expand saat diklik agar user bisa ngetik
                        isOutletDropdownExpanded = !isOutletDropdownExpanded
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { query ->
                            searchText = query
                            isOutletDropdownExpanded = true // Otomatis buka menu saat ngetik

                            // Jika user mengetik, kita reset selectedOutlet sementara agar user "dipaksa" memilih ulang dari list
                            // Kecuali teks-nya sama persis dengan nama outlet (opsional)
                            if (selectedOutlet != null && query != selectedOutlet!!.name) {
                                selectedOutlet = null
                            }
                        },
                        label = { Text("Cari nama toko...") },
                        // readOnly FALSE agar bisa diketik
                        readOnly = false,
                        trailingIcon = {
                            // Tambahkan tombol silang (X) untuk hapus teks dengan cepat
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchText = ""
                                    selectedOutlet = null
                                    isOutletDropdownExpanded = true
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOutletDropdownExpanded)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null) }, // Ganti ikon Toko jadi Search biar intuitif
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )

                    // Menu Item
                    ExposedDropdownMenu(
                        expanded = isOutletDropdownExpanded,
                        onDismissRequest = { isOutletDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 250.dp) // Batasi tinggi dropdown biar gak menuhin layar
                    ) {
                        if (filteredOutlets.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Outlet tidak ditemukan", color = Color.Gray) },
                                onClick = {}
                            )
                        } else {
                            filteredOutlets.forEach { outlet ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(outlet.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                outlet.address ?: "-",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedOutlet = outlet
                                        searchText = outlet.name // Set teks jadi nama outlet
                                        isOutletDropdownExpanded = false // Tutup menu
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. LIST ITEM BELANJAAN
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
                    CartItemRow(item)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // 3. INPUT PEMBAYARAN
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

                                if ((method == "CREDIT" || method == "KREDIT") && (selectedPayment == "CREDIT" || selectedPayment == "KREDIT")) {
                                    Row(
                                        modifier = Modifier
                                            .padding(start = 56.dp, end = 16.dp, bottom = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Tempo:", fontSize = 14.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedTextField(
                                            value = creditTerm,
                                            onValueChange = {
                                                if (it.all { char -> char.isDigit() }) {
                                                    creditTerm = it
                                                }
                                            },
                                            modifier = Modifier.width(120.dp),
                                            singleLine = true,
                                            suffix = { Text("Hari") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                        )
                                    }
                                }

                                if (method != paymentOptions.last()) {
                                    Divider(color = Color.LightGray.copy(alpha=0.3f), modifier = Modifier.padding(start = 56.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Catatan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Isi catatan...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ... (Composable CartItemRow TETAP SAMA) ...
@Composable
fun CartItemRow(item: CartManager.CartItem) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val currentQty = CartManager.items[item.variant.id]?.qty ?: 0

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = com.example.ta_sales_outlet.utils.UrlHelper.getFullImageUrl(item.product.photoUrl),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Varian: ${item.variant.size} - ${item.variant.color}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRp.format(item.product.price),
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { CartManager.removeVariant(item.variant.id) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        if (currentQty > 1) Icons.Default.Remove else Icons.Default.Delete,
                        contentDescription = "Kurang",
                        tint = Color.Gray
                    )
                }

                Text(
                    text = currentQty.toString(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { CartManager.addVariant(item.product, item.variant) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color(0xFF1976D2))
                }
            }
        }
    }
}