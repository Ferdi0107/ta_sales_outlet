package com.example.ta_sales_outlet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ta_sales_outlet.data.model.Product
import java.text.NumberFormat
import java.util.*

@Composable
fun ProductCard(
    product: Product,
    buttonText: String = "Tambah", // Bisa diganti "Beli", "Order", dll
    buttonColor: Color = Color(0xFF1976D2),
    onButtonClick: (Product) -> Unit // Callback aksi saat tombol ditekan
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Gambar Produk
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Info Produk
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

                val formatRp = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                Text(
                    text = formatRp.format(product.price),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = buttonColor // Warna harga mengikuti tema tombol
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Dinamis
                Button(
                    onClick = { onButtonClick(product) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text(buttonText, fontSize = 12.sp)
                }
            }
        }
    }
}