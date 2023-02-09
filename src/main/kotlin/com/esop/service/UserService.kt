package com.esop.service

import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO
import com.esop.dto.UserCreationDTO
import com.esop.repository.UserRecords
import com.esop.schema.InventoryTransferRequest
import com.esop.schema.MoneyTransferRequest
import com.esop.schema.Order
import com.esop.schema.User
import jakarta.inject.Singleton

@Singleton
class UserService(private val userRecords: UserRecords) {

    fun addOrderToUser(order: Order) {
        userRecords.addOrder(order)
    }

    fun checkIfUserExists(userName: String): List<String> {
        if (!userRecords.checkIfUserExists(userName))
            return listOf("User doesn't exist.")
        return emptyList()
    }

    fun lockWalletForUser(userName: String, amountToBeLocked: Long) {
        val user = userRecords.getUser(userName)
        user?.lockAmount(amountToBeLocked)
    }

    fun lockInventoryForUser(userName: String, inventoryType: String, quantity: Long) {
        val user = userRecords.getUser(userName)
        user?.lockInventory(inventoryType, quantity)
    }

    fun registerUser(userData: UserCreationDTO): Map<String, String> {
        val user = User(
            userData.firstName.trim(),
            userData.lastName.trim(),
            userData.phoneNumber,
            userData.email,
            userData.username
        )
        userRecords.addUser(user)
        userRecords.addEmail(user.email)
        userRecords.addPhoneNumber(user.phoneNumber)
        return mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "phoneNumber" to user.phoneNumber,
            "email" to user.email,
            "username" to user.username
        )
    }

    fun accountInformation(userName: String): Map<String, Any?> {
        val errorList = checkIfUserExists(userName)

        if (errorList.isNotEmpty()) {
            return mapOf("error" to errorList)
        }
        val user = userRecords.getUser(userName)!!

        return mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "phoneNumber" to user.phoneNumber,
            "email" to user.email,
            "wallet" to mapOf(
                "free" to user.userWallet.getFreeMoney(),
                "locked" to user.userWallet.getLockedMoney()
            ),
            "inventory" to arrayListOf<Any>(
                mapOf(
                    "type" to "PERFORMANCE",
                    "free" to user.userPerformanceInventory.getFreeInventory(),
                    "locked" to user.userPerformanceInventory.getLockedInventory()
                ),
                mapOf(
                    "type" to "NON_PERFORMANCE",
                    "free" to user.userNonPerfInventory.getFreeInventory(),
                    "locked" to user.userNonPerfInventory.getLockedInventory()
                )
            )
        )
    }

    fun validateInventoryRequest(userName: String, inventoryRequest: AddInventoryDTO): List<String> {
        val errorList = mutableListOf<String>()
        errorList.addAll(checkIfUserExists(userName))
        if (errorList.isNotEmpty()) {
            return errorList
        }

        val user = userRecords.getUser(userName)!!
        val inventory = user.getInventory(inventoryRequest.esopType!!)
        errorList.addAll(inventory.checkInventoryOverflow(inventoryRequest.quantity))
        if (errorList.isNotEmpty()) {
            return errorList
        }
        return emptyList()
    }

    fun addingInventoryToUser(userName: String, inventoryData: AddInventoryDTO): String {
        val user = userRecords.getUser(userName)!!

        user.addToInventory(inventoryData)

        return "${inventoryData.quantity} ${inventoryData.esopType!!.lowercase()} esops added to account."
    }

    fun validateWalletRequest(userName: String, walletRequest: AddWalletDTO): List<String> {
        val errorList = mutableListOf<String>()
        errorList.addAll(checkIfUserExists(userName))
        if (errorList.isNotEmpty()) {
            return errorList
        }

        val user = userRecords.getUser(userName)!!
        val userWallet = user.userWallet
        errorList.addAll(userWallet.checkWalletOverflow(walletRequest.price))
        if (errorList.isNotEmpty()) {
            return errorList
        }
        return emptyList()
    }

    fun addMoneyToUser(userName: String, amountToBeAdded: Long): String {
        val user = userRecords.getUser(userName)!!

        user.addToWallet(amountToBeAdded)

        return "$amountToBeAdded amount added to account."
    }

    fun moveInventoryFromSellerToBuyer(
        sellerUserName: String,
        buyerUserName: String,
        inventoryTransferRequest: InventoryTransferRequest
    ) {
        val buyer = userRecords.getUser(buyerUserName)
        val seller = userRecords.getUser(sellerUserName)

        seller!!.getInventory(inventoryTransferRequest.sellerInventoryType)
            .removeESOPsFromLockedState(inventoryTransferRequest.transferQuantity)
        buyer!!.getInventory("NON_PERFORMANCE").addESOPsToInventory(inventoryTransferRequest.transferQuantity)
    }

    fun moveMoneyFromBuyerToSeller(
        buyerUserName: String,
        sellerUserName: String,
        moneyTransferRequest: MoneyTransferRequest
    ) {
        val buyer = userRecords.getUser(buyerUserName)
        val seller = userRecords.getUser(sellerUserName)

        buyer!!.userWallet.removeMoneyFromLockedState(moneyTransferRequest.amountToBeDebitedFromBuyersWallet)
        seller!!.userWallet.addMoneyToWallet(moneyTransferRequest.amountToBeCreditedToSellersAccount)

    }

    fun releaseLockedMoneyFromBuyer(buyerUserName: String, amountToBeReleased: Long) {
        val buyer = userRecords.getUser(buyerUserName)
        buyer!!.userWallet.moveMoneyFromLockedToFree(amountToBeReleased)
    }
}