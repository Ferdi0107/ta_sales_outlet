package com.example.ta_sales_outlet.utils

import com.example.ta_sales_outlet.data.model.User // Asumsi Anda punya model User, kalau tidak pakai String/Int saja

object SessionManager {
    // 1. Data User (Salesperson) yang sedang Login
    var userId: Int = 0
    var userName: String = ""
    var userRole: String = ""

    // 2. Data Konteks (Sedang berada di toko mana?)
    // Ini di-set saat user klik tombol "Kunjungi" di Home
    var currentOutletId: Int = 0
    var currentOutletName: String = ""

    // Fungsi untuk cek apakah data sales sudah siap
    fun isLoggedIn(): Boolean {
        return userId != 0
    }

    // Fungsi Logout (Bersihkan data)
    fun clearSession() {
        userId = 0
        userName = ""
        userRole = ""
        currentOutletId = 0
        currentOutletName = ""
    }
}