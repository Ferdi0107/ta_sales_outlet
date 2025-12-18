package com.example.ta_sales_outlet.ui.sales.catalog

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
import com.example.ta_sales_outlet.data.local.CartManager // Import CartManager
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.repository.ProductRepository
import com.example.ta_sales_outlet.ui.components.ProductCard // Import ProductCard
import java.text.NumberFormat
import java.util.*
import com.example.ta_sales_outlet.ui.components.VariantBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen(navController: NavController) {
    val context = LocalContext.current

    // State Data Produk
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    // State Keranjang (Mengamati perubahan di CartManager)
    // trik: kita baca size-nya agar UI merender ulang saat ada item ditambah/kurang
    val cartSize = CartManager.items.size
    val totalItem = CartManager.getTotalItems()
    val totalPrice = CartManager.getTotalPrice()

    // Load Data Produk
    LaunchedEffect(Unit) {
        Thread {
            productList = ProductRepository.getAllProducts(context)
            isLoading = false
        }.start()
    }

    // Filter Pencarian
    val filteredList = if (searchQuery.isEmpty()) {
        productList
    } else {
        productList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Buat Pesanan", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // Search Bar Sederhana
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari produk...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        },
        // Floating Bottom Bar (Keranjang)
        bottomBar = {
            if (totalItem > 0) {
                CartSummaryBar(
                    totalItem = totalItem,
                    totalPrice = totalPrice,
                    onClick = {
                        navController.navigate("cart_checkout")
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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { product ->
                        ProductCard(
                            product = product,
                            onCardClick = { clickedProduct ->
                                // SAAT DIKLIK, ISI STATE selectedProduct
                                // INI AKAN MEMICU MODAL MUNCUL
                                selectedProduct = clickedProduct
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
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

// Komponen Bar Keranjang Melayang di Bawah
@Composable
fun CartSummaryBar(totalItem: Int, totalPrice: Double, onClick: () -> Unit) {
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