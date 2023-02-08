package com.esop.schema

@Suppress("unused")
class OrderFilledLog(
    var quantity: Long = 0,
    var amount: Long = 0,
    var esopType: String? = null,
    var sellerUsername: String? = null,
    var buyerUsername: String? = null
)