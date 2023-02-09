package com.esop.schema

data class MoneyTransferRequest(
    val amountToBeDebitedFromBuyersWallet: Long,
    val amountToBeCreditedToSellersAccount: Long
)