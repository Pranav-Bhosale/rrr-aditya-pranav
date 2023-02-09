package com.esop.schema

import com.esop.MAX_INVENTORY_CAPACITY
import java.util.*

class Inventory(
    private var freeInventory: Long = 0L,
    private var lockedInventory: Long = 0L,
    private var type: String
) {

    private fun totalESOPQuantity(): Long {
        return freeInventory + lockedInventory
    }

    fun addESOPsToInventory(esopsToBeAdded: Long) {
        this.freeInventory = this.freeInventory + esopsToBeAdded
    }

    fun checkInventoryOverflow(esopsToBeAddedInInventory: Long): List<String> {
        val modifiedInventory = totalESOPQuantity() + esopsToBeAddedInInventory
        if (modifiedInventory > MAX_INVENTORY_CAPACITY) {
            return listOf("Inventory Limit exceeded")
        }
        return emptyList()
    }

    fun moveESOPsFromFreeToLockedState(esopsToBeLocked: Long): String {
        if (this.freeInventory < esopsToBeLocked) {
            return "Insufficient ${type.lowercase(Locale.getDefault())} inventory."
        }
        this.freeInventory = this.freeInventory - esopsToBeLocked
        this.lockedInventory = this.lockedInventory + esopsToBeLocked
        return "SUCCESS"
    }

    fun getFreeInventory(): Long {
        return freeInventory
    }

    fun getLockedInventory(): Long {
        return lockedInventory
    }

    fun removeESOPsFromLockedState(esopsToBeRemoved: Long) {
        this.lockedInventory = this.lockedInventory - esopsToBeRemoved
    }

    fun checkInventory(esopsToBeChecked: Long): Boolean {
        if (freeInventory < esopsToBeChecked) return false
        return true
    }
}