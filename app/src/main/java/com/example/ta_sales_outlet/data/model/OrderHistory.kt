package com.example.ta_sales_outlet.data.model

data class OrderHistory(
    val id: Int,
    val kode: String,
    val outletName: String,
    val total: Double,
    val status: String,         // PACKING, SHIPPED, dll
    val paymentStatus: String,  // PAID, NOT_DUE, dll
    val paymentMethod: String,  // CASH, CREDIT
    val date: String,            // YYYY-MM-DD

    val address: String? = null,
    val notes: String? = null
)