package com.esop.schema

import com.esop.MAX_WALLET_CAPACITY

class Wallet {
    private var freeMoney: Long = 0
    private var lockedMoney: Long = 0

    private fun totalMoneyInWallet(): Long {
        return freeMoney + lockedMoney
    }

    fun checkWalletOverflow(amountToBeAddedInWallet: Long): List<String> {
        val modifiedWalletBalance = totalMoneyInWallet() + amountToBeAddedInWallet
        if (modifiedWalletBalance > MAX_WALLET_CAPACITY) {
            return listOf("Wallet Limit exceeded")
        }
        return emptyList()
    }

    fun addMoneyToWallet(amountToBeAdded: Long) {
        this.freeMoney = this.freeMoney + amountToBeAdded
    }

    fun moveMoneyFromFreeToLockedState(amountToBeLocked: Long) {
        this.freeMoney = this.freeMoney - amountToBeLocked
        this.lockedMoney = this.lockedMoney + amountToBeLocked
    }

    fun getFreeMoney(): Long {
        return freeMoney
    }

    fun getLockedMoney(): Long {
        return lockedMoney
    }

    fun removeMoneyFromLockedState(amountToBeRemoved: Long) {
        this.lockedMoney = this.lockedMoney - amountToBeRemoved
    }

    fun moveMoneyFromLockedToFree(amount: Long) {
        removeMoneyFromLockedState(amount)
        addMoneyToWallet(amount)
    }

    fun checkBalance(amountToBeChecked: Long): Boolean {
        if (freeMoney < amountToBeChecked) return false
        return true
    }
}