package com.example.ta_sales_outlet.data.model

import com.google.gson.annotations.SerializedName

data class Route(
    @SerializedName("idroutes") val id: Int,
    @SerializedName("route_date") val date: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("total_stops") val totalStops: Int?,
    @SerializedName("distance_km") val distanceKm: Double?,
    @SerializedName("stops") val stops: List<RouteStop>? = null,

    @SerializedName("start_lat") val startLat: Double?,
    @SerializedName("start_lng") val startLng: Double?,
    @SerializedName("end_lat") val endLat: Double?,
    @SerializedName("end_lng") val endLng: Double?,
    @SerializedName("start_point") val startPoint: String?,
    @SerializedName("end_point") val endPoint: String?
)

data class RouteStop(
    @SerializedName("idroute_stops") val id: Int,
    @SerializedName("seq") val sequenceNumber: Int?,
    @SerializedName("status") val status: String?, // PLANNED, VISITED
    @SerializedName("outlets_idoutlets") val outletId: Int?,
    @SerializedName("outlet_detail") val outlet: Outlet?,

    @SerializedName("planned_arrival") val plannedArrival: String?,
    @SerializedName("planned_departure") val plannedDeparture: String?
)