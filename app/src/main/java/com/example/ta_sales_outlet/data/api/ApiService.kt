package com.example.ta_sales_outlet.data.api

import com.example.ta_sales_outlet.data.model.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // --- AUTH ---
    @POST("login")
    fun login(@Body request: Map<String, String>): Call<BaseResponse<User>>

    // --- PRODUCTS (Katalog) ---
    @GET("products")
    fun getProducts(): Call<BaseResponse<List<Product>>>

    // --- SALESPERSON FEATURES ---
    // Mengambil rute hari ini berdasarkan ID Sales
    @GET("sales/routes/today")
    fun getTodayRoute(@Query("sales_id") salesId: Int): Call<BaseResponse<Route>>

    // Mengambil detail kunjungan/outlet dalam rute
    @GET("sales/routes/stops/{id}")
    fun getRouteStopDetail(@Path("id") stopId: Int): Call<BaseResponse<RouteStop>>

    // Check-in / Check-out atau Update Status Kunjungan
    @POST("visits")
    fun updateVisitStatus(@Body request: Map<String, Any>): Call<BaseResponse<Any>>

    // Create Order (Sales Order)
    @POST("orders")
    fun createOrder(@Body orderData: Order): Call<BaseResponse<Order>>

    // --- OUTLET FEATURES ---
    // Outlet membuat pesanan sendiri (Self Order)
    @POST("outlets/self-order")
    fun createSelfOrder(@Body orderData: Order): Call<BaseResponse<Order>>

    // Outlet melihat riwayat pesanan
    @GET("outlets/orders/history")
    fun getOutletHistory(@Query("outlet_id") outletId: Int): Call<BaseResponse<List<Order>>>
}