package com.example.ta_sales_outlet.data.repository

import android.util.Log
import com.example.ta_sales_outlet.data.MySQLHelper
import java.sql.SQLException

data class IncomingVisit(
    val routeStopId: Int,
    val salesName: String,
    val date: String,     // Tanggal (misal: "Senin, 20 Okt")
    val arrivalTime: String, // JAM (misal: "09:30") <--- TAMBAHAN BARU
    val status: String
)

object OutletVisitRepository {

    // 1. GET LIST KUNJUNGAN (Status = DRAFT / PUBLISHED / PENDING)
    // Sesuai Schema: Join route_stops -> routes -> users
    // VERSI FIX: Mencari jadwal berdasarkan USER ID yang login
    fun getIncomingVisits(userId: Int): List<IncomingVisit> {
        val list = ArrayList<IncomingVisit>()
        val conn = MySQLHelper.connect() ?: return list

        try {
            // Ambil kolom planned_arrival
            val sql = """
                SELECT 
                    rs.idroute_stops, 
                    rs.status, 
                    rs.planned_arrival,  -- AMBIL INI
                    r.route_date, 
                    u.nama AS sales_name
                FROM route_stops rs
                JOIN routes r ON rs.routes_idroutes = r.idroutes
                LEFT JOIN users u ON r.sales_idusers = u.idusers
                JOIN outlets o ON rs.outlets_idoutlets = o.idoutlets
                WHERE o.outlet_idusers = ?
                  AND rs.status IN ('PUBLISHED', 'CONFIRMED', 'CANCELLED') 
                  AND r.route_date >= CURDATE()
                ORDER BY rs.planned_arrival ASC
            """

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, userId)
            val rs = stmt.executeQuery()

            // Formatter untuk mengambil jam saja dari DATETIME MySQL
            // Format MySQL: yyyy-MM-dd HH:mm:ss
            val timeParser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val dateFormatter = java.text.SimpleDateFormat("EEE, dd MMM", java.util.Locale("id", "ID"))

            while (rs.next()) {
                val rawDate = rs.getString("planned_arrival")

                // Parse Jam & Tanggal
                var jamStr = "--:--"
                var tglStr = rs.getString("route_date")

                try {
                    if (rawDate != null) {
                        val dateObj = timeParser.parse(rawDate)
                        jamStr = timeFormatter.format(dateObj ?: java.util.Date())
                        tglStr = dateFormatter.format(dateObj ?: java.util.Date())
                    }
                } catch (e: Exception) { e.printStackTrace() }

                list.add(IncomingVisit(
                    routeStopId = rs.getInt("idroute_stops"),
                    salesName = rs.getString("sales_name") ?: "Sales Admin",
                    date = tglStr,
                    arrivalTime = jamStr, // Masukkan Jam
                    status = rs.getString("status")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // 2. KONFIRMASI KUNJUNGAN (UPDATE 2 TABEL)
    fun confirmVisit(routeStopId: Int): Boolean {
        val conn = MySQLHelper.connect() ?: return false

        try {
            conn.autoCommit = false // Mulai Transaksi

            // --- STEP A: Update Tabel 'route_stops' ---
            val sqlRouteStop = """
                UPDATE route_stops 
                SET status = 'CONFIRMED', 
                    confirmed_by_outlet = 1, 
                    confirmed_at = NOW() 
                WHERE idroute_stops = ?
            """
            val stmtRS = conn.prepareStatement(sqlRouteStop)
            stmtRS.setInt(1, routeStopId)
            stmtRS.executeUpdate()


            val sqlUpdateSchedule = """
                UPDATE scheduled_outlet target
                JOIN (
                    SELECT r.schedules_idschedules, rs.outlets_idoutlets
                    FROM route_stops rs
                    JOIN routes r ON rs.routes_idroutes = r.idroutes
                    WHERE rs.idroute_stops = ?
                ) source ON target.schedules_idschedules = source.schedules_idschedules 
                        AND target.outlets_idoutlets = source.outlets_idoutlets
                SET target.status = 'CONFIRMED'
            """

            val stmtSched = conn.prepareStatement(sqlUpdateSchedule)
            stmtSched.setInt(1, routeStopId)
            stmtSched.executeUpdate()

            conn.commit() // Simpan Permanen
            conn.close()
            return true

        } catch (e: SQLException) {
            e.printStackTrace()
            try { conn.rollback() } catch (ex: Exception) {}
            return false
        }
    }

    // 3. TOLAK KUNJUNGAN
    fun rejectVisit(routeStopId: Int): Boolean {
        val conn = MySQLHelper.connect() ?: return false
        try {
            conn.autoCommit = false

            // Update route_stops jadi CANCELLED (atau REJECTED jika enum ditambah)
            // Di enum anda ada: 'SKIPPED', 'CANCELLED'. Kita pakai 'CANCELLED'
            val sqlRS = "UPDATE route_stops SET status = 'CANCELLED' WHERE idroute_stops = ?"
            val stmt = conn.prepareStatement(sqlRS)
            stmt.setInt(1, routeStopId)
            stmt.executeUpdate()

            // Update scheduled_outlet juga jadi CANCELLED
            val sqlSched = """
                UPDATE scheduled_outlet target
                JOIN (
                    SELECT r.schedules_idschedules, rs.outlets_idoutlets
                    FROM route_stops rs
                    JOIN routes r ON rs.routes_idroutes = r.idroutes
                    WHERE rs.idroute_stops = ?
                ) source ON target.schedules_idschedules = source.schedules_idschedules 
                        AND target.outlets_idoutlets = source.outlets_idoutlets
                SET target.status = 'CANCELLED'
            """
            val stmtSched = conn.prepareStatement(sqlSched)
            stmtSched.setInt(1, routeStopId)
            stmtSched.executeUpdate()

            conn.commit()
            conn.close()
            return true
        } catch (e: Exception) {
            try { conn.rollback() } catch (ex: Exception) {}
            return false
        }
    }
}