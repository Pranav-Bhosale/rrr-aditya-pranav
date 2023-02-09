package com.esop.schema

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("SameParameterValue")
class OrderTest {

    private fun createBuyOrder(quantity: Long, price: Long): Order {
        return Order(1, quantity = quantity, type = "BUY", price = price, userName = "abc", esopType = null)
    }

    private fun createPerformanceSellOrder(quantity: Long, price: Long): Order {
        return Order(1, quantity = quantity, type = "SELL", price = price, userName = "abc", esopType = "PERFORMANCE")
    }

    private fun createNonPerformanceSellOrder(quantity: Long, price: Long): Order {
        return Order(
            1,
            quantity = quantity,
            type = "SELL",
            price = price,
            userName = "abc",
            esopType = "NON_PERFORMANCE"
        )
    }

    @Test
    fun `it should update remaining quantity`() {
        val buy = createBuyOrder(10, 10)
        val expectedRemainingQuantity = 5L

        buy.subtractFromRemainingQuantity(5L)

        assertEquals(expectedRemainingQuantity, buy.remainingQuantity)
    }

    @Test
    fun `it should set the status as completed`() {
        val buy = createBuyOrder(10, 10)
        buy.remainingQuantity = 0

        buy.updateStatus()

        assertEquals("COMPLETED", buy.orderStatus)
    }

    @Test
    fun `it should set the status as partial`() {
        val buy = createBuyOrder(10, 10)
        buy.remainingQuantity = 5

        buy.updateStatus()

        assertEquals("PARTIAL", buy.orderStatus)
    }

    @Test
    fun `it should add order log`() {
        val buyOrder = createBuyOrder(10, 10)
        val buyOrderLog = OrderFilledLog(
            10,
            10,
            null,
            "Sankar",
            null
        )

        buyOrder.addExecutionDetails(buyOrderLog)

        assertEquals(1, buyOrder.orderFilledLogs.size)
    }
}