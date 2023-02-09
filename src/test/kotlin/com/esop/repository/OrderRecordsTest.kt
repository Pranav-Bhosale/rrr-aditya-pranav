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
        val buyOrder = Order(
            orderRecords.generateOrderId(),
            quantity = quantity,
            type = "BUY",
            price = price,
            userName = "abc",
            esopType = null
        )
        orderRecords.addOrder(buyOrder)
        return buyOrder
    }

    private fun createPerformanceSellOrder(quantity: Long, price: Long): Order {
        val performanceSellOrder = Order(
            orderRecords.generateOrderId(),
            quantity = quantity,
            type = "SELL",
            price = price,
            userName = "abc",
            esopType = "PERFORMANCE"
        )
        orderRecords.addOrder(performanceSellOrder)
        return performanceSellOrder
    }

    private fun createNonPerformanceSellOrder(quantity: Long, price: Long): Order {
        val nonPerformanceSellOrder = Order(
            orderRecords.generateOrderId(),
            quantity = quantity,
            type = "SELL",
            price = price,
            userName = "abc",
            esopType = "NON_PERFORMANCE"
        )
        orderRecords.addOrder(nonPerformanceSellOrder)
        return nonPerformanceSellOrder
    }

    @Test
    fun `should return order id`() {
        val expectedOrderId = 1L

        val response = orderRecords.generateOrderId()

        assertEquals(expectedOrderId, response)
    }

    @Test
    fun `should return best match sell order`() {
        val buyOrder = createBuyOrder(10, 10)
        createNonPerformanceSellOrder(10, 10)
        val expectedOrderType = "SELL"

        val response = orderRecords.getMatchSellOrder(buyOrder)

        assertEquals(expectedOrderType, response?.getType())
    }

    @Test
    fun `getSellOrder should return empty`() {
        val buyOrder = createBuyOrder(10, 10)

        val response = orderRecords.getMatchSellOrder(buyOrder)

        assertNull(response)
    }

    @Test
    fun `should return buy order`() {
        createBuyOrder(10, 10)
        val sellOrder = createNonPerformanceSellOrder(10, 10)
        val expectedOrderType = "BUY"

        val response = orderRecords.getMatchBuyOrder(sellOrder)

        assertEquals(expectedOrderType, response?.getType())
    }

    @Test
    fun `getBuyOrder should return empty`() {
        val sellOrder = createNonPerformanceSellOrder(10, 10)

        val response = orderRecords.getMatchBuyOrder(sellOrder)

        assertNull(response)
    }

    @Test
    fun `it should remove buy order`() {
        createBuyOrder(10, 10)
        val sellOrder = createNonPerformanceSellOrder(10, 10)
        val response = orderRecords.getMatchBuyOrder(sellOrder)

        response?.orderStatus = "COMPLETED"
        orderRecords.removeOrderIfFilled(response!!)

        assertNull(orderRecords.getMatchBuyOrder(sellOrder))
    }

    @Test
    fun `it should remove sell order`() {
        createNonPerformanceSellOrder(10, 10)
        val buyOrder = createBuyOrder(10, 10)
        val response = orderRecords.getMatchSellOrder(buyOrder)

        response?.orderStatus = "COMPLETED"
        orderRecords.removeOrderIfFilled(response!!)

        assertNull(orderRecords.getMatchSellOrder(buyOrder))
    }

    @Test
    fun `it should return performance sell order when one non performance sell order and performance sell order is added`() {
        val nonPerformanceSellOrder = createNonPerformanceSellOrder(15, 10)
        val performanceSellOrder = createPerformanceSellOrder(15, 10)
        val buyOrder = createBuyOrder(10, 10)
        orderRecords.addOrder(nonPerformanceSellOrder)
        orderRecords.addOrder(performanceSellOrder)

        val response = orderRecords.getMatchSellOrder(buyOrder)

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
    }

    @Test
    fun `it should return non performance sell order whose price is less when two non performance sell order are added`() {
        val nonPerformanceSellOrder1 = createNonPerformanceSellOrder(10, 10)
        val nonPerformanceSellOrder2 = createNonPerformanceSellOrder(10, 15)
        val buyOrder = createBuyOrder(10, 20)
        orderRecords.addOrder(nonPerformanceSellOrder1)
        orderRecords.addOrder(nonPerformanceSellOrder2)

        val response = orderRecords.getMatchSellOrder(buyOrder)

        val expectedOrderType = "SELL"
        val expectedEsopType = "NON_PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return performance sell order which is added early when two performance sell order has same price`() {
        createPerformanceSellOrder(10, 10)
        sleep(10)
        createPerformanceSellOrder(10, 10)
        val buyOrder = createBuyOrder(20, 10)

        val response1 = orderRecords.getMatchSellOrder(buyOrder)
        if (response1 != null) {
            response1.orderStatus = "COMPLETED"
            orderRecords.removeOrderIfFilled(response1)
        }
        val response2 = orderRecords.getMatchSellOrder(buyOrder)

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return performance sell order when two performance sell order has same price`() {
        createPerformanceSellOrder(10, 10)
        sleep(10)
        createPerformanceSellOrder(10, 10)
        val buyOrder = createBuyOrder(20, 10)

        val response1 = orderRecords.getMatchSellOrder(buyOrder)
        if (response1 != null) {
            response1.orderStatus = "COMPLETED"
            orderRecords.removeOrderIfFilled(response1)
        }
        val response2 = orderRecords.getMatchSellOrder(buyOrder)

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return non performance sell order which is added early when two non performance sell order has same price`() {
        createNonPerformanceSellOrder(10, 10)
        sleep(10)
        createNonPerformanceSellOrder(10, 10)
        val buyOrder = createBuyOrder(20, 10)

        val response1 = orderRecords.getMatchSellOrder(buyOrder)
        if (response1 != null) {
            response1.orderStatus = "COMPLETED"
            orderRecords.removeOrderIfFilled(response1)
        }
        val response2 = orderRecords.getMatchSellOrder(buyOrder)

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return non performance sell order when two non performance sell order has same price`() {
        createNonPerformanceSellOrder(10, 10)
        sleep(10)
        createNonPerformanceSellOrder(10, 10)
        val buyOrder = createBuyOrder(20, 10)

        val response1 = orderRecords.getMatchSellOrder(buyOrder)
        if (response1 != null) {
            response1.orderStatus = "COMPLETED"
            orderRecords.removeOrderIfFilled(response1)
        }
        val response2 = orderRecords.getMatchSellOrder(buyOrder)

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return performance sell order whose price is less when two performance sell order are added`() {
        createPerformanceSellOrder(10, 10)
        createPerformanceSellOrder(10, 15)
        val buyOrder = createBuyOrder(20, 10)
        val response = orderRecords.getMatchSellOrder(buyOrder)

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return performance sell order whose price is less when two performance sell order are added when addition of order is altered`() {
        createPerformanceSellOrder(10, 10)
        createPerformanceSellOrder(10, 15)
        val buyOrder = createBuyOrder(10, 10)
        val response = orderRecords.getMatchSellOrder(buyOrder)

        val expectedOrderType = "SELL"
        val expectedEsopType = "PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return non performance sell order whose price is less when two non performance sell order are added when addition of order is altered`() {
        createNonPerformanceSellOrder(10, 10)
        createNonPerformanceSellOrder(10, 15)
        val buyOrder = createBuyOrder(10, 10)

        val response = orderRecords.getMatchSellOrder(buyOrder)

        val expectedOrderType = "SELL"
        val expectedEsopType = "NON_PERFORMANCE"
        val expectedPrice = 10L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.esopType == expectedEsopType)
        assertTrue(response?.getPrice() == expectedPrice)
    }

    @Test
    fun `it should return buy order which is added early when two buy order has same price`() {
        createBuyOrder(10, 10)
        sleep(10)
        createBuyOrder(10, 10)
        val sellOrder = createNonPerformanceSellOrder(10, 10)

        val response1 = orderRecords.getMatchBuyOrder(sellOrder)
        if (response1 != null) {
            response1.orderStatus = "COMPLETED"
            orderRecords.removeOrderIfFilled(response1)
        }
        val response2 = orderRecords.getMatchBuyOrder(sellOrder)

        assertTrue {
            response1?.timeStamp!! < response2?.timeStamp!!
        }
    }

    @Test
    fun `it should return buy order of highest price when two buy order has different price`() {
        createBuyOrder(10, 10)
        createBuyOrder(15, 15)
        val sellOrder = createNonPerformanceSellOrder(10, 10)

        val response = orderRecords.getMatchBuyOrder(sellOrder)

        val expectedOrderType = "BUY"
        val expectedPrice = 15L
        assertTrue(response?.getType() == expectedOrderType)
        assertTrue(response?.getPrice() == expectedPrice)
    }
}