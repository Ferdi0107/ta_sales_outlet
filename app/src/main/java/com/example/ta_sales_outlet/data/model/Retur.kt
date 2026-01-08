package com.example.ta_sales_outlet.data.model

data class ReturItem(
    val orderDetailId: Int,
    val productName: String,
    val variantName: String,
    val maxQty: Int,

    var inputQty: Int = 0,
    var isSelected: Boolean = false,
    val photoUrl: String? = null
)

// Enum Alasan (Sesuai kolom ENUM di database)
enum class ReturReason(val label: String, val dbValue: String) {
    DAMAGED("Barang Rusak", "DAMAGED"),
    WRONG_ITEM("Salah Barang", "WRONG_ITEM"),
    OTHER("Lainnya", "OTHER")
}