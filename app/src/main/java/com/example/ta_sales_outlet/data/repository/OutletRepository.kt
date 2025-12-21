package com.example.ta_sales_outlet.data.repository

import android.util.Log
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.Outlet
import com.example.ta_sales_outlet.data.model.BillSummary
import java.sql.ResultSet

object OutletRepository {

    fun getAllOutletsSimple(): List<Outlet> {
        val list = ArrayList<Outlet>()
        val conn = MySQLHelper.connect() ?: return list

        if (conn == null) {
            Log.e("DEBUG_OUTLET", "Koneksi Database GAGAL (Null)")
            return list
        } else {
            Log.d("DEBUG_OUTLET", "Koneksi Database BERHASIL")
        }

        try {
            val sql = "SELECT idoutlets, nama, alamat, tipe_pembayaran FROM outlets ORDER BY nama ASC"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)

            while (rs.next()) {
                // Buat object Outlet minimalis
                // Asumsi data class Outlet Anda punya parameter ini
                // Sesuaikan constructor data class Anda (isi null untuk yang tidak perlu)
                list.add(Outlet(
                    id = rs.getInt("idoutlets"),
                    name = rs.getString("nama"),
                    address = rs.getString("alamat"),
                    phone = null,
                    latitude = null,
                    longitude = null,
                    contactPerson = null,
                    paymentType = rs.getString("tipe_pembayaran"),
                ))
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getOutletDetail(outletId: Int): Outlet? {
        var outlet: Outlet? = null
        val conn = MySQLHelper.connect() ?: return null

        try {
            val sql = "SELECT idoutlets, nama, alamat, tipe_pembayaran, no_telp FROM outlets WHERE idoutlets = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, outletId)

            val rs: ResultSet = stmt.executeQuery()

            if (rs.next()) {
                outlet = Outlet(
                    id = rs.getInt("idoutlets"),
                    name = rs.getString("nama"),
                    address = rs.getString("alamat") ?: "-", // Ambil Alamat
                    phone = rs.getString("no_telp"),
                    latitude = null,
                    longitude = null,
                    contactPerson = null,
                    paymentType = rs.getString("tipe_pembayaran") // Ambil Tipe Bayar (CREDIT/CASH/KONSINYASI)
                )
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return outlet
    }

    fun getOutletsByUser(userId: Int): List<Outlet> {
        val list = ArrayList<Outlet>()
        val conn = MySQLHelper.connect() ?: return list

        try {
            // WHERE outlet_idusers = ? (Pastikan nama kolom FK di DB anda benar)
            val sql = "SELECT idoutlets, nama, alamat, tipe_pembayaran, no_telp FROM outlets WHERE outlet_idusers = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, userId)

            val rs: ResultSet = stmt.executeQuery()

            while (rs.next()) {
                list.add(Outlet(
                    id = rs.getInt("idoutlets"),
                    name = rs.getString("nama"),
                    address = rs.getString("alamat") ?: "-",
                    phone = rs.getString("no_telp"),
                    latitude = null,
                    longitude = null,
                    contactPerson = null,
                    paymentType = rs.getString("tipe_pembayaran")
                ))
            }

            Log.d("DEBUG_REPO", "User ID $userId punya ${list.size} outlet.")
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getBillSummary(userId: Int): BillSummary {
        var summary = BillSummary(0.0, null, 0.0, 0)
        val conn = MySQLHelper.connect() ?: return summary

        try {
            // 1. HITUNG TOTAL & JUMLAH NOTA BELUM LUNAS
            // Asumsi: Kita cari outlet milik user ini dulu, lalu cari ordernya
            val sqlTotal = """
                SELECT SUM(o.grand_total) as total_hutang, COUNT(o.idorders) as jumlah_nota
                FROM orders o
                JOIN outlets outl ON o.outlets_idoutlets = outl.idoutlets
                WHERE outl.outlet_idusers = ? 
                AND o.payment_status IN ('NOT_DUE', 'AWAITING_PAYMENT')
            """
            val stmtTotal = conn.prepareStatement(sqlTotal)
            stmtTotal.setInt(1, userId)
            val rsTotal = stmtTotal.executeQuery()

            var totalAll = 0.0
            var count = 0

            if (rsTotal.next()) {
                totalAll = rsTotal.getDouble("total_hutang")
                count = rsTotal.getInt("jumlah_nota")
            }

            // 2. CARI YANG JATUH TEMPO PALING DEKAT
            // Urutkan berdasarkan due_date ASC (Ascending/Meningkat) -> yang paling kecil (terlampaui/dekat) muncul duluan
            val sqlNearest = """
                SELECT o.grand_total, o.due_date
                FROM orders o
                JOIN outlets outl ON o.outlets_idoutlets = outl.idoutlets
                WHERE outl.outlet_idusers = ? 
                AND o.payment_status IN ('NOT_DUE', 'AWAITING_PAYMENT')
                ORDER BY o.due_date ASC
                LIMIT 1
            """
            val stmtNearest = conn.prepareStatement(sqlNearest)
            stmtNearest.setInt(1, userId)
            val rsNearest = stmtNearest.executeQuery()

            var nearDate: String? = null
            var nearAmount = 0.0

            if (rsNearest.next()) {
                nearAmount = rsNearest.getDouble("grand_total")
                val dateRaw = rsNearest.getDate("due_date")
                // Format tanggal jadi dd MMM yyyy
                nearDate = if (dateRaw != null) {
                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(dateRaw)
                } else "-"
            }

            summary = BillSummary(totalAll, nearDate, nearAmount, count)
            conn.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return summary
    }
}