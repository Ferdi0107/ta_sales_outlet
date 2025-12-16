package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

// T adalah "Generic", artinya bisa diisi User, Product, atau List<Product>
data class BaseResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)