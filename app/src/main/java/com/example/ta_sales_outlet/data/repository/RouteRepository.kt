package com.example.ta_sales_outlet.data.repository

import com.example.ta_sales_outlet.data.MySQLHelper
import com.example.ta_sales_outlet.data.model.Outlet
import com.example.ta_sales_outlet.data.model.Route
import com.example.ta_sales_outlet.data.model.RouteStop

object RouteRepository {

    // Mengambil SEMUA rute, diurutkan dari yang tanggalnya paling dekat dengan Hari Ini
    fun getAllRoutesSorted(salesId: Int): List<Route> {
        val routeList = mutableListOf<Route>()
        val conn = MySQLHelper.connect() ?: return routeList

        try {
            // 1. Query Header: Ambil semua rute user ini
            // ORDER BY ABS(DATEDIFF(...)) ASC -> Mengurutkan berdasarkan jarak hari terdekat ke "Hari Ini"
            val sqlRoute = """
                SELECT * FROM routes 
                WHERE sales_idusers = $salesId 
                ORDER BY ABS(DATEDIFF(route_date, CURDATE())) ASC
            """
            val stmt = conn.createStatement()
            val rsRoute = stmt.executeQuery(sqlRoute)

            while (rsRoute.next()) {
                val routeId = rsRoute.getInt("idroutes")

                // 2. Query Detail (Stops) untuk setiap rute
                val stopList = mutableListOf<RouteStop>()
                val sqlStops = """
                    SELECT rs.*, o.* FROM route_stops rs
                    JOIN outlets o ON rs.outlets_idoutlets = o.idoutlets
                    WHERE rs.routes_idroutes = $routeId
                    ORDER BY rs.seq ASC
                """
                // Gunakan statement baru agar tidak menimpa result set header
                val stmtStop = conn.createStatement()
                val rsStops = stmtStop.executeQuery(sqlStops)

                while (rsStops.next()) {
                    val outlet = Outlet(
                        id = rsStops.getInt("idoutlets"),
                        name = rsStops.getString("nama"),
                        address = rsStops.getString("alamat"),
                        latitude = rsStops.getDouble("lat"),
                        longitude = rsStops.getDouble("lng"),
                        phone = rsStops.getString("no_telp"),
                        contactPerson = rsStops.getString("contact_person"),
                        paymentType = rsStops.getString("tipe_pembayaran")
                    )

                    stopList.add(
                        RouteStop(
                            id = rsStops.getInt("idroute_stops"),
                            sequenceNumber = rsStops.getInt("seq"),
                            status = rsStops.getString("status"),
                            outletId = outlet.id,
                            outlet = outlet
                        )
                    )
                }
                rsStops.close()
                stmtStop.close()

                // Tambahkan ke list rute
                routeList.add(
                    Route(
                        id = routeId,
                        date = rsRoute.getString("route_date"), // Format YYYY-MM-DD
                        status = rsRoute.getString("status"),
                        totalStops = rsRoute.getInt("total_stops"),
                        distanceKm = rsRoute.getDouble("distance_km"),
                        stops = stopList
                    )
                )
            }
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return routeList
    }
}