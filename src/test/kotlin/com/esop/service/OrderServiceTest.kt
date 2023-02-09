package com.esop.service

import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO
import com.esop.dto.CreateOrderDTO
import com.esop.repository.OrderRecords
import com.esop.repository.UserRecords
import com.esop.schema.Order
import com.esop.schema.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

@Suppress("SameParameterValue")
class OrderServiceTest {

    private lateinit var userRecords: UserRecords
    private lateinit var orderRecords: OrderRecords
    private lateinit var orderService: OrderService
    private lateinit var userService: UserService

    @BeforeEach
    fun `It should create user`() {
        userRecords = UserRecords()
        orderRecords = OrderRecords()
        userService = UserService(userRecords)
        orderService = OrderService(userService, userRecords, orderRecords)

        val buyer1 = User("Sankaranarayanan", "M", "7550276216", "sankaranarayananm@sahaj.ai", "sankar")
        val buyer2 = User("Aditya", "Tiwari", "", "aditya@sahaj.ai", "aditya")
        val seller1 = User("Kajal", "Pawar", "", "kajal@sahaj.ai", "kajal")
        val seller2 = User("Arun", "Murugan", "", "arun@sahaj.ai", "arun")

        userRecords.addUser(buyer1)
        userRecords.addUser(buyer2)
        userRecords.addUser(seller1)
        userRecords.addUser(seller2)
    }

    private fun addNonPerformanceInventoryToUser(userName: String, quantity: Long) {
        userService.addingInventory(AddInventoryDTO(quantity, "NON_PERFORMANCE"), userName)
    }

    private fun addPerformanceInventoryToUser(userName: String, quantity: Long) {
        userService.addingInventory(AddInventoryDTO(quantity, "PERFORMANCE"), userName)
    }

    private fun addMoneyToUser(userName: String, amount: Long) {
        userService.addingMoney(AddWalletDTO(amount), userName)
    }

    private fun createNonPerformanceSellOrderForUser(userName: String, quantity: Long, price: Long): Order {
        return orderService.createOrder(userName, CreateOrderDTO("SELL", quantity, price, "NON_PERFORMANCE"))
    }

    private fun createPerformanceSellOrderForUser(userName: String, quantity: Long, price: Long): Order {
        return orderService.createOrder(userName, CreateOrderDTO("SELL", quantity, price, "PERFORMANCE"))
    }

    private fun createBuyOrderForUser(userName: String, quantity: Long, price: Long): Order {
        return orderService.createOrder(userName, CreateOrderDTO("BUY", quantity, price, null))
    }

    @Test
    fun `It should place BUY order`() {
        //Arrange
        val buyOrder = Order(10, "BUY", 10, "sankar", null)

        //Act
        orderService.executeOrder(buyOrder)

        //Assert
        assertTrue {
            buyOrder == orderRecords.getBuyOrder()
        }
    }

    @Test
    fun `It should place SELL order`() {
        //Arrange
        val sellOrder = Order(10, "SELL", 10, "kajal", "NON_PERFORMANCE")

        //Act
        orderService.executeOrder(sellOrder)

        //Assert
        assertTrue {
            sellOrder == orderRecords.getSellOrder()
        }
    }

    @Test
    fun `It should match BUY order for existing SELL order`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        createNonPerformanceSellOrderForUser("kajal", 10, 10)

        addMoneyToUser("sankar", 100)
        val buyOrder = createBuyOrderForUser("sankar", 10, 10)

