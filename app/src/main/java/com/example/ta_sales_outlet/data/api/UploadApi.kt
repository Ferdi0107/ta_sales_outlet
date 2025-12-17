package com.example.ta_sales_outlet.data.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Model respon dari Laravel
data class UploadResponse(val status: String, val path: String)

interface UploadApi {
    @Multipart
    @POST("api/upload-visit") // Sesuaikan dengan route Laravel tadi
    fun uploadVisitPhoto(
        @Part photo: MultipartBody.Part
    ): Call<UploadResponse>

    companion object {
        // Setup Retrofit Sederhana
        fun create(): UploadApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/") // Sesuaikan IP
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(UploadApi::class.java)
        }
    }
}