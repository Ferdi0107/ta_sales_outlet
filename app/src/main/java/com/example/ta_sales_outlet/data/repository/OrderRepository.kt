package com.example.ta_sales_outlet.data.repository

import android.util.Log
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.local.CartManager
import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.*

object OrderRepository {

    fun createOrder(
        salesId: Int,
        outletId: Int,
        paymentMethod: String,
        notes: String,
        items: List<CartManager.CartItem>,
        totalPrice: Double,
        creditTermInDays: Int = 0 // Parameter Baru (Default 0 untuk Cash)
    ): Boolean {
        Log.d("DEBUG_ORDER", "Mulai Transaksi. Method: $paymentMethod, Term: $creditTermInDays hari")

        val conn = MySQLHelper.connect() ?: return false

        try {
            conn.autoCommit = false

            // --- 1. LOGIC STATUS & TANGGAL JATUH TEMPO ---

            // A. Status Order (Sesuai Request: Selalu PACKING)
            val orderStatus = "PACKING"

            // B. Status Pembayaran & Due Date
            val paymentStatus: String
            val calendar = Calendar.getInstance()
            val orderDate = java.sql.Date(calendar.timeInMillis) // Hari ini

            when (paymentMethod) {
                "CASH" -> {
                    paymentStatus = "AWAITING_PAYMENT"
                    // Cash biasanya Due Date = Order Date (Hari ini)
                }
                "CREDIT" -> {
                    paymentStatus = "NOT_DUE"
                    // Tambahkan hari sesuai inputan user (Default 90 nanti dikirim dari UI)
                    calendar.add(Calendar.DAY_OF_YEAR, creditTermInDays)
                }
                "KONSINYASI" -> {
                    paymentStatus = "NOT_DUE"
                    // Konsinyasi biasanya due date panjang atau ditentukan nanti
                    // Di sini kita set default hari ini dulu atau tambah 30 hari (opsional)
                    calendar.add(Calendar.DAY_OF_YEAR, 30)
                }
                else -> {
                    paymentStatus = "AWAITING_PAYMENT"
                }
            }

            val dueDate = java.sql.Date(calendar.timeInMillis) // Hasil perhitungan tanggal
            Log.d("DEBUG_ORDER", "Due Date: $dueDate, Term: $creditTermInDays hari")


            // --- 2. INSERT HEADER ---

            val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            val orderCode = "ORD-$timeStamp"

            val sqlOrder = """
                INSERT INTO orders (
                    kode, sales_idusers, channel, status, payment_status, 
                    grand_total, payment_method, order_date, due_date, 
                    notes, outlets_idoutlets, created_at, updated_at
                ) VALUES (?, ?, 'SALES_VISIT', ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """

            val stmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)
            stmtOrder.setString(1, orderCode)
            stmtOrder.setInt(2, salesId)
            stmtOrder.setString(3, orderStatus)   // PACKING
            stmtOrder.setString(4, paymentStatus) // AWAITING_PAYMENT / NOT_DUE
            stmtOrder.setDouble(5, totalPrice)
            stmtOrder.setString(6, paymentMethod)
            stmtOrder.setDate(7, orderDate)
            stmtOrder.setDate(8, dueDate)         // Tanggal Jatuh Tempo Dinamis
            stmtOrder.setString(9, notes)
            stmtOrder.setInt(10, outletId)

            val affectedRows = stmtOrder.executeUpdate()
            if (affectedRows == 0) { conn.rollback(); return false }

            val generatedKeys = stmtOrder.generatedKeys
            var orderId = 0
            if (generatedKeys.next()) { orderId = generatedKeys.getInt(1) }
            else { conn.rollback(); return false }

            // --- 3. INSERT DETAILS & UPDATE STOCK (SAMA SEPERTI SEBELUMNYA) ---
            val sqlDetail = "INSERT INTO order_details (orders_idorders, quantity, sub_total, product_variants_idproduct_variants, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())"
            val sqlUpdateStock = "UPDATE product_variants SET stock = stock - ? WHERE idproduct_variants = ?"

            val stmtDetail = conn.prepareStatement(sqlDetail)
            val stmtStock = conn.prepareStatement(sqlUpdateStock)

            for (item in items) {
                val subTotal = item.product.price * item.qty
                stmtDetail.setInt(1, orderId)
                stmtDetail.setString(2, item.qty.toString())
                stmtDetail.setString(3, subTotal.toString())
                stmtDetail.setInt(4, item.variant.id)
                stmtDetail.executeUpdate()

                stmtStock.setInt(1, item.qty)
                stmtStock.setInt(2, item.variant.id)
                stmtStock.executeUpdate()
            }

            conn.commit()
            conn.close()
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            try { conn.rollback(); conn.close() } catch (ex: Exception) {}
            return false
        }
    }
}