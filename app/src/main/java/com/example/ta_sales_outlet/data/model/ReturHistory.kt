package com.example.ta_sales_outlet.data.model

data class ReturHistory(
    val id: Int,
    val kodeRetur: String,
    val orderKode: String,
    val date: String,
    val status: String,
    val reason: String,
    val orderId: Int
)