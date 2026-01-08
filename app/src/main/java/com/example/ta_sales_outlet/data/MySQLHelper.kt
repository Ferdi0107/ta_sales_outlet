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
//    private const val IP = "192.168.1.8"
    private const val PORT = "3306"
    private const val DB_NAME = "skripsi" // Ganti sesuai nama database di phpMyAdmin kamu
    private const val USER = "root"
    private const val PASS = ""
//    private const val USER = "skripsi_user"
//    private const val PASS = "skripsi_user"

    fun connect(): Connection? {
        var conn: Connection? = null
        val url = "jdbc:mysql://$IP:$PORT/$DB_NAME"

        try {
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

        try {
            // 1. Matikan Auto-Commit untuk memulai Transaksi
            // (Supaya kalau insert gagal, update status juga dibatalkan)
            conn.autoCommit = false

            // --- QUERY 1: INSERT KE TABEL VISITS ---
            // Kita gabungkan info Lokasi ke dalam Notes karena belum ada kolom lat/lng di tabel visits
            val fullNotes = "$notes (Loc: $lat, $lng)"

            val sqlInsert = """
                INSERT INTO visits (
                    route_stops_idroute_stops, 
                    check_in, 
                    status, 
                    img_url, 
                    notes, 
                    created_at, 
                    updated_at
                ) VALUES (?, NOW(), 'VISITED', ?, ?, NOW(), NOW())
            """

            val stmtInsert = conn.prepareStatement(sqlInsert)
            stmtInsert.setInt(1, stopId)      // route_stops_idroute_stops
            stmtInsert.setString(2, photoPath) // img_url
            stmtInsert.setString(3, fullNotes) // notes

            val rowsInserted = stmtInsert.executeUpdate()

            // --- QUERY 2: UPDATE STATUS DI ROUTE_STOPS ---
            val sqlUpdate = """
                UPDATE route_stops 
                SET status = 'VISITED', 
                    updated_at = NOW() 
                WHERE idroute_stops = ?
            """

            val stmtUpdate = conn.prepareStatement(sqlUpdate)
            stmtUpdate.setInt(1, stopId)

            val rowsUpdated = stmtUpdate.executeUpdate()

            // 2. Cek apakah kedua aksi berhasil?
            if (rowsInserted > 0 && rowsUpdated > 0) {
                conn.commit() // Simpan perubahan permanen
                conn.close()
                return true
            } else {
                conn.rollback() // Batalkan jika ada yang gagal
                conn.close()
                return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                conn.rollback() // Batalkan transaksi jika error
                conn.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
            return false
        }
    }
}