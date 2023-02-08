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

    fun addBuyOrder(buyOrder: Order) {
        buyOrders.add(buyOrder)
    }

    fun addSellOrder(sellOrder: Order) {
        sellOrders.add(sellOrder)
    }

    fun removeBuyOrder(buyOrder: Order) {
        buyOrders.remove(buyOrder)
    }

    fun removeSellOrder(sellOrder: Order) {
        sellOrders.remove(sellOrder)
    }

    fun getBuyOrder(): Order? {
        if(buyOrders.size > 0){
            var sortedBuyOrders = buyOrders.sortedWith(compareByDescending<Order> { it.getPrice() }.thenBy { it.timeStamp })
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
                    if(o1.getPrice() == o2.getPrice()){
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