package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("idproducts") val id: Int,
    @SerializedName("kode") val code: String?,
    @SerializedName("nama") val name: String,
    @SerializedName("deskripsi") val description: String?,
    @SerializedName("price") val price: Double,
    @SerializedName("url_photo") val photoUrl: String?,
    @SerializedName("categories_idcategories") val categoryId: Int
)