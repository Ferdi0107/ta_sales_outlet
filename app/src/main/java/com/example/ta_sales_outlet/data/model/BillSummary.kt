package com.example.ta_sales_outlet.data.model

data class BillSummary(
    val totalUnpaid: Double,       // Total semua utang
    val nearestDueDate: String?,   // Tanggal jatuh tempo terdekat (bisa null jika lunas semua)
    val nearestBillAmount: Double, // Nominal tagihan terdekat itu
    val countUnpaid: Int           // Jumlah invoice yang belum lunas
)