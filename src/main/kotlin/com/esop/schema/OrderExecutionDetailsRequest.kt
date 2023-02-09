package com.esop.schema

data class OrderExecutionDetailsRequest(
    val orderExecutionQuantity: Long,
    val orderExecutionPrice: Long,
    val buyerUserName: String,
    val sellerUserName: String,
    val esopType: String,
    val totalOrderExecutionValue: Long = orderExecutionQuantity * orderExecutionPrice
)