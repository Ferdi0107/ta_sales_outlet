package com.example.ta_sales_outlet.ui.outlet.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// Import komponen yang sama dengan Sales
import com.example.ta_sales_outlet.data.local.CartManager
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.repository.ProductRepository
import com.example.ta_sales_outlet.ui.components.ProductCard
import com.example.ta_sales_outlet.ui.components.VariantBottomSheet
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletProductCatalogScreen(navController: NavController) {
    val context = LocalContext.current

    // --- STATE ---
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // State untuk BottomSheet Varian
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    // State Keranjang (Pantau CartManager)
    // Trik: Baca size agar Compose merender ulang saat ada item masuk
    val cartSize = CartManager.items.size
    val totalItem = CartManager.getTotalItems()
    val totalPrice = CartManager.getTotalPrice()

    // --- LOAD DATA ---
    LaunchedEffect(Unit) {
        // Ambil data produk menggunakan Repository yang sudah ada
        // Menggunakan Dispatchers.IO untuk thread background
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val data = ProductRepository.getAllProducts(context)
            // Update UI di Main Thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                productList = data
                isLoading = false
            }
        }
    }

    // Filter Pencarian
    val filteredList = if (searchQuery.isEmpty()) {
        productList
    } else {
        productList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.code?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Belanja Stok", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari baju, celana...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        disabledContainerColor = Color(0xFFF5F5F5),

                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        },
        // Floating Bottom Bar (Muncul jika ada isi keranjang)
        bottomBar = {
            if (totalItem > 0) {
                OutletCartSummaryBar(
                    totalItem = totalItem,
                    totalPrice = totalPrice,
                    onClick = {
                        // Arahkan ke Halaman Cart Outlet
                        navController.navigate("outlet_cart")
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9F9))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Text("Produk tidak ditemukan", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { product ->
                        // REUSE: Menggunakan ProductCard milik Sales
                        ProductCard(
                            product = product,
                            onCardClick = { clickedProduct ->
                                selectedProduct = clickedProduct
                            }
                        )
                    }
                    // Spacer agar item terbawah tidak tertutup CartBar
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // REUSE: Menggunakan VariantBottomSheet milik Sales
        if (selectedProduct != null) {
            VariantBottomSheet(
                product = selectedProduct!!,
                onDismiss = {
                    selectedProduct = null // Tutup Sheet
                }
            )
        }
    }
}

// Komponen Bar Keranjang (Sama persis dengan sales, cuma ganti nama fungsi biar rapi)
@Composable
fun OutletCartSummaryBar(totalItem: Int, totalPrice: Double, onClick: () -> Unit) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$totalItem Item dipilih",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text = formatRp.format(totalPrice),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lihat Keranjang", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
            }
        }
    }
}