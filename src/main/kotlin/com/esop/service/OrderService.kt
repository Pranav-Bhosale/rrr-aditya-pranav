package com.esop.service


import com.esop.dto.CreateOrderDTO
import com.esop.repository.OrderRecords
import com.esop.repository.UserRecords
import com.esop.schema.*
import com.esop.schema.PlatformFee.Companion.addPlatformFee
import jakarta.inject.Singleton
import java.util.*
import kotlin.math.min

@Singleton
class OrderService(
    private val userService: UserService, private val userRecords: UserRecords, private val orderRecords: OrderRecords

) {

    fun validateOrderRequest(userName: String, orderRequest: CreateOrderDTO): MutableList<String> {
        val errorList = mutableListOf<String>()

        errorList.addAll(userService.checkIfUserExists(userName))
        if (errorList.isNotEmpty()) return errorList

        val user = userRecords.getUser(userName)!!
        if (orderRequest.type == "BUY") {
            errorList.addAll(validateBuyOrderRequest(user, orderRequest))
        } else if (orderRequest.type == "SELL") {
            errorList.addAll(validateSellOrderRequest(user, orderRequest))
        }

        return errorList
    }

    private fun validateSellOrderRequest(
        user: User, orderRequest: CreateOrderDTO
    ): List<String> {
        val errorList = mutableListOf<String>()

        errorList.addAll(checkForInsufficientInventory(user, orderRequest))

        val wallet = user.userWallet
        errorList.addAll(wallet.checkWalletOverflow(orderRequest.orderValue))

        return errorList
    }

    private fun validateBuyOrderRequest(
        user: User, orderRequest: CreateOrderDTO
    ): List<String> {
        val errorList = mutableListOf<String>()

        errorList.addAll(checkForInsufficientFunds(user, orderRequest))

        val inventory = user.getInventory("NON_PERFORMANCE")
        errorList.addAll(inventory.checkInventoryOverflow(orderRequest.quantity))

        return errorList
    }

    private fun checkForInsufficientInventory(
        user: User, orderRequest: CreateOrderDTO
    ): List<String> {
        if (!user.checkInventory(orderRequest)) {
            return listOf("Insufficient ${orderRequest.esopType!!.lowercase(Locale.getDefault())} inventory.")
        }
        return emptyList()
    }

    private fun checkForInsufficientFunds(user: User, orderRequest: CreateOrderDTO): List<String> {
        if (!user.checkBalance(orderRequest.orderValue)) {
            return listOf("Insufficient funds")
        }
        return emptyList()
    }

    fun placeOrder(userName: String, orderRequest: CreateOrderDTO): Order {
        val order = createOrder(orderRequest, userName)

        orderRecords.addOrder(order)
        userService.addOrderToUser(order)

        lockResourcesForOrder(order)

        return order
    }

    private fun lockResourcesForOrder(order: Order) {
        if (order.getType() == "BUY") {
            userService.lockWalletForUser(order.getUserName(), order.getOrderValue())
        } else {
            userService.lockInventoryForUser(order.getUserName(), order.esopType!!, order.getQuantity())
        }
    }

    private fun createOrder(orderRequest: CreateOrderDTO, userName: String): Order {
        val esopType: String? = if (orderRequest.type == "SELL") orderRequest.esopType!!
        else null

        return Order(
            orderRecords.generateOrderId(),
            orderRequest.quantity,
            orderRequest.type,
            orderRequest.price,
            userName,
            esopType
        )
    }

    private fun modifyOrderState(order: Order, orderExecutionDetails: OrderExecutionDetailsRequest) {
        order.subtractFromRemainingQuantity(orderExecutionDetails.orderExecutionQuantity)
        order.updateStatus()
    }

    fun executeOrder(currentOrder: Order): String {
        while (currentOrder.orderStatus != "COMPLETED") {
            val matchOrder = getBestMatchOrder(currentOrder) ?: break
            performOrderMatching(currentOrder, matchOrder)
        }
        return "Order placed successfully."
    }

    private fun getBestMatchOrder(order: Order): Order? {
        return if (order.getType() == "BUY") orderRecords.getMatchSellOrder(buyOrder = order)
        else orderRecords.getMatchBuyOrder(sellOrder = order)
    }

    private fun performOrderMatching(currentOrder: Order, matchOrder: Order) {
        val buyOrder = identifyBuyOrder(currentOrder, matchOrder)
        val sellOrder = identifySellOrder(currentOrder, matchOrder)

        val orderExecutionDetails = createOrderExecutionDetails(sellOrder, buyOrder)

        val platformFee = handlePlatformFee(sellOrder, orderExecutionDetails)

        handleResources(orderExecutionDetails, platformFee, buyOrder, sellOrder)

        updateOrderStatus(buyOrder, orderExecutionDetails, sellOrder)

        addOrderExecutionDetails(orderExecutionDetails, sellOrder, buyOrder)

        removeOrdersFromOrderRecordsIfFilled(buyOrder, sellOrder)
    }

    private fun removeOrdersFromOrderRecordsIfFilled(buyOrder: Order, sellOrder: Order) {
        orderRecords.removeOrderIfFilled(buyOrder)
        orderRecords.removeOrderIfFilled(sellOrder)
    }

    private fun updateOrderStatus(
        buyOrder: Order,
        orderExecutionDetails: OrderExecutionDetailsRequest,
        sellOrder: Order
    ) {
        modifyOrderState(buyOrder, orderExecutionDetails)
        modifyOrderState(sellOrder, orderExecutionDetails)
    }

    private fun handleResources(
        orderExecutionDetails: OrderExecutionDetailsRequest,
        platformFee: Long,
        buyOrder: Order,
        sellOrder: Order
    ) {
        transferResources(orderExecutionDetails, platformFee)
        releaseResources(buyOrder, sellOrder, orderExecutionDetails)
    }

    private fun handlePlatformFee(
        sellOrder: Order,
        orderExecutionDetails: OrderExecutionDetailsRequest
    ): Long {
        val platformFee = PlatformFee.calculatePlatformFee(
            sellOrder.esopType!!,
            orderExecutionDetails.totalOrderExecutionValue
        )

        addPlatformFee(platformFee)
        return platformFee
    }

    private fun transferResources(orderExecutionDetails: OrderExecutionDetailsRequest, platformFee: Long) {
        val moneyTransferRequest = MoneyTransferRequest(
            amountToBeDebitedFromBuyersWallet = orderExecutionDetails.totalOrderExecutionValue,
            amountToBeCreditedToSellersAccount = (orderExecutionDetails.totalOrderExecutionValue - platformFee)
        )
        userService.moveMoneyFromBuyerToSeller(
            orderExecutionDetails.buyerUserName,
            orderExecutionDetails.sellerUserName,
            moneyTransferRequest
        )

        val inventoryTransferRequest =
            InventoryTransferRequest(
                sellerInventoryType = orderExecutionDetails.esopType,
                transferQuantity = orderExecutionDetails.orderExecutionQuantity
            )
        userService.moveInventoryFromSellerToBuyer(
            orderExecutionDetails.sellerUserName, orderExecutionDetails.buyerUserName,
            inventoryTransferRequest
        )
    }

    private fun createOrderExecutionDetails(
        sellOrder: Order,
        buyOrder: Order
    ) = OrderExecutionDetailsRequest(
        min(sellOrder.remainingQuantity, buyOrder.remainingQuantity),
        sellOrder.getPrice(),
        buyOrder.getUserName(),
        sellOrder.getUserName(),
        sellOrder.esopType!!
    )

    private fun releaseResources(
        buyOrder: Order,
        sellOrder: Order,
        orderExecutionDetailsRequest: OrderExecutionDetailsRequest
    ) {
        val amountToBeReleased =
            (buyOrder.getPrice() - sellOrder.getPrice()) * orderExecutionDetailsRequest.orderExecutionQuantity
        userService.releaseLockedMoneyFromBuyer(buyOrder.getUserName(), amountToBeReleased)
    }

    private fun identifyBuyOrder(currentOrder: Order, matchOrder: Order): Order {
        if (currentOrder.getType() == "BUY") return currentOrder
        return matchOrder
    }

    private fun identifySellOrder(currentOrder: Order, matchOrder: Order): Order {
        if (currentOrder.getType() == "SELL") return currentOrder
        return matchOrder
    }

    private fun addOrderExecutionDetails(
        orderExecutionDetailsRequest: OrderExecutionDetailsRequest, sellOrder: Order, buyOrder: Order
    ) {
        val buyOrderLog = OrderFilledLog(
            orderExecutionDetailsRequest.orderExecutionQuantity,
            orderExecutionDetailsRequest.orderExecutionPrice,
            null,
            sellOrder.getUserName(),
            null
        )
        val sellOrderLog = OrderFilledLog(
            orderExecutionDetailsRequest.orderExecutionQuantity,
            orderExecutionDetailsRequest.orderExecutionPrice,
            sellOrder.esopType,
            null,
            buyOrder.getUserName()
        )

        buyOrder.addExecutionDetails(buyOrderLog)
        sellOrder.addExecutionDetails(sellOrderLog)
    }

    fun orderHistory(userName: String): Any {
        val userErrors: List<String> = userService.checkIfUserExists(userName)
        if (userErrors.isNotEmpty()) return mapOf("error" to userErrors)
        val orderDetails = userRecords.getUser(userName)!!.orderList
        val orderHistory = ArrayList<History>()

        for (orders in orderDetails) {
            orderHistory.add(
                History(
                    orders.getOrderId(),
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