package com.example.ta_sales_outlet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ta_sales_outlet.data.local.CartManager
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.model.ProductVariant
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantBottomSheet(
    product: Product,
    onDismiss: () -> Unit
) {
    val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header Produk
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRp.format(product.price),
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text("Pilih Varian:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // 2. List Varian
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp) // Batasi tinggi agar tidak menutupi layar
            ) {
                if (product.variants.isEmpty()) {
                    item {
                        Text(
                            "Tidak ada varian tersedia.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(product.variants) { variant ->
                        VariantRowItem(product, variant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ... imports tetap sama ...

// ... imports tetap sama ...

@Composable
fun VariantRowItem(product: Product, variant: ProductVariant) {
    val cartItem = CartManager.items[variant.id]
    val currentQty = cartItem?.qty ?: 0
    val maxStock = variant.stock
    val brandColor = Color(0xFF1976D2)
    val isOutOfStock = maxStock <= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(if (isOutOfStock) Color(0xFFEEEEEE) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- INFO KIRI (Tetap) ---
        Column(modifier = Modifier.padding(start = if (isOutOfStock) 8.dp else 0.dp)) {
            Text(
                text = "${variant.size} - ${variant.color}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isOutOfStock) Color.Gray else Color.Black
            )
            if (isOutOfStock) {
                Text("Stok Habis", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
            } else {
                Text("Stok: $maxStock", fontSize = 11.sp, color = Color.Gray)
            }
        }

        // --- KONTROL KANAN (DIUBAH) ---
        if (!isOutOfStock) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // 1. TOMBOL MINUS
                Box(
                    modifier = Modifier
                        .size(36.dp) // Sedikit diperbesar agar nyaman
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                        .clickable { CartManager.removeVariant(variant.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. INPUT ANGKA MANUAL (PERBAIKAN UTAMA)
                Box(
                    modifier = Modifier
                        .width(60.dp)  // Lebar fix sedikit lebih luas
                        .height(36.dp) // Tinggi disamakan dengan tombol +/-
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)) // Border lebih halus
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center // Kunci agar teks di tengah vertikal & horizontal
                ) {
                    BasicTextField(
                        // Tampilkan string kosong jika sedang diedit dan user menghapus angka,
                        // tapi logika update akan handle '0'
                        value = currentQty.toString(),
                        onValueChange = { input ->
                            // Filter hanya angka
                            if (input.all { it.isDigit() }) {
                                // Jika kosong, anggap 0. Jika ada angka, parse ke Int.
                                val newQty = if (input.isEmpty()) 0 else input.toIntOrNull() ?: 0

                                // Cegah input melebihi stok (opsional, UX lebih baik)
                                val finalQty = newQty.coerceAtMost(maxStock)

                                CartManager.updateVariantQty(product, variant, finalQty)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, // Rata tengah horizontal
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth() // Agar text align center bekerja
                            .padding(horizontal = 4.dp) // Padding kiri kanan agar angka panjang tidak nempel border
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 3. TOMBOL PLUS
                val isMaxReached = currentQty >= maxStock
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMaxReached) Color.LightGray else brandColor)
                        .clickable(enabled = !isMaxReached) {
                            CartManager.addVariant(product, variant)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}