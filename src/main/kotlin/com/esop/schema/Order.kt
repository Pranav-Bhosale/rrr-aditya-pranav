package com.esop.schema

import com.esop.schema.InventoryPriority.*

enum class InventoryPriority(val priority: Int) {
    NONE(0),
    PERFORMANCE(1),
    NON_PERFORMANCE(2)
}

class Order(
    private val orderId: Long,
    private val quantity: Long,
    private val type: String,
    private val price: Long,
    private val userName: String,
    val esopType: String?
) {
    val timeStamp = System.currentTimeMillis()
    var orderStatus: String = "PENDING" // COMPLETED, PARTIAL, PENDING
    val orderFilledLogs: MutableList<OrderFilledLog> = mutableListOf()


    var inventoryPriority = NONE
    var remainingQuantity = quantity

    init {
        if (isTypeSellAndEsopTypePerformance()) {
            inventoryPriority = PERFORMANCE
        } else if (isTypeSellAndEsopTypeNonPerformance()) {
            inventoryPriority = NON_PERFORMANCE
        }
    }

    private fun isTypeSellAndEsopTypePerformance() = type == "SELL" && esopType == "PERFORMANCE"

    private fun isTypeSellAndEsopTypeNonPerformance() = type == "SELL" && esopType == "NON_PERFORMANCE"
    fun getQuantity(): Long {
        return quantity
    }

    fun getPrice(): Long {
        return price
    }

    fun getOrderValue(): Long {
        return price * quantity
    }

    fun getType(): String {
        return type
    }

    fun getUserName(): String {
        return userName
    }

    fun getOrderId(): Long {
        return orderId
    }


    fun subtractFromRemainingQuantity(quantityToBeUpdated: Long) {
        remainingQuantity -= quantityToBeUpdated
    }

    fun updateStatus() {
        if (remainingQuantity == 0L) {
            orderStatus = "COMPLETED"
        } else if (remainingQuantity != quantity) {
            orderStatus = "PARTIAL"
        }
    }

    fun addExecutionDetails(orderFilledLog: OrderFilledLog) {
        orderFilledLogs.add(orderFilledLog)
    }
}