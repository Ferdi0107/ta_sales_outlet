package com.example.ta_sales_outlet.data.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Model Data Provinsi dari wilayah.id
data class ProvinceResponse(
    val data: List<Province>
)

data class Province(
    val code: String,
    val name: String
)

// Interface Retrofit
interface WilayahService {
    @GET("api/provinces.json")
    fun getProvinces(): Call<ProvinceResponse>
}

// Object Repository Wilayah
object WilayahRepository {
    private const val BASE_URL = "https://wilayah.id/"

    private val service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WilayahService::class.java)

    // Ambil Provinsi dari API
    fun getProvinces(callback: (List<Province>) -> Unit) {
        Thread {
            try {
                val response = service.getProvinces().execute()
                if (response.isSuccessful) {
                    callback(response.body()?.data ?: emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(emptyList())
            }
        }.start()
    }
}