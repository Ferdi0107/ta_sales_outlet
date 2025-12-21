package com.example.ta_sales_outlet.data.repository

import android.content.Context
import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.model.ProductVariant

object ProductRepository {

    fun getAllProducts(context: Context): List<Product> {
        val resultList = ArrayList<Product>()

        // Map Sementara untuk Grouping:
        // Key = ID Produk, Value = Pair(Objek Produk, List Varian Sementara)
        val tempMap = LinkedHashMap<Int, Pair<Product, MutableList<ProductVariant>>>()

        val conn = MySQLHelper.connect()
        if (conn == null) return emptyList()

        try {
            // QUERY: Ambil Produk + Varian-nya sekaligus
            // Kita pakai LEFT JOIN agar produk yang BELUM punya varian tetap muncul (opsional)
            val sql = """
                SELECT 
                    p.idproducts, p.kode, p.nama, p.deskripsi, p.price, p.url_photo, p.categories_idcategories,
                    v.idproduct_variants, v.size, v.color, v.min_order, v.stock, v.products_idproducts
                FROM products p
                LEFT JOIN product_variants v ON p.idproducts = v.products_idproducts
                WHERE p.price IS NOT NULL 
                ORDER BY p.nama ASC, v.idproduct_variants ASC
            """

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)

            while (rs.next()) {
                val pId = rs.getInt("idproducts")

                // 1. Cek apakah produk ini sudah ada di Map sementara?
                if (!tempMap.containsKey(pId)) {
                    // Kalau belum ada, kita buat objek Produk dasarnya
                    val product = Product(
                        id = pId,
                        code = rs.getString("kode"),
                        name = rs.getString("nama"),
                        description = rs.getString("deskripsi"),
                        hpp = 0.0, // Di query SELECT saya skip HPP karena biasanya rahasia sales, tapi bisa ditambah
                        price = rs.getDouble("price"),
                        photoUrl = rs.getString("url_photo"),
                        categoryId = rs.getInt("categories_idcategories"),
                        variants = emptyList() // Nanti kita timpa ini saat return
                    )
                    // Simpan ke Map dengan List Varian yang masih kosong
                    tempMap[pId] = Pair(product, mutableListOf())
                }

                // 2. Ambil Data Varian (Cek dulu ID-nya valid/tidak null karena LEFT JOIN)
                val vId = rs.getInt("idproduct_variants")
                if (vId != 0) {
                    val variant = ProductVariant(
                        id = vId,
                        size = rs.getString("size"),
                        color = rs.getString("color"),
                        minOrder = rs.getInt("min_order"),
                        productId = pId,
                        stock = rs.getInt("stock")
                    )
                    tempMap[pId]?.second?.add(variant)
                }
            }

            // 3. Gabungkan Produk + List Varian yang sudah terkumpul
            tempMap.values.forEach { (product, variantList) ->
                // Kita gunakan .copy() untuk mengisi field 'variants' yang tadinya kosong
                val finalProduct = product.copy(variants = variantList)
                resultList.add(finalProduct)
            }

            conn.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultList
    }
}