        //Act
        orderService.executeOrder(buyOrder)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
    }

    @Test
    fun `It should place 2 SELL orders followed by a BUY order where the BUY order is partial`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        addNonPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createNonPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 25, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(98, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(50, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "PARTIAL",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders followed by a BUY order where the BUY order is complete`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        addNonPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createNonPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(98, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 1 SELL orders followed by a BUY order where the BUY order is complete`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 5, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(5, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(49, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "PARTIAL",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
    }

    @Test
    fun `It should place 1 SELL orders followed by a BUY order where the BUY order is partial`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)


        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 15, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(50, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "PARTIAL",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
    }

    @Test
    fun `It should place 2 BUY orders followed by a SELL order where the SELL order is partial`() {
        //Arrange
        addMoneyToUser("sankar", 100)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 10, 10)


        addMoneyToUser("aditya", 100)
        val buyOrderByAditya = createBuyOrderForUser("aditya", 10, 10)

        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 25, 10)

        //Act
        orderService.executeOrder(sellOrderByKajal)

        //Assert
        assertEquals(25, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("aditya")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(196, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
        assertEquals(
            "PARTIAL",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("aditya")!!.orderList[userRecords.getUser("aditya")!!.orderList.indexOf(buyOrderByAditya)].orderStatus
        )
    }

    @Test
    fun `It should place 2 BUY orders followed by a SELL order where the SELL order is complete`() {
        //Arrange
        addMoneyToUser("kajal", 100)
        val buyOrderByKajal = createBuyOrderForUser("kajal", 10, 10)

        addMoneyToUser("arun", 100)
        val buyOrderByArun = createBuyOrderForUser("arun", 10, 10)

        addNonPerformanceInventoryToUser("sankar", 30)
        val sellOrderBySankar = createNonPerformanceSellOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(sellOrderBySankar)

        //Assert
        assertEquals(10, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(0, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())

        assertEquals(10, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(0, userRecords.getUser("arun")!!.userWallet.getFreeMoney())

        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98 + 98, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())

        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(
                sellOrderBySankar
            )].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(buyOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(buyOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should match BUY order for existing SELL order for PERFORMANCE esop type`() {
        //Arrange
        addPerformanceInventoryToUser("kajal", 50)
        createPerformanceSellOrderForUser("kajal", 10, 10)

        addMoneyToUser("sankar", 100)
        val buyOrder = createBuyOrderForUser("sankar", 10, 10)

        //Act
        orderService.executeOrder(buyOrder)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(100, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
    }

    @Test
    fun `It should match SELL order for PERFORMANCE for existing BUY order`() {
        //Arrange
        addMoneyToUser("sankar", 100)
        createBuyOrderForUser("sankar", 10, 10)

        addPerformanceInventoryToUser("kajal", 50)
        val sellOrder = createPerformanceSellOrderForUser("kajal", 10, 10)

        //Act
        orderService.executeOrder(sellOrder)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(100, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
    }

    @Test
    fun `It should match SELL order for existing BUY order where SELL order is complete`() {
        //Arrange
        addMoneyToUser("sankar", 200)
        createBuyOrderForUser("sankar", 20, 10)

        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrder = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        //Act
        orderService.executeOrder(sellOrder)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(10, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getFreeMoney())
    }

    @Test
    fun `It should place 2 SELL orders where one SELL order is of PERFORMANCE esop type and other is of NON-PERFORMANCE esop type followed by a BUY order where the BUY order is complete`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        addPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(100, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders of PERFORMANCE esop type followed by a BUY order where the BUY order is complete`() {
        //Arrange
        addPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createPerformanceSellOrderForUser("kajal", 10, 10)

        addPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(100, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(100, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders of PERFORMANCE esop type followed by a BUY order where higher timestamp order placed first`() {
        //Arrange
        addPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createPerformanceSellOrderForUser("kajal", 10, 10)

        sleep(10)
        addPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createPerformanceSellOrderForUser("arun", 10, 10)
        orderService.executeOrder(sellOrderByKajal)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userPerformanceInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(100, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(100, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders of NON-PERFORMANCE esop type followed by a BUY order where higher timestamp order placed first`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 10)

        sleep(10)
        addNonPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createNonPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 250)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 10)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(98, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(98, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders of NON-PERFORMANCE esop type followed by a BUY order where SELL order price is different`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 20)

        addNonPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createNonPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 400)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 20)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(196, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(98, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }

    @Test
    fun `It should place 2 SELL orders of NON-PERFORMANCE esop type followed by a BUY order where lower SELL order price is placed first`() {
        //Arrange
        addNonPerformanceInventoryToUser("kajal", 50)
        val sellOrderByKajal = createNonPerformanceSellOrderForUser("kajal", 10, 20)

        addNonPerformanceInventoryToUser("arun", 50)
        val sellOrderByArun = createNonPerformanceSellOrderForUser("arun", 10, 10)

        addMoneyToUser("sankar", 400)
        val buyOrderBySankar = createBuyOrderForUser("sankar", 20, 20)

        //Act
        orderService.executeOrder(buyOrderBySankar)

        //Assert
        assertEquals(40, userRecords.getUser("kajal")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(40, userRecords.getUser("arun")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(20, userRecords.getUser("sankar")!!.userNonPerfInventory.getFreeInventory())
        assertEquals(196, userRecords.getUser("kajal")!!.userWallet.getFreeMoney())
        assertEquals(98, userRecords.getUser("arun")!!.userWallet.getFreeMoney())
        assertEquals(0, userRecords.getUser("sankar")!!.userWallet.getLockedMoney())
        assertEquals(
            "COMPLETED",
            userRecords.getUser("sankar")!!.orderList[userRecords.getUser("sankar")!!.orderList.indexOf(buyOrderBySankar)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("kajal")!!.orderList[userRecords.getUser("kajal")!!.orderList.indexOf(sellOrderByKajal)].orderStatus
        )
        assertEquals(
            "COMPLETED",
            userRecords.getUser("arun")!!.orderList[userRecords.getUser("arun")!!.orderList.indexOf(sellOrderByArun)].orderStatus
        )
    }
}
