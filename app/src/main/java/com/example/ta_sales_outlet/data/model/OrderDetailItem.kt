package com.example.ta_sales_outlet.data.model

data class OrderDetailItem(
    val productName: String,
    val variantName: String, // Gabungan Size - Color
    val qty: Int,
    val price: Double,       // Harga Satuan
    val subTotal: Double,    // Harga x Qty
    val photoUrl: String?
)