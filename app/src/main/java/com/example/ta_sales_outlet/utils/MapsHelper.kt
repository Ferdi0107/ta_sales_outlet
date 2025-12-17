package com.example.ta_sales_outlet.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object MapsHelper {

    // Fungsi untuk membuka Google Maps mode Turn-by-Turn Navigation
    fun openNavigation(context: Context, lat: Double, lng: Double) {
        try {
            // Format URI untuk Google Maps Navigation
            // q = query (koordinat tujuan)
            // mode = d (driving)
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")

            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Paksa buka app Google Maps

            // Cek apakah user punya aplikasi Google Maps
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback jika tidak punya aplikasi Maps (buka di Browser)
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka Peta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}