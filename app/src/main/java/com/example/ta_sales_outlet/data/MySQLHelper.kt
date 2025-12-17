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

    fun insertVisitCheckIn(stopId: Int, photoPath: String, lat: Double, lng: Double, notes: String): Boolean {
        val conn = connect() ?: return false
        return try {
            // Update tabel route_stops atau insert ke tabel visits (sesuai struktur DB Anda)
            // Disini saya asumsikan update status di route_stops dan simpan bukti

            // Contoh: Kita anggap ada kolom 'proof_photo', 'actual_lat', 'actual_lng', 'visit_time' di route_stops
            val sql = """
            UPDATE route_stops 
            SET status = 'VISITED',
                proof_photo = ?,
                actual_lat = ?,
                actual_lng = ?,
                visit_time = NOW(),
                notes = ?
            WHERE idroute_stops = ?
        """

            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, photoPath) // Path dari Laravel (visits/foto.jpg)
            stmt.setDouble(2, lat)
            stmt.setDouble(3, lng)
            stmt.setString(4, notes)
            stmt.setInt(5, stopId)

            val rows = stmt.executeUpdate()
            conn.close()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}