package com.esop.service

import com.esop.InventoryLimitExceededException
import com.esop.MAX_INVENTORY_CAPACITY
import com.esop.MAX_WALLET_CAPACITY
import com.esop.WalletLimitExceededException
import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO
import com.esop.dto.CreateOrderDTO
import com.esop.dto.UserCreationDTO
import com.esop.repository.OrderRecords
import com.esop.repository.UserRecords
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        orderService = OrderService(userRecords, orderRecords)
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

        val errors = orderService.validateOrderReq(userName, order)

        assertEquals(expectedErrors, errors, "user non existent error should be present in the errors list")
    }

    @Test
    fun `should add money to wallet`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "Sankar")
        userService.registerUser(user)
        val walletDetails = AddWalletDTO(price = 1000)
        val expectedFreeMoney: Long = 1000
        val expectedUsername = "Sankar"

        userService.addingMoney(walletDetails, "Sankar")

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

        userService.addingInventory(inventoryDetails, "Sankar")

        val actualFreeMoney = userRecords.getUser(expectedUsername)!!.userNonPerfInventory.getFreeInventory()
        assertEquals(expectedFreeInventory, actualFreeMoney)
    }

    @Test
    fun `should check if return empty list if there is sufficient free amount is in wallet to place BUY order`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingMoney(AddWalletDTO(price = 100L), userName = "sankar06")
        val order = CreateOrderDTO(
            quantity = 10, type = "BUY", price = 10, esopType = null
        )

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

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
        userService.addingMoney(AddWalletDTO(price = 99L), userName = "sankar06")

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

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
        userService.addingMoney(AddWalletDTO(200), userName = "sankar06")
        userService.addingInventory(AddInventoryDTO(MAX_INVENTORY_CAPACITY, "NON_PERFORMANCE"), userName = "sankar06")

        assertThrows<InventoryLimitExceededException> {
            orderService.validateOrderReq(user.username!!, order)
        }
    }

    @Test
    fun `it should return empty error list when there is sufficient free Non Performance ESOPs in the Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventory(AddInventoryDTO(quantity = 10L), userName = "sankar06")
        val order = CreateOrderDTO(
            quantity = 10, type = "SELL", price = 10, esopType = "NON_PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

        val expectedErrors = emptyList<String>()
        assertEquals(expectedErrors, actualErrors, "error list returned must be empty")
    }

    @Test
    fun `it should return error list with error when there is insufficient free Non Performance ESOPs in Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventory(AddInventoryDTO(quantity = 10L), userName = "sankar06")
        val order = CreateOrderDTO(
            quantity = 29, type = "SELL", price = 10, esopType = "NON_PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

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
        userService.addingInventory(AddInventoryDTO(20,"NON_PERFORMANCE"), userName = "sankar06")
        userService.addingMoney(AddWalletDTO(MAX_WALLET_CAPACITY), userName = "sankar06")

        assertThrows<WalletLimitExceededException> {
            orderService.validateOrderReq(user.username!!, order)
        }
    }

    @Test
    fun `it should return empty error list when there is sufficient free Performance ESOPs in the Inventory`() {
        val user = UserCreationDTO("Sankar", "M", "+917550276216", "sankar@sahaj.ai", "sankar06")
        userService.registerUser(user)
        userService.addingInventory(AddInventoryDTO(quantity = 10L, esopType = "PERFORMANCE"), userName = "sankar06")
        val order = CreateOrderDTO(
            quantity = 10, type = "SELL", price = 10, esopType = "PERFORMANCE"
        )

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

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

        val actualErrors = orderService.validateOrderReq(user.username!!, order)

        val expectedErrors = listOf("Insufficient performance inventory.")
        assertEquals(
            expectedErrors,
            actualErrors,
            "error list should contain \"Insufficient performance inventory.\""
        )
    }

}