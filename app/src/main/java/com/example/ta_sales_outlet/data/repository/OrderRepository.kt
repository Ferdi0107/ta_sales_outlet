package com.example.ta_sales_outlet.data.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.local.CartManager
import com.example.ta_sales_outlet.data.model.ReturItem
import com.example.ta_sales_outlet.data.model.ReturReason
import java.sql.Statement
import java.sql.Types
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.model.OrderDetailItem
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

object OrderRepository {

    fun createOrder(
        userId: Int,
        channel: String, // "SALES_VISIT" atau "SELF_ORDER"
        outletId: Int,
        paymentMethod: String,
        notes: String,
        items: List<CartManager.CartItem>,
        totalPrice: Double,
        creditTermInDays: Int = 0
    ): Boolean {
        // ... (Logika status dan tanggal tetap sama) ...
        // ... Copy paste logika Due Date dll dari codingan sebelumnya ...

        // SAYA TULIS ULANG BAGIAN PENTINGNYA SAJA:
        val conn = MySQLHelper.connect() ?: return false

        try {
            conn.autoCommit = false

            // --- 1. SETUP VARIABEL STATUS DLL (SAMA SEPERTI SEBELUMNYA) ---
            val orderStatus = "PACKING"
            val paymentStatus: String
            val calendar = Calendar.getInstance()
            val orderDate = java.sql.Date(calendar.timeInMillis)

            when (paymentMethod) {
                "CASH" -> paymentStatus = "AWAITING_PAYMENT"
                "CREDIT" -> {
                    paymentStatus = "NOT_DUE"
                    calendar.add(Calendar.DAY_OF_YEAR, creditTermInDays)
                }
                "KONSINYASI" -> {
                    paymentStatus = "NOT_DUE"
                    calendar.add(Calendar.DAY_OF_YEAR, 30)
                }
                else -> paymentStatus = "AWAITING_PAYMENT"
            }
            val dueDate = java.sql.Date(calendar.timeInMillis)

            // --- 2. INSERT HEADER (PERBAIKAN DI SINI) ---
            val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            val orderCode = "ORD-$timeStamp"

            val sqlOrder = """
                INSERT INTO orders (
                    kode, sales_idusers, channel, status, payment_status, 
                    grand_total, payment_method, order_date, due_date, 
                    notes, outlets_idoutlets, outlet_idusers, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """

            val stmtOrder = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)
            stmtOrder.setString(1, orderCode)

            if (channel == "SELF_ORDER") {
                stmtOrder.setNull(2, Types.INTEGER)
                stmtOrder.setInt(12, userId)
            } else {
                stmtOrder.setInt(2, userId)
                stmtOrder.setNull(12, Types.INTEGER)
            }

            stmtOrder.setString(3, channel)
            stmtOrder.setString(4, orderStatus)
            stmtOrder.setString(5, paymentStatus)
            stmtOrder.setDouble(6, totalPrice)
            stmtOrder.setString(7, paymentMethod)
            stmtOrder.setDate(8, orderDate)
            stmtOrder.setDate(9, dueDate)
            stmtOrder.setString(10, notes)
            stmtOrder.setInt(11, outletId)

            // ... (Eksekusi query dan Insert Detail tetap sama) ...

            val affectedRows = stmtOrder.executeUpdate()
            if (affectedRows == 0) { conn.rollback(); return false }

            val generatedKeys = stmtOrder.generatedKeys
            var orderId = 0
            if (generatedKeys.next()) { orderId = generatedKeys.getInt(1) }
            else { conn.rollback(); return false }

            // Insert Details
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

    fun getReturItems(orderId: Int): List<ReturItem> {
        val list = ArrayList<ReturItem>()
        val conn = MySQLHelper.connect() ?: return list

        try {
            // PERBAIKAN 1: Tambahkan 'p.url_photo' di bagian SELECT
            val sql = """
            SELECT 
                od.idorder_details, 
                od.quantity,
                p.nama AS product_name,
                p.url_photo,  -- <--- TAMBAHKAN INI
                v.size, 
                v.color
            FROM order_details od
            JOIN product_variants v ON od.product_variants_idproduct_variants = v.idproduct_variants
            JOIN products p ON v.products_idproducts = p.idproducts
            WHERE od.orders_idorders = ?
        """

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, orderId)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                list.add(ReturItem(
                    orderDetailId = rs.getInt("idorder_details"),
                    productName = rs.getString("product_name"),
                    variantName = "${rs.getString("size")} - ${rs.getString("color")}",
                    maxQty = rs.getInt("quantity"),

                    // Default value
                    inputQty = 0,
                    isSelected = false,

                    // PERBAIKAN 2: Masukkan data foto ke variabel photoUrl
                    // Pastikan nama kolom 'url_photo' sesuai dengan tabel database Anda
                    photoUrl = rs.getString("url_photo")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // 2. SIMPAN DATA RETUR KE DATABASE
    @SuppressLint("SimpleDateFormat")
    fun createRetur(
        orderId: Int,
        reason: ReturReason,
        description: String,
        photoPath: String?,
        items: List<ReturItem>
    ): Boolean {
        val conn = MySQLHelper.connect() ?: return false

        // ... (Logika filter items sama seperti sebelumnya) ...
        val selectedItems = items.filter { it.isSelected && it.inputQty > 0 }
        if (selectedItems.isEmpty()) return false

        try {
            conn.autoCommit = false

            // 1. INSERT TABLE RETURS (Sama seperti sebelumnya)
            val timeStamp = java.text.SimpleDateFormat("yyyyMMddHHmmss").format(java.util.Date())
            val kodeRetur = "RET-$timeStamp"
            val sqlHead = "INSERT INTO returs (kode, status, reason, `desc`, photo, requested_at, orders_idorders, created_at, updated_at) VALUES (?, 'REQUESTED', ?, ?, ?, NOW(), ?, NOW(), NOW())"
            val stmtHead = conn.prepareStatement(sqlHead, java.sql.Statement.RETURN_GENERATED_KEYS)
            stmtHead.setString(1, kodeRetur)
            stmtHead.setString(2, reason.dbValue)
            stmtHead.setString(3, description)
            stmtHead.setString(4, photoPath)
            stmtHead.setInt(5, orderId)

            if (stmtHead.executeUpdate() == 0) { conn.rollback(); return false }

            val keys = stmtHead.generatedKeys
            var returId = 0
            if (keys.next()) returId = keys.getInt(1) else { conn.rollback(); return false }

            // 2. INSERT RETUR ITEMS (Sama seperti sebelumnya)
            val sqlItem = "INSERT INTO retur_items (returs_idreturs, order_details_idorder_details, qty, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())"
            val stmtItem = conn.prepareStatement(sqlItem)
            for (item in selectedItems) {
                stmtItem.setInt(1, returId)
                stmtItem.setInt(2, item.orderDetailId)
                stmtItem.setInt(3, item.inputQty)
                stmtItem.executeUpdate()
            }

            // 3. [BARU] UPDATE STATUS ORDER JADI 'RETURN_REQUESTED'
            val sqlUpdateOrder = "UPDATE orders SET status = 'RETURN_REQUESTED', updated_at = NOW() WHERE idorders = ?"
            val stmtUpdate = conn.prepareStatement(sqlUpdateOrder)
            stmtUpdate.setInt(1, orderId)
            stmtUpdate.executeUpdate()

            conn.commit()
            conn.close()
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            try { conn.rollback() } catch (ex: Exception) {}
            return false
        }
    }

    // --- [BARU] FUNGSI KONFIRMASI PESANAN ---
    fun verifyOrder(orderId: Int): Boolean {
        val conn = MySQLHelper.connect() ?: return false
        try {
            val sql = "UPDATE orders SET status = 'VERIFIED', updated_at = NOW() WHERE idorders = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, orderId)
            val affected = stmt.executeUpdate()
            conn.close()
            return affected > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getOrderDetailFull(orderId: Int): Pair<OrderHistory, List<OrderDetailItem>>? {
        val conn = MySQLHelper.connect() ?: return null

        var header: OrderHistory? = null
        val items = ArrayList<OrderDetailItem>()

        try {
            // A. QUERY HEADER
            // Ambil juga 'alamat' dan 'notes'
            val sqlHeader = """
                SELECT 
                    o.idorders, o.kode, o.status, o.order_date, o.grand_total, 
                    o.payment_method, o.payment_status, o.notes,
                    outl.nama AS outlet_name, outl.alamat AS outlet_address
                FROM orders o
                JOIN outlets outl ON o.outlets_idoutlets = outl.idoutlets
                WHERE o.idorders = ?
            """
            val stmtHead = conn.prepareStatement(sqlHeader)
            stmtHead.setInt(1, orderId)
            val rsHead = stmtHead.executeQuery()

            if (rsHead.next()) {
                val dateRaw = rsHead.getDate("order_date")
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(dateRaw)

                // Mapping ke Model OrderHistory Anda
                header = OrderHistory(
                    id = rsHead.getInt("idorders"),
                    kode = rsHead.getString("kode"),
                    outletName = rsHead.getString("outlet_name"),
                    total = rsHead.getDouble("grand_total"),
                    status = rsHead.getString("status"),
                    paymentStatus = rsHead.getString("payment_status"),
                    paymentMethod = rsHead.getString("payment_method"),
                    date = dateStr,
                    // Field Tambahan
                    address = rsHead.getString("outlet_address"),
                    notes = rsHead.getString("notes")
                )
            }
            if (header == null) { conn.close(); return null }

            // B. QUERY ITEMS (Mapping ke Model OrderDetailItem Anda)
            val sqlItems = """
                SELECT 
                    od.quantity, od.sub_total,
                    p.nama AS product_name, p.price, p.url_photo,
                    v.size, v.color
                FROM order_details od
                JOIN product_variants v ON od.product_variants_idproduct_variants = v.idproduct_variants
                JOIN products p ON v.products_idproducts = p.idproducts
                WHERE od.orders_idorders = ?
            """
            val stmtItem = conn.prepareStatement(sqlItems)
            stmtItem.setInt(1, orderId)
            val rsItem = stmtItem.executeQuery()

            while (rsItem.next()) {
                items.add(OrderDetailItem(
                    productName = rsItem.getString("product_name"),
                    variantName = "${rsItem.getString("size")} - ${rsItem.getString("color")}",
                    qty = rsItem.getInt("quantity"),
                    price = rsItem.getDouble("price"),
                    subTotal = rsItem.getDouble("sub_total"),
                    photoUrl = rsItem.getString("url_photo")
                ))
            }

            conn.close()
            return Pair(header, items)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}