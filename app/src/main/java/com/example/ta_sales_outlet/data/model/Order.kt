package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class Order(
    @SerializedName("idorders") val id: Int,
    @SerializedName("kode") val code: String?,
    @SerializedName("order_date") val date: String?,
    @SerializedName("grand_total") val total: Double,
    @SerializedName("status") val status: String?,
    @SerializedName("payment_status") val paymentStatus: String?
)