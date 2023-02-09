package com.esop.service

import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO
import com.esop.dto.UserCreationDTO
import com.esop.repository.UserRecords
import com.esop.schema.User
import jakarta.inject.Singleton

@Singleton
class UserService(private val userRecords: UserRecords) {


    fun checkIfUserExists(userName: String): List<String> {
        if (!userRecords.checkIfUserExists(userName))
            return listOf("User doesn't exist.")
        return emptyList()
    }

    fun registerUser(userData: UserCreationDTO): Map<String, String> {
        val user = User(
            userData.firstName!!.trim(),
            userData.lastName!!.trim(),
            userData.phoneNumber!!,
            userData.email!!,
            userData.username!!
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


    fun addingInventory(inventoryData: AddInventoryDTO, userName: String): Map<String, Any> {
        val errorList = checkIfUserExists(userName)
        if (errorList.isNotEmpty()) {
            return mapOf("error" to errorList)
        }
        return mapOf("message" to userRecords.getUser(userName)!!.addToInventory(inventoryData))
    }

    fun addingMoney(walletData: AddWalletDTO, userName: String): Map<String, Any> {
        val errorList = checkIfUserExists(userName)
        if (errorList.isNotEmpty()) {
            return mapOf("error" to errorList)
        }
        val user = userRecords.getUser(userName)!!
        return mapOf("message" to user.addToWallet(walletData))
    }
}