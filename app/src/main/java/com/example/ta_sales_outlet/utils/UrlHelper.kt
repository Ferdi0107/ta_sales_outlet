package com.example.ta_sales_outlet.utils

import android.util.Log

object UrlHelper {
    // Pastikan IP ini benar (10.0.2.2 untuk emulator, IP Laptop untuk HP Fisik)
    private const val BASE_URL = "http://10.0.2.2:8000"

    fun getFullImageUrl(subPath: String?): String {
        var path = subPath

        // 1. Logika Default
        if (path.isNullOrEmpty()) {
            return "$BASE_URL/storage/no_picture.png"
        }

        // 2. Bersihkan slash di depan (jika ada)
        if (path!!.startsWith("/")) {
            path = path.substring(1)
        }

        if (!path.contains("/") && !path.contains("no_picture")) {
            path = "products/$path"
        }
        // ---------------------------------------

        val finalUrl = "$BASE_URL/storage/$path"

        Log.d("DEBUG_IMG", "URL Final diperbaiki: $finalUrl")

        return finalUrl
    }
}