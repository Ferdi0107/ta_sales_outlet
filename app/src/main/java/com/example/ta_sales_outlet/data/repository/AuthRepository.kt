package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import org.mindrot.jbcrypt.BCrypt
import java.sql.Statement

object AuthRepository {

    fun registerOutlet(
        nama: String,
        email: String,
        password: String,
        noTelp: String,
        alamat: String,
        lat: Double,
        lng: Double,
        photoPath: String?,
        cityId: Int,           // PARAMETER BARU
        paymentType: String,   // PARAMETER BARU (CREDIT/KONSINYASI)
        timePref: String       // PARAMETER BARU (PAGI/SIANG)
    ): Pair<Boolean, String> {
        val conn = MySQLHelper.connect() ?: return Pair(false, "Koneksi Database Gagal")

        try {
            conn.autoCommit = false

            // 1. HASH PASSWORD (Sesuai Laravel Hash::make)
            // Menggunakan BCrypt
            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

            // 2. INSERT USER
            val sqlUser = """
                INSERT INTO users (
                    nama, email, password, no_telp, role, status, photo, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'OUTLET', 1, ?, NOW(), NOW())
            """

            val stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)
            stmtUser.setString(1, nama)
            stmtUser.setString(2, email)
            stmtUser.setString(3, hashedPassword) // SIMPAN HASH, BUKAN PLAIN TEXT
            stmtUser.setString(4, noTelp)
            stmtUser.setString(5, photoPath)

            val affectedUser = stmtUser.executeUpdate()
            if (affectedUser == 0) { conn.rollback(); return Pair(false, "Gagal user") }

            val keys = stmtUser.generatedKeys
            var userId = 0
            if (keys.next()) userId = keys.getInt(1)
            else { conn.rollback(); return Pair(false, "Gagal ID User") }

            // 3. INSERT OUTLET (KOLOM LENGKAP)
            val sqlOutlet = """
                INSERT INTO outlets (
                    nama, email, no_telp, alamat, lat, lng, 
                    outlet_idusers, cities_idcities, 
                    tipe_pembayaran, pref_waktu,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """
            val stmtOutlet = conn.prepareStatement(sqlOutlet)
            stmtOutlet.setString(1, nama)
            stmtOutlet.setString(2, email)
            stmtOutlet.setString(3, noTelp)
            stmtOutlet.setString(4, alamat)
            stmtOutlet.setDouble(5, lat)
            stmtOutlet.setDouble(6, lng)
            stmtOutlet.setInt(7, userId)
            stmtOutlet.setInt(8, cityId)       // Masukkan ID Kota Pilihan
            stmtOutlet.setString(9, paymentType) // CREDIT / KONSINYASI
            stmtOutlet.setString(10, timePref)   // PAGI / SIANG

            stmtOutlet.executeUpdate()

            conn.commit()
            conn.close()
            return Pair(true, "Registrasi Berhasil! Silakan Login.")

        } catch (e: Exception) {
            try { conn.rollback() } catch (ex: Exception) {}
            e.printStackTrace()
            if (e.message?.contains("Duplicate entry") == true) return Pair(false, "Email sudah terdaftar!")
            return Pair(false, "Error: ${e.message}")
        }
    }
}