package com.example.ta_sales_outlet.data

import android.os.StrictMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object MySQLHelper {
    // PENTING:
    // Jika pakai Emulator: 10.0.2.2
    // Jika pakai HP Fisik: Ganti dengan IP Laptop (contoh: 192.168.1.5)
    private const val IP = "10.0.2.2"
    private const val PORT = "3306"
    private const val DB_NAME = "skripsi" // Ganti sesuai nama database di phpMyAdmin kamu
    private const val USER = "root" // Default XAMPP biasanya root
    private const val PASS = ""     // Default XAMPP biasanya kosong

    fun connect(): Connection? {
        var conn: Connection? = null
        val url = "jdbc:mysql://$IP:$PORT/$DB_NAME"

        try {
            // Izin akses Network di Main Thread (Khusus metode kampus/direct)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            Class.forName("com.mysql.jdbc.Driver")
            conn = DriverManager.getConnection(url, USER, PASS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return conn
    }

}