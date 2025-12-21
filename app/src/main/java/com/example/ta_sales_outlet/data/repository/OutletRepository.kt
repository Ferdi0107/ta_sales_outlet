package com.example.ta_sales_outlet.data.repository

import android.util.Log
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.Outlet // Pastikan model Outlet ada

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
}