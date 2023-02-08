package com.esop.repository

import com.esop.schema.Order
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

class OrderRecordsTest {
    private lateinit var orderRecords: OrderRecords

    @BeforeEach
    fun setup() {
        orderRecords = OrderRecords()
    }

    private fun createBuyOrder(quantity: Long, price: Long): Order {
        return Order(quantity = quantity, type = "BUY", price = price, userName = "abc", esopType = null)
    }

    private fun createPerformanceSellOrder(quantity: Long, price: Long): Order {
        return Order(quantity = quantity, type = "SELL", price = price, userName = "abc", esopType = "PERFORMANCE")
    }

    private fun createNonPerformanceSellOrder(quantity: Long, price: Long): Order {
        return Order(quantity = quantity, type = "SELL", price = price, userName = "abc", esopType = "NON_PERFORMANCE")
    }

    @Test
    fun `should return order id`() {
        val expectedOrderId = 1L

        val response = orderRecords.generateOrderId()

        assertEquals(expectedOrderId, response)
    }

    @Test
    fun `should return sell order`() {
        val sellOrder = Order(10, "SELL", 10, "sankar", "NON_PERFORMANCE")
        orderRecords.addSellOrder(sellOrder)
        val expectedOrderType = "SELL"

        val response = orderRecords.getSellOrder()

        assertEquals(expectedOrderType, response?.getType())
    }

    @Test
    fun `getSellOrder should return empty`() {
        val response = orderRecords.getSellOrder()

        assertNull(response)
    }

    @Test
    fun `should return buy order`() {
        val buyOrder = Order(10, "BUY", 10, "sankar", null)
        orderRecords.addBuyOrder(buyOrder)
        val expectedOrderType = "BUY"

        val response = orderRecords.getBuyOrder()

        assertEquals(expectedOrderType, response?.getType())
    }

    @Test
    fun `getBuyOrder should return empty`() {
        val response = orderRecords.getBuyOrder()

        assertNull(response)
    }

    @Test
    fun `it should remove buy order`() {
        val buyOrder = Order(10, "BUY", 10, "sankar", null)
        orderRecords.addBuyOrder(buyOrder)
        val response = orderRecords.getBuyOrder()

        orderRecords.removeBuyOrder(response!!)

        assertNull(orderRecords.getBuyOrder())
    }

    @Test
    fun `it should remove sell order`() {
        val sellOrder = Order(10, "SELL", 10, "sankar", "NON_PERFORMANCE")
        orderRecords.addSellOrder(sellOrder)
        val response = orderRecords.getSellOrder()

        orderRecords.removeSellOrder(response!!)

        assertNull(orderRecords.getSellOrder())
    }

    @Test
    fun `it should return performance sell order when one non performance sell order and performance sell order is added`() {
        val nonPerformanceSellOrder = createNonPerformanceSellOrder(15, 10)
        val performanceSellOrder = createPerformanceSellOrder(15, 10)
        orderRecords.addSellOrder(nonPerformanceSellOrder)
        orderRecords.addSellOrder(performanceSellOrder)

        val response = orderRecords.getSellOrder()

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
    }

