package com.esop.schema

data class InventoryTransferRequest(
    val sellerInventoryType: String,
    val transferQuantity: Long
)