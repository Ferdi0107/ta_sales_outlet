package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class ProductVariant(
    @SerializedName("idproduct_variants") val id: Int,
    @SerializedName("size") val size: String?,
    @SerializedName("color") val color: String?,
    @SerializedName("stock") val stock: Int = 0,
    @SerializedName("min_order") val minOrder: Int?,
    @SerializedName("products_idproducts") val productId: Int
) {
    // Helper untuk menampilkan nama lengkap varian (Misal: "L - Merah")
    fun getFullName(): String {
        return "$size - $color"
    }
}