package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper

object UserRepository {
    // Fungsi untuk mengambil ID Sales berdasarkan Email (yang disimpan di SharedPref)
    fun getSalesInfoByEmail(email: String): Pair<Int, String>? {
        var result: Pair<Int, String>? = null
        val conn = MySQLHelper.connect() ?: return null

        try {
            val sql = "SELECT idusers, name FROM users WHERE email = ? LIMIT 1"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, email)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                val id = rs.getInt("idusers")
                val name = rs.getString("name")
                result = Pair(id, name)
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}