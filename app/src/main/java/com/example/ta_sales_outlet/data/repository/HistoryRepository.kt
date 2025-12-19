package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.OrderHistory
import com.example.ta_sales_outlet.data.model.OrderDetailItem
import java.util.ArrayList

object HistoryRepository {

    fun getSalesHistory(salesId: Int): List<OrderHistory> {
        val list = ArrayList<OrderHistory>()
        val conn = MySQLHelper.connect() ?: return list

        try {
            val sql = """
                SELECT 
                    o.idorders, o.kode, o.grand_total, o.status, 
                    o.payment_status, o.payment_method, o.order_date,
                    outl.nama as outlet_name
                FROM orders o
                JOIN outlets outl ON o.outlets_idoutlets = outl.idoutlets
                WHERE o.sales_idusers = ?
                ORDER BY o.created_at DESC
            """

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, salesId)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                list.add(OrderHistory(
                    id = rs.getInt("idorders"),
                    kode = rs.getString("kode"),
                    outletName = rs.getString("outlet_name"),
                    total = rs.getDouble("grand_total"),
                    status = rs.getString("status"),
                    paymentStatus = rs.getString("payment_status"),
                    paymentMethod = rs.getString("payment_method"),
                    date = rs.getString("order_date")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // ... fungsi getSalesHistory yang lama ...

    // 1. AMBIL HEADER (Info Toko, Status, Tanggal) berdasarkan Order ID
    fun getOrderHeader(orderId: Int): OrderHistory? {
        var order: OrderHistory? = null
        val conn = MySQLHelper.connect() ?: return null

        try {
            val sql = """
                SELECT 
                    o.idorders, o.kode, o.grand_total, o.status, 
                    o.payment_status, o.payment_method, o.order_date,
                    outl.nama as outlet_name
                FROM orders o
                JOIN outlets outl ON o.outlets_idoutlets = outl.idoutlets
                WHERE o.idorders = ?
                LIMIT 1
            """

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, orderId)
            val rs = stmt.executeQuery()

            if (rs.next()) {
                order = OrderHistory(
                    id = rs.getInt("idorders"),
                    kode = rs.getString("kode"),
                    outletName = rs.getString("outlet_name"),
                    total = rs.getDouble("grand_total"),
                    status = rs.getString("status"),
                    paymentStatus = rs.getString("payment_status"),
                    paymentMethod = rs.getString("payment_method"),
                    date = rs.getString("order_date")
                )
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return order
    }

    // 2. AMBIL ITEM BARANG (JOIN order_details -> variants -> products)
    fun getOrderItems(orderId: Int): List<OrderDetailItem> {
        val list = ArrayList<OrderDetailItem>()
        val conn = MySQLHelper.connect() ?: return list

        try {
            val sql = """
                SELECT 
                    od.quantity, od.sub_total,
                    p.nama as product_name, p.price, p.url_photo,
                    v.size, v.color
                FROM order_details od
                JOIN product_variants v ON od.product_variants_idproduct_variants = v.idproduct_variants
                JOIN products p ON v.products_idproducts = p.idproducts
                WHERE od.orders_idorders = ?
            """

            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, orderId)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                // Formatting nama varian
                val size = rs.getString("size") ?: "-"
                val color = rs.getString("color") ?: "-"

                list.add(OrderDetailItem(
                    productName = rs.getString("product_name"),
                    variantName = "$size - $color",
                    qty = rs.getInt("quantity"), // Pastikan kolom DB quantity tipe INT/VARCHAR yg valid
                    price = rs.getDouble("price"),
                    subTotal = rs.getDouble("sub_total"),
                    photoUrl = rs.getString("url_photo")
                ))
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}