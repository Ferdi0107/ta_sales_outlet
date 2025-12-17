package com.example.ta_sales_outlet.utils

object UrlHelper {
    // Ganti 10.0.2.2 dengan IP Laptop jika pakai HP Fisik
    private const val BASE_URL = "http://10.0.2.2:8000"

    fun getFullImageUrl(subPath: String?): String {
        // Logika Default Image
        val cleanPath = if (subPath.isNullOrEmpty()) {
            "no_picture.png" // File default yang tadi Anda siapkan
        } else {
            subPath
        }

        // Gabungkan: http://... + /storage/ + nama_file.jpg
        return "$BASE_URL/storage/$cleanPath"
    }
}