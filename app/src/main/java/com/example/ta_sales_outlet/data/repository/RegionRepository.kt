package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import java.util.ArrayList

data class City(
    val id: Int,
    val name: String
    // val provinceCode: String // Tidak perlu lagi jika logicnya by Name
)

object RegionRepository {

    // UBAH PARAMETER JADI String (Nama)
    fun getCitiesByProvinceName(provinceName: String): List<City> {
        val list = ArrayList<City>()
        val conn = MySQLHelper.connect()

        if (conn == null) {
            android.util.Log.e("DEBUG_REGION", "Koneksi Database Gagal!")
            return list
        }

        try {
            // 1. LOG INPUT
            android.util.Log.d("DEBUG_REGION", "Mencari kota di provinsi: '$provinceName'")

            // Query
            val sql = "SELECT idcities, nama FROM cities WHERE provinsi LIKE ? ORDER BY nama ASC"

            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, provinceName)

            // 2. LOG SQL FINAL
            // MySQL Driver otomatis mengubah .toString() menjadi query lengkap dengan valuenya
            android.util.Log.d("DEBUG_REGION", "Eksekusi Query: $stmt")

            val rs = stmt.executeQuery()

            var rowCount = 0
            while (rs.next()) {
                rowCount++

                val cityName = rs.getString("nama")
                val cityId = rs.getInt("idcities")

                list.add(City(id = cityId, name = cityName))
            }

            // 3. LOG HASIL
            android.util.Log.d("DEBUG_REGION", "Selesai. Ditemukan $rowCount kota.")

            conn.close()

        } catch (e: Exception) {
            android.util.Log.e("DEBUG_REGION", "ERROR SQL: ${e.message}")
            e.printStackTrace()
        }
        return list
    }
}