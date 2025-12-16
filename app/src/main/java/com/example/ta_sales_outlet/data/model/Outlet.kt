package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class Outlet(
    @SerializedName("idoutlets") val id: Int,
    @SerializedName("nama") val name: String,
    @SerializedName("alamat") val address: String?,
    @SerializedName("lat") val latitude: Double?,
    @SerializedName("lng") val longitude: Double?,
    @SerializedName("no_telp") val phone: String?,
    @SerializedName("contact_person") val contactPerson: String?,
    @SerializedName("tipe_pembayaran") val paymentType: String?
)