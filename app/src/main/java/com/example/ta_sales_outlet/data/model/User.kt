package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("idusers") val id: Int,
    @SerializedName("nama") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String?, // "SALESPERSON" atau "OUTLET"
    @SerializedName("no_telp") val phoneNumber: String?,
    @SerializedName("photo") val photoUrl: String?,
    @SerializedName("token") val token: String? // Token ini nanti didapat dari respon Login API
)