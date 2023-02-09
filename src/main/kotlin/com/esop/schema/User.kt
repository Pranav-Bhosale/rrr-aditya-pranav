package com.esop.schema

import com.esop.dto.AddInventoryDTO
import com.esop.dto.CreateOrderDTO

class User(
    var firstName: String,
    var lastName: String,
    var phoneNumber: String,
    var email: String,
    var username: String
) {
    val userWallet: Wallet = Wallet()
    val userNonPerfInventory: Inventory = Inventory(type = "NON_PERFORMANCE")
    val userPerformanceInventory: Inventory = Inventory(type = "PERFORMANCE")
    val orderList: ArrayList<Order> = ArrayList()

    fun addToWallet(amountToBeAdded: Long) {
        userWallet.addMoneyToWallet(amountToBeAdded)
    }

    fun addToInventory(inventoryData: AddInventoryDTO): String {
        if (inventoryData.esopType == "NON_PERFORMANCE") {
            userNonPerfInventory.addESOPsToInventory(inventoryData.quantity)
        } else if (inventoryData.esopType == "PERFORMANCE") {
            userPerformanceInventory.addESOPsToInventory(inventoryData.quantity)
            return "${inventoryData.quantity} Performance ESOPs added to account."
        }
        return "None"
    }

    fun checkBalance(amountToBeChecked: Long): Boolean {
        return userWallet.checkBalance(amountToBeChecked)
    }

    fun lockAmount(price: Long) {
        userWallet.moveMoneyFromFreeToLockedState(price)
    }

    fun getInventory(type: String): Inventory {
        if (type == "PERFORMANCE") return userPerformanceInventory
        return userNonPerfInventory
    }

    fun transferLockedESOPsTo(buyer: User, esopTransferData: InventoryTransferRequest) {
        this.getInventory(esopTransferData.sellerInventoryType)
            .removeESOPsFromLockedState(esopTransferData.transferQuantity)
        buyer.getInventory("NON_PERFORMANCE").addESOPsToInventory(esopTransferData.transferQuantity)
    }

    fun addOrder(order: Order) {
        orderList.add(order)
    }

    fun checkInventory(orderRequest: CreateOrderDTO): Boolean {
        if (orderRequest.esopType == "PERFORMANCE") return userPerformanceInventory.checkInventory(orderRequest.quantity)
        return userNonPerfInventory.checkInventory(orderRequest.quantity)
    }

    fun lockInventory(inventoryType: String, esopsToBeChecked: Long) {
        if (inventoryType == "PERFORMANCE") {
            userPerformanceInventory.moveESOPsFromFreeToLockedState(esopsToBeChecked)
        } else {
            userNonPerfInventory.moveESOPsFromFreeToLockedState(esopsToBeChecked)
        }
    }
}