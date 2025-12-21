package com.example.ta_sales_outlet.data.local

import androidx.compose.runtime.mutableStateMapOf
import com.example.ta_sales_outlet.data.model.Product
import com.example.ta_sales_outlet.data.model.ProductVariant

object CartManager {
    val items = mutableStateMapOf<Int, CartItem>()

    data class CartItem(
        val product: Product,
        val variant: ProductVariant,
        val qty: Int // GANTI JADI VAL (Agar kita terpaksa pakai .copy)
    )

    fun addVariant(product: Product, variant: ProductVariant) {
        val variantId = variant.id

        if (items.containsKey(variantId)) {
            val currentItem = items[variantId]!!
            // SOLUSI BUG FREEZE:
            // Ganti item lama dengan item baru (copy) + qty baru
            // Assignment (=) ini yang memicu UI update
            items[variantId] = currentItem.copy(qty = currentItem.qty + 1)
        } else {
            val initialQty = if ((variant.minOrder ?: 0) > 0) variant.minOrder!! else 1
            items[variantId] = CartItem(product, variant, initialQty)
        }
    }

    fun removeVariant(variantId: Int) {
        if (items.containsKey(variantId)) {
            val currentItem = items[variantId]!!

            if (currentItem.qty > 1) {
                // SOLUSI BUG FREEZE:
                items[variantId] = currentItem.copy(qty = currentItem.qty - 1)
            } else {
                items.remove(variantId)
            }
        }
    }

    // Helper tetap sama, tapi pastikan UI memanggil ini
    fun getQty(variantId: Int): Int {
        return items[variantId]?.qty ?: 0
    }

    fun getTotalPrice(): Double {
        return items.values.sumOf { it.product.price * it.qty }
    }

    fun getTotalItems(): Int {
        return items.values.sumOf { it.qty }
    }

    fun clearCart() {
        items.clear()
    }

    fun updateVariantQty(product: Product, variant: ProductVariant, newQty: Int) {
        val variantId = variant.id
        val maxStock = variant.stock

        // 1. Validasi: Tidak boleh minus
        if (newQty < 0) return

        // 2. Validasi: Hapus jika 0
        if (newQty == 0) {
            items.remove(variantId)
            return
        }

        // 3. Validasi: Tidak boleh melebihi stok
        // Jika user ketik 999 padahal stok 10, kita paksa jadi 10
        val finalQty = if (newQty > maxStock) maxStock else newQty

        // 4. Update Map (Gunakan .copy agar UI refresh)
        if (items.containsKey(variantId)) {
            val currentItem = items[variantId]!!
            items[variantId] = currentItem.copy(qty = finalQty)
        } else {
            // Jika sebelumnya 0/belum ada, masukkan item baru
            items[variantId] = CartItem(product, variant, finalQty)
        }
    }
}