    @Test
    fun `it should return non performance sell order whose price is less when two non performance sell order are added`() {
        val nonPerformanceSellOrder1 = createNonPerformanceSellOrder(10, 10)
        val nonPerformanceSellOrder2 = createNonPerformanceSellOrder(10, 15)
        orderRecords.addSellOrder(nonPerformanceSellOrder1)
        orderRecords.addSellOrder(nonPerformanceSellOrder2)

        val response = orderRecords.getSellOrder()

        val expectedOrderType = "SELL"
        val expectedEsopType = "NON_PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return performance sell order which is added early when two performance sell order has same price`() {
        val performanceSellOrder1 = createPerformanceSellOrder(10, 10)
        sleep(10)
        val performanceSellOrder2 = createPerformanceSellOrder(10, 10)
        orderRecords.addSellOrder(performanceSellOrder2)
        orderRecords.addSellOrder(performanceSellOrder1)

        val response1 = orderRecords.getSellOrder()
        if (response1 != null) {
            orderRecords.removeSellOrder(response1)
        }
        val response2 = orderRecords.getSellOrder()

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return performance sell order when two performance sell order has same price`() {
        val performanceSellOrder1 = createPerformanceSellOrder(10, 10)
        sleep(10)
        val performanceSellOrder2 = createPerformanceSellOrder(10, 10)
        orderRecords.addSellOrder(performanceSellOrder1)
        orderRecords.addSellOrder(performanceSellOrder2)

        val response1 = orderRecords.getSellOrder()
        if (response1 != null) {
            orderRecords.removeSellOrder(response1)
        }
        val response2 = orderRecords.getSellOrder()

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return non performance sell order which is added early when two non performance sell order has same price`() {
        val nonPerformanceSellOrder1 = createNonPerformanceSellOrder(10, 10)
        sleep(10)
        val nonPerformanceSellOrder2 = createNonPerformanceSellOrder(10, 10)
        orderRecords.addSellOrder(nonPerformanceSellOrder2)
        orderRecords.addSellOrder(nonPerformanceSellOrder1)

        val response1 = orderRecords.getSellOrder()
        if (response1 != null) {
            orderRecords.removeSellOrder(response1)
        }
        val response2 = orderRecords.getSellOrder()

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return non performance sell order when two non performance sell order has same price`() {
        val nonPerformanceSellOrder1 = createNonPerformanceSellOrder(10, 10)
        sleep(10)
        val nonPerformanceSellOrder2 = createNonPerformanceSellOrder(10, 10)
        orderRecords.addSellOrder(nonPerformanceSellOrder1)
        orderRecords.addSellOrder(nonPerformanceSellOrder2)

        val response1 = orderRecords.getSellOrder()
        if (response1 != null) {
            orderRecords.removeSellOrder(response1)
        }
        val response2 = orderRecords.getSellOrder()

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return performance sell order whose price is less when two performance sell order are added`() {
        val performanceSellOrder1 = createPerformanceSellOrder(10, 10)
        val performanceSellOrder2 = createPerformanceSellOrder(10, 15)
        orderRecords.addSellOrder(performanceSellOrder1)
        orderRecords.addSellOrder(performanceSellOrder2)

        val response = orderRecords.getSellOrder()

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return performance sell order whose price is less when two performance sell order are added when addition of order is altered`() {
        val performanceSellOrder1 = createPerformanceSellOrder(10, 10)
        val performanceSellOrder2 = createPerformanceSellOrder(10, 15)
        orderRecords.addSellOrder(performanceSellOrder2)
        orderRecords.addSellOrder(performanceSellOrder1)

        val response = orderRecords.getSellOrder()

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return non performance sell order whose price is less when two non performance sell order are added when addition of order is altered`() {
        val nonPerformanceSellOrder1 = createNonPerformanceSellOrder(10, 10)
        val nonPerformanceSellOrder2 = createNonPerformanceSellOrder(10, 15)
        orderRecords.addSellOrder(nonPerformanceSellOrder2)
        orderRecords.addSellOrder(nonPerformanceSellOrder1)

        val response = orderRecords.getSellOrder()

        val expectedOrderType = "SELL"
        val expectedEsopType = "NON_PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return buy order which is added early when two buy order has same price`() {
        val buyOrder1 = createBuyOrder(10, 10)
        sleep(10)
        val buyOrder2 = createBuyOrder(10, 10)
        orderRecords.addBuyOrder(buyOrder1)
        orderRecords.addBuyOrder(buyOrder2)

        val response1 = orderRecords.getBuyOrder()
        if (response1 != null) {
            orderRecords.removeBuyOrder(response1)
        }
        val response2 = orderRecords.getBuyOrder()

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return buy order of highest price when two buy order has different price`() {
        val buyOrder1 = createBuyOrder(10, 10)
        val buyOrder2 = createBuyOrder(15, 15)
        orderRecords.addBuyOrder(buyOrder1)
        orderRecords.addBuyOrder(buyOrder2)

        val response = orderRecords.getBuyOrder()

        val expectedOrderType = "BUY"
        val expectedPrice = 15L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.getPrice() == expectedPrice)
    }
}