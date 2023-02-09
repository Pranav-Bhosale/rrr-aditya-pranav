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
        if (order.orderStatus == "COMPLETED") {
            if (order.getType() == "BUY") {
                buyOrders.remove(order)
            } else if (order.getType() == "SELL") {
                sellOrders.remove(order)
            }
        }
    }

    fun getMatchBuyOrder(sellOrder: Order): Order? {
        if (buyOrders.isNotEmpty()) {
            val sortedBuyOrders = sortBuyOrders()
            val matchBuyOrder = sortedBuyOrders[0]
            if (matchBuyOrder.getPrice() >= sellOrder.getPrice())
                return matchBuyOrder
        }
        return null
    }

    fun getMatchSellOrder(buyOrder: Order): Order? {
        if (sellOrders.isNotEmpty()) {
            val sortedSellOrders = sortSellOrders()
            val matchSellOrder = sortedSellOrders[0]
            if (matchSellOrder.getPrice() <= buyOrder.getPrice())
                return matchSellOrder
        }
        return null
    }

    private fun sortBuyOrders() =
        buyOrders.sortedWith(compareByDescending<Order> { it.getPrice() }.thenBy { it.timeStamp })

    private fun sortSellOrders(): List<Order> {
        return sellOrders.sortedWith(object : Comparator<Order> {
            override fun compare(order1: Order, order2: Order): Int {

                if (order1.inventoryPriority.priority != order2.inventoryPriority.priority)
                    return order1.inventoryPriority.priority - order2.inventoryPriority.priority

                if (order1.inventoryPriority.priority == 1) {
                    if (order1.getPrice() == order2.getPrice()) {
                        if (order1.timeStamp < order2.timeStamp)
                            return -1
                        return 1
                    }
                    if (order1.getPrice() < order2.getPrice())
                        return -1
                    return 1
                }

                if (order1.getPrice() == order2.getPrice()) {
                    if (order1.timeStamp < order2.timeStamp)
                        return -1
                    return 1
                }
                if (order1.getPrice() < order2.getPrice())
                    return -1
                return 1
            }
        })
    }
}