package com.esop.service


import com.esop.dto.CreateOrderDTO
import com.esop.repository.OrderRecords
import com.esop.repository.UserRecords
import com.esop.schema.*
import com.esop.schema.PlatformFee.Companion.addPlatformFee
import jakarta.inject.Singleton
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.math.round

private const val TWO_PERCENT = 0.02

@Singleton
class OrderService(
    private val userService: UserService,
    private val userRecords: UserRecords,
    private val orderRecords: OrderRecords
) {

    fun validateOrderReq(userName: String, orderRequest: CreateOrderDTO): MutableList<String> {
        val errorList = mutableListOf<String>()

        errorList.addAll(userService.checkIfUserExists(userName))
        if (errorList.isNotEmpty())
            return errorList
        val user = userRecords.getUser(userName)!!
        val wallet = user.userWallet
        val nonPerformanceInventory = user.userNonPerfInventory
        val orderValue = orderRequest.price!! * orderRequest.quantity!!

        if (orderRequest.type == "BUY") {
            if (!user.checkBalance(orderValue)) {
                errorList.add("Insufficient funds")
                return errorList
            }
            nonPerformanceInventory.assertInventoryWillNotOverflowOnAdding(orderRequest.quantity!!)
        } else if (orderRequest.type == "SELL") {
            if (!user.checkInventory(orderRequest.esopType!!, orderRequest.quantity!!)) {
                errorList.add("Insufficient ${orderRequest.esopType!!.lowercase(Locale.getDefault())} inventory.")
                return errorList
            }
            wallet.assertWalletWillNotOverflowOnAdding(orderValue)
        }
        return errorList
    }

    fun createOrder(userName: String, orderRequest: CreateOrderDTO): Order {
        val user = userRecords.getUser(userName)!!

        var esopType: String? = null
        if (orderRequest.type == "SELL") {
            esopType = orderRequest.esopType!!
        }

        val order = Order(
            orderRequest.quantity!!.toLong(),
            orderRequest.type.toString().uppercase(),
            orderRequest.price!!.toLong(),
            userName,
            esopType
        )
        order.orderID = orderRecords.generateOrderId()

        userRecords.addOrderToUser(order)

        if (order.getType() == "BUY") {
            orderRecords.addBuyOrder(order)
            user.lockAmount(order.getPrice() * order.getQuantity())
        } else {
            orderRecords.addSellOrder(order)
            user.lockInventory(order.esopType!!, order.getQuantity())
        }

        return order
    }

    private fun updateOrderDetails(
        currentTradeQuantity: Long,
        sellerOrder: Order,
        buyerOrder: Order
    ) {
        // Deduct money of quantity taken from buyer
        val sellAmount = sellerOrder.getPrice() * (currentTradeQuantity)
        val buyer = userRecords.getUser(buyerOrder.getUserName())!!
        val seller = userRecords.getUser(sellerOrder.getUserName())!!
        var platformFee = 0L


        if (sellerOrder.esopType == "NON_PERFORMANCE")
            platformFee = round(sellAmount * TWO_PERCENT).toLong()

        updateWalletBalances(sellAmount, platformFee, buyer, seller)


        seller.transferLockedESOPsTo(buyer, EsopTransferRequest(sellerOrder.esopType!!, currentTradeQuantity))

        val amountToBeReleased = (buyerOrder.getPrice() - sellerOrder.getPrice()) * (currentTradeQuantity)
        buyer.userWallet.moveMoneyFromLockedToFree(amountToBeReleased)

    }

    private fun updateWalletBalances(
        sellAmount: Long,
        platformFee: Long,
        buyer: User,
        seller: User
    ) {
        val adjustedSellAmount = sellAmount - platformFee
        addPlatformFee(platformFee)

        buyer.userWallet.removeMoneyFromLockedState(sellAmount)
        seller.userWallet.addMoneyToWallet(adjustedSellAmount)
    }


    fun placeOrder(order: Order): Map<String, String> {
        if (order.getType() == "BUY") {
            executeBuyOrder(order)
        } else {
            executeSellOrder(order)
        }
        return mapOf("message" to "Order placed successfully.")
    }

    private fun executeBuyOrder(buyOrder: Order) {
        orderRecords.addBuyOrder(buyOrder)
        var sellOrder = orderRecords.getSellOrder()
        if (sellOrder != null) {
            while (buyOrder.orderStatus != "COMPLETED" && sellOrder!!.getPrice() <= buyOrder.getPrice()) {
                performOrderMatching(sellOrder, buyOrder)
                sellOrder = orderRecords.getSellOrder()
                if (sellOrder == null)
                    break
            }
        }
    }

    private fun executeSellOrder(sellOrder: Order) {
        orderRecords.addSellOrder(sellOrder)
        var buyOrder = orderRecords.getBuyOrder()
        if (buyOrder != null) {
            while (sellOrder.orderStatus != "COMPLETED" && sellOrder.getPrice() <= buyOrder!!.getPrice()) {
                performOrderMatching(sellOrder, buyOrder)
                buyOrder = orderRecords.getBuyOrder()
                if (buyOrder == null)
                    break
            }
        }
    }

    private fun performOrderMatching(sellOrder: Order, buyOrder: Order) {
        val orderExecutionPrice = sellOrder.getPrice()
        val orderExecutionQuantity = min(sellOrder.remainingQuantity, buyOrder.remainingQuantity)

        buyOrder.subtractFromRemainingQuantity(orderExecutionQuantity)
        sellOrder.subtractFromRemainingQuantity(orderExecutionQuantity)

        buyOrder.updateStatus()
        sellOrder.updateStatus()

        createOrderFilledLogs(orderExecutionQuantity, orderExecutionPrice, sellOrder, buyOrder)

        updateOrderDetails(
            orderExecutionQuantity,
            sellOrder,
            buyOrder
        )

        if (buyOrder.orderStatus == "COMPLETED") {
            orderRecords.removeBuyOrder(buyOrder)
        }
        if (sellOrder.orderStatus == "COMPLETED") {
            orderRecords.removeSellOrder(sellOrder)
        }
    }

    private fun createOrderFilledLogs(
        orderExecutionQuantity: Long,
        orderExecutionPrice: Long,
        sellOrder: Order,
        buyOrder: Order
    ) {
        val buyOrderLog = OrderFilledLog(
            orderExecutionQuantity,
            orderExecutionPrice,
            null,
            sellOrder.getUserName(),
            null
        )
        val sellOrderLog = OrderFilledLog(
            orderExecutionQuantity,
            orderExecutionPrice,
            sellOrder.esopType,
            null,
            buyOrder.getUserName()
        )

        buyOrder.addOrderFilledLogs(buyOrderLog)
        sellOrder.addOrderFilledLogs(sellOrderLog)
    }

    fun orderHistory(userName: String): Any {
        val userErrors: List<String> = userService.checkIfUserExists(userName)
        if (userErrors.isNotEmpty())
            return mapOf("error" to userErrors)
        val orderDetails = userRecords.getUser(userName)!!.orderList
        val orderHistory = ArrayList<History>()

        for (orders in orderDetails) {
            orderHistory.add(
                History(
                    orders.orderID,
                    orders.getQuantity(),
                    orders.getType(),
                    orders.getPrice(),
                    orders.orderStatus,
                    orders.orderFilledLogs
                )
            )
        }
        return orderHistory
    }
}