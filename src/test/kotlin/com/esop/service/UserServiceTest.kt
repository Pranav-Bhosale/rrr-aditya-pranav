package com.esop.service

import com.esop.MAX_INVENTORY_CAPACITY
import com.esop.MAX_WALLET_CAPACITY
import com.esop.dto.AddInventoryDTO
import com.esop.dto.CreateOrderDTO
import com.esop.dto.UserCreationDTO
import com.esop.repository.OrderRecords
import com.esop.repository.UserRecords
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserServiceTest {

    private lateinit var userService: UserService
    private lateinit var orderService: OrderService
    private lateinit var userRecords: UserRecords
    private lateinit var orderRecords: OrderRecords

    @BeforeEach
    fun setup() {
        userRecords = UserRecords()
        orderRecords = OrderRecords()
        userService = UserService(userRecords)
        orderService = OrderService(userService, userRecords, orderRecords)
    }


    @Test
    fun `should register a valid user`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        val expected = mapOf(
            "firstName" to "Sankar",
            "lastName" to "M",
            "phoneNumber" to "+917550276216",
            "email" to "sankar@sahaj.ai",
            "username" to "sankar06"
        )

        //Action
        val response = userService.registerUser(user)

        //Assert
        assertEquals(response, expected)
    }

    @Test
    fun `should check user doesn't exist before placing Order`() {
        val order = CreateOrderDTO(
            quantity = 10, type = "BUY", price = 10, esopType = null
        )
        val userName = "Sankar"
        val expectedErrors = listOf("User doesn't exist.")

        val errors = orderService.validateOrderRequest(userName, order)

        assertEquals(expectedErrors, errors, "user non existent error should be present in the errors list")
    }

    @Test
    fun `should add money to wallet`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "Sankar")
        userService.registerUser(user)
        val amountToBeAddedToWallet = 1000L
        val expectedFreeMoney: Long = 1000
        val expectedUsername = "Sankar"

        userService.addMoneyToUser("Sankar", amountToBeAddedToWallet)

        val actualFreeMoney = userRecords.getUser(expectedUsername)!!.userWallet.getFreeMoney()
        assertEquals(expectedFreeMoney, actualFreeMoney)
    }

    @Test
    fun `should add ESOPS to inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "Sankar")
        userService.registerUser(user)
        val inventoryDetails = AddInventoryDTO(quantity = 1000L, esopType = "NON_PERFORMANCE")
        val expectedFreeInventory: Long = 1000
        val expectedUsername = "Sankar"

        userService.addingInventoryToUser("Sankar", inventoryDetails)

        val actualFreeMoney = userRecords.getUser(expectedUsername)!!.userNonPerfInventory.getFreeInventory()
        assertEquals(expectedFreeInventory, actualFreeMoney)
    }

    @Test
    fun `should check if return empty list if there is sufficient free amount is in wallet to place BUY order`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addMoneyToUser("sankar06", 100L)
        val order = CreateOrderDTO(
            quantity = 10, type = "BUY", price = 10, esopType = null
        )

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = emptyList<String>()
        assertEquals(expectedErrors, actualErrors, "error list returned must be empty")
    }

    @Test
    fun `it should return error list with error if there is insufficient free amount in wallet to place BUY order`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        val order = CreateOrderDTO(
            quantity = 10, type = "BUY", price = 10, esopType = null
        )
        userService.addMoneyToUser("sankar06", 99L)

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = listOf("Insufficient funds")
        assertEquals(expectedErrors, actualErrors)
    }

    @Test
    fun `it should return error when the buyer inventory overflows`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        val order = CreateOrderDTO(
            quantity = 10, type = "BUY", price = 10, esopType = null
        )
        userService.addMoneyToUser("sankar06", 200)
        userService.addingInventoryToUser(
            userName = "sankar06",
            AddInventoryDTO(MAX_INVENTORY_CAPACITY, "NON_PERFORMANCE")
        )

        val validateOrderResponse = orderService.validateOrderRequest(user.username!!, order)
        assertTrue(validateOrderResponse.size == 1)
        assertTrue(validateOrderResponse[0] == "Inventory Limit exceeded")
    }

    @Test
    fun `it should return empty error list when there is sufficient free Non Performance ESOPs in the Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventoryToUser(userName = "sankar06", AddInventoryDTO(quantity = 10L))
        val order = CreateOrderDTO(
            quantity = 10, type = "SELL", price = 10, esopType = "NON_PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = emptyList<String>()
        assertEquals(expectedErrors, actualErrors, "error list returned must be empty")
    }

    @Test
    fun `it should return error list with error when there is insufficient free Non Performance ESOPs in Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventoryToUser(userName = "sankar06", AddInventoryDTO(quantity = 10L))
        val order = CreateOrderDTO(
            quantity = 29, type = "SELL", price = 10, esopType = "NON_PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = listOf("Insufficient non_performance inventory.")
        assertEquals(
            expectedErrors,
            actualErrors,
            "error list should contain \"Insufficient non_performance inventory.\""
        )
    }

    @Test
    fun `it should return error when the seller wallet overflows`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        val order = CreateOrderDTO(
            quantity = 10, type = "SELL", price = 10, esopType = "NON_PERFORMANCE"
        )
        userService.addingInventoryToUser(userName = "sankar06", AddInventoryDTO(20, "NON_PERFORMANCE"))
        userService.addMoneyToUser("sankar06", MAX_WALLET_CAPACITY)

        val validateOrderResponse = orderService.validateOrderRequest(user.username!!, order)
        assertTrue(validateOrderResponse.size == 1)
        assertTrue(validateOrderResponse[0] == "Wallet Limit exceeded")
    }

    @Test
    fun `it should return empty error list when there is sufficient free Performance ESOPs in the Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventoryToUser(
            userName = "sankar06",
            AddInventoryDTO(quantity = 10L, esopType = "PERFORMANCE")
        )
        val order = CreateOrderDTO(
            quantity = 10, type = "SELL", price = 10, esopType = "PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = emptyList<String>()
        assertEquals(expectedErrors, actualErrors, "error list returned must be empty")
    }

    @Test
    fun `it should return error list with error when there is insufficient free Performance ESOPs in Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        val order = CreateOrderDTO(
            quantity = 29, type = "SELL", price = 10, esopType = "PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderRequest(user.username!!, order)

        val expectedErrors = listOf("Insufficient performance inventory.")
        assertEquals(
            expectedErrors,
            actualErrors,
            "error list should contain \"Insufficient performance inventory.\""
        )
    }

}