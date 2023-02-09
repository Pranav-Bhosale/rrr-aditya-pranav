package com.esop.schema

import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO

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

    fun addToWallet(walletData: AddWalletDTO): String {
        userWallet.addMoneyToWallet(walletData.price!!)
        return "${walletData.price} amount added to account."
    }

    fun addToInventory(inventoryData: AddInventoryDTO): String {
        if (inventoryData.esopType.toString().uppercase() == "NON_PERFORMANCE") {
            userNonPerfInventory.addESOPsToInventory(inventoryData.quantity!!)
            return "${inventoryData.quantity} Non-Performance ESOPs added to account."
        } else if (inventoryData.esopType.toString().uppercase() == "PERFORMANCE") {
            userPerformanceInventory.addESOPsToInventory(inventoryData.quantity!!)
            return "${inventoryData.quantity} Performance ESOPs added to account."
        }
        return "None"
    }

    fun lockPerformanceInventory(quantity: Long): String {
        return userPerformanceInventory.moveESOPsFromFreeToLockedState(quantity)
    }

    fun lockNonPerformanceInventory(quantity: Long): String {
        return userNonPerfInventory.moveESOPsFromFreeToLockedState(quantity)
    }

    fun checkBalance(amountToBeChecked: Long): Boolean {
        return userWallet.checkBalance(amountToBeChecked)
    }

    fun lockAmount(price: Long) {
        userWallet.moveMoneyFromFreeToLockedState(price)
    }

    private fun getInventory(type: String): Inventory {
        if (type == "PERFORMANCE") return userPerformanceInventory
        return userNonPerfInventory
    }

    fun transferLockedESOPsTo(buyer: User, esopTransferData: EsopTransferRequest) {
        this.getInventory(esopTransferData.esopType).removeESOPsFromLockedState(esopTransferData.currentTradeQuantity)
        buyer.getInventory("NON_PERFORMANCE").addESOPsToInventory(esopTransferData.currentTradeQuantity)
    }

    fun addOrder(order: Order) {
        orderList.add(order)
    }

    fun checkInventory(inventoryType: String, esopsToBeChecked: Long): Boolean{
        if(inventoryType == "PERFORMANCE") return userPerformanceInventory.checkInventory(esopsToBeChecked)
        return userNonPerfInventory.checkInventory(esopsToBeChecked)
    }

    fun lockInventory(inventoryType: String, esopsToBeChecked: Long){
        if(inventoryType == "PERFORMANCE"){
            userPerformanceInventory.moveESOPsFromFreeToLockedState(esopsToBeChecked)
        }
        else{
            userNonPerfInventory.moveESOPsFromFreeToLockedState(esopsToBeChecked)
        }
    }
}