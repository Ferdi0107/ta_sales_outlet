package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.Statement

object AuthRepository {

    fun registerOutlet(
        namaToko: String,      // Masuk ke outlets.nama
        contactPerson: String, // Masuk ke users.nama & outlets.contact_person
        email: String,
        password: String,
        noTelp: String,
        alamat: String,
        lat: Double,
        lng: Double,
        photoPath: String?,
        cityId: Int,
        paymentType: String,
        timePref: String,
        // PARAMETER BARU
        jamBuka: String,       // Format "08:00"
        jamTutup: String,      // Format "17:00"
        hariBuka: String       // Format "Senin,Selasa,Rabu,..."
    ): Pair<Boolean, String> {
        val conn = MySQLHelper.connect() ?: return Pair(false, "Koneksi Database Gagal")

        try {
            conn.autoCommit = false

            // --- 1. GENERATE KODE OTOMATIS ---
            val generatedKode = generateOutletCode(conn, cityId)
            if (generatedKode.isEmpty()) {
                conn.rollback()
                return Pair(false, "Gagal generate kode outlet (Kota tidak valid)")
            }

            // --- 2. HASH PASSWORD ---
            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

            // --- 3. INSERT USER (Gunakan Contact Person sebagai Nama User) ---
            val sqlUser = """
                INSERT INTO users (
                    nama, email, password, no_telp, role, status, photo, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'OUTLET', 1, ?, NOW(), NOW())
            """
            val stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)
            stmtUser.setString(1, contactPerson) // <--- PENTING: Nama User = Contact Person
            stmtUser.setString(2, email)
            stmtUser.setString(3, hashedPassword)
            stmtUser.setString(4, noTelp)
            stmtUser.setString(5, photoPath)

            val affectedUser = stmtUser.executeUpdate()
            if (affectedUser == 0) { conn.rollback(); return Pair(false, "Gagal insert user") }

            val keys = stmtUser.generatedKeys
            var userId = 0
            if (keys.next()) userId = keys.getInt(1)
            else { conn.rollback(); return Pair(false, "Gagal mendapatkan ID User") }

            // --- 4. INSERT OUTLET (LENGKAP) ---
            val sqlOutlet = """
                INSERT INTO outlets (
                    kode, nama, contact_person, email, no_telp, alamat, lat, lng, 
                    outlet_idusers, cities_idcities, 
                    tipe_pembayaran, pref_waktu,
                    jam_buka, jam_tutup, hari_buka,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """
            val stmtOutlet = conn.prepareStatement(sqlOutlet)
            stmtOutlet.setString(1, generatedKode)  // Kode Otomatis
            stmtOutlet.setString(2, namaToko)       // Nama Toko
            stmtOutlet.setString(3, contactPerson)  // Contact Person
            stmtOutlet.setString(4, email)
            stmtOutlet.setString(5, noTelp)
            stmtOutlet.setString(6, alamat)
            stmtOutlet.setDouble(7, lat)
            stmtOutlet.setDouble(8, lng)
            stmtOutlet.setInt(9, userId)
            stmtOutlet.setInt(10, cityId)
            stmtOutlet.setString(11, paymentType)
            stmtOutlet.setString(12, timePref)
            stmtOutlet.setString(13, jamBuka)       // Baru
            stmtOutlet.setString(14, jamTutup)      // Baru
            stmtOutlet.setString(15, hariBuka)      // Baru

            stmtOutlet.executeUpdate()

            conn.commit()
            conn.close()
            return Pair(true, "Registrasi Berhasil!")

        } catch (e: Exception) {
            try { conn.rollback() } catch (ex: Exception) {}
            e.printStackTrace()
            if (e.message?.contains("Duplicate entry") == true) return Pair(false, "Email sudah terdaftar!")
            return Pair(false, "Error: ${e.message}")
        }
    }

    // --- LOGIKA GENERATE KODE (Contoh: SBY001, SBY002) ---
    private fun generateOutletCode(conn: java.sql.Connection, cityId: Int): String {
        try {
            // A. Ambil Kode Kota
            val sqlCity = "SELECT kode_city FROM cities WHERE idcities = ?"
            val stmtCity = conn.prepareStatement(sqlCity)
            stmtCity.setInt(1, cityId)
            val rsCity = stmtCity.executeQuery()

            var cityCode = ""
            if (rsCity.next()) {
                cityCode = rsCity.getString("kode_city") ?: ""
            }
            rsCity.close()
            stmtCity.close()

            if (cityCode.isEmpty()) return ""

            // B. Cari kode terakhir
            val sqlLast = """
                SELECT kode FROM outlets 
                WHERE cities_idcities = ? 
                ORDER BY idoutlets DESC LIMIT 1
            """
            val stmtLast = conn.prepareStatement(sqlLast)
            stmtLast.setInt(1, cityId)
            val rsLast = stmtLast.executeQuery()

            var newSequence = 1
            if (rsLast.next()) {
                val lastKode = rsLast.getString("kode") // Misal: "TLG-013"

                // Ambil angkanya saja: Buang Huruf Kota & Tanda Strip
                // Contoh: "TLG-013" -> dibuang TLG -> "-013" -> dibuang strip -> "013"
                var numberStr = lastKode.replace(cityCode, "").replace("-", "")

                // Konversi ke INT untuk ditambah 1 (Disini nol hilang, jadi 13)
                // 13 + 1 = 14
                newSequence = numberStr.toIntOrNull()?.plus(1) ?: 1
            }
            rsLast.close()
            stmtLast.close()

            // --- STEP D: FORMAT KEMBALI KE STRING DENGAN 3 DIGIT ---
            // Kita ubah angka 14 menjadi "014" menggunakan String.format
            // %03d artinya: Format angka desimal, minimal 3 digit, isi depannya dengan nol
            val sequenceString = String.format("%03d", newSequence)

            // Gabungkan: TLG + - + 014
            return "$cityCode-$sequenceString"

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}