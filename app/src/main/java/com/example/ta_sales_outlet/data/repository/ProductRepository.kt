package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.Product

object ProductRepository {

    fun getAllProducts(): List<Product> {
        val productList = mutableListOf<Product>()
        val conn = MySQLHelper.connect() ?: return productList

        try {
            // Ambil semua produk
            val sql = "SELECT * FROM products"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)

            while (rs.next()) {
                productList.add(
                    Product(
                        id = rs.getInt("idproducts"),
                        code = rs.getString("kode"),
                        name = rs.getString("nama"),
                        description = rs.getString("deskripsi"),
                        price = rs.getDouble("price"),
                        photoUrl = rs.getString("url_photo"),
                        categoryId = rs.getInt("categories_idcategories")
                    )
                )
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return productList
    }

    // Nanti bisa tambah fungsi pencarian:
    // fun searchProducts(query: String): List<Product> { ... }
}