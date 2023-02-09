package com.esop.repository

import com.esop.schema.Order
import jakarta.inject.Singleton

@Singleton
class OrderRecords {
    private var orderId = 1L
    private var buyOrders = mutableListOf<Order>()
    private var sellOrders = mutableListOf<Order>()

    fun generateOrderId(): Long {
        return orderId++
    }

    fun addOrder(order: Order) {
        if (order.getType() == "BUY") {
            buyOrders.add(order)
        } else if (order.getType() == "SELL") {
            sellOrders.add(order)
        }
    }

    fun removeOrderIfFilled(order: Order) {
        if(order.orderStatus == "COMPLETED") {
            if (order.getType() == "BUY") {
                buyOrders.remove(order)
            } else if (order.getType() == "SELL") {
                sellOrders.remove(order)
            }
        }
    }

    fun getBuyOrder(): Order? {
        if (buyOrders.size > 0) {
            var sortedBuyOrders =
                buyOrders.sortedWith(compareByDescending<Order> { it.getPrice() }.thenBy { it.timeStamp })
            return sortedBuyOrders[0]
        }
        return null
    }

    fun getSellOrder(): Order? {
        if (sellOrders.size > 0) {
            var sortedSellOrders = sortAscending()
            return sortedSellOrders[0]
        }
        return null
    }

    private fun sortAscending(): List<Order> {
        return sellOrders.sortedWith(object : Comparator<Order> {
            override fun compare(o1: Order, o2: Order): Int {

                if (o1.inventoryPriority.priority != o2.inventoryPriority.priority)
                    return o1.inventoryPriority.priority - o2.inventoryPriority.priority

                if (o1.inventoryPriority.priority == 1) {
                    if (o1.getPrice() == o2.getPrice()) {
                        if (o1.timeStamp < o2.timeStamp)
                            return -1
                        return 1
                    }
                    if (o1.getPrice() < o2.getPrice())
                        return -1
                    return 1
                }

                if (o1.getPrice() == o2.getPrice()) {
                    if (o1.timeStamp < o2.timeStamp)
                        return -1
                    return 1
                }
                if (o1.getPrice() < o2.getPrice())
                    return -1
                return 1
            }
        })
    }
}