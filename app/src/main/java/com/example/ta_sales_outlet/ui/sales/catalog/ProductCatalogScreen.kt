package com.example.ta_sales_outlet.ui.sales.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ta_sales_outlet.ui.components.ProductCard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.repository.ProductRepository
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCatalogScreen() {
    // State
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var filteredList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Warna Brand
    val brandColor = Color(0xFF1976D2)

    // Load Data Produk
    LaunchedEffect(Unit) {
        Thread {
            val data = ProductRepository.getAllProducts()
            productList = data
            filteredList = data
            isLoading = false
        }.start()
    }

    // Logic Pencarian
    fun performSearch(query: String) {
        searchQuery = query
        filteredList = if (query.isEmpty()) {
            productList
        } else {
            productList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.code?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            // Search Bar di Header
            Surface(
                shadowElevation = 4.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Katalog Produk",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { performSearch(it) },
                        placeholder = { Text("Cari baju, celana...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = brandColor
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // Tombol Keranjang (Nanti difungsikan)
            FloatingActionButton(
                onClick = { /* TODO: Ke Keranjang */ },
                containerColor = brandColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5)) // Background abu-abu muda
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = brandColor
                )
            } else if (filteredList.isEmpty()) {
                Text(
                    "Produk tidak ditemukan",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                // Tampilan Grid 2 Kolom
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { product ->
                        ProductCard(
                            product = product,
                            buttonText = "Orderkan",
                            buttonColor = brandColor,
                            onButtonClick = { selectedProduct ->
                                println("Sales memilih: ${selectedProduct.name}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(product: Product, brandColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // 1. Gambar Produk (Menggunakan Coil Library)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = product.photoUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    // Placeholder jika gambar loading/error
                    error = null // Bisa diganti gambar default dari resource
                )
            }

            // 2. Info Produk
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.code ?: "-",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Format Rupiah
                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                Text(
                    text = formatRp.format(product.price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = brandColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Tambah
                Button(
                    onClick = { /* TODO: Add to Cart Logic */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Text("Tambah", fontSize = 12.sp)
                }
            }
        }
    }
}