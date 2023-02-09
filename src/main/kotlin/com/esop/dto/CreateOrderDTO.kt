package com.esop.dto

import com.esop.MAX_INVENTORY_CAPACITY
import com.esop.MAX_WALLET_CAPACITY
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.*


class CreateOrderDTO @JsonCreator constructor(

    @field:NotBlank(message = "Order Type can not be missing or empty.")
    @field:Pattern(regexp = "^((?i)BUY|(?i)SELL)$", message = "Invalid Type: should be one of BUY or SELL")
    val type: String,

    @field:NotNull(message = "Quantity can not be missing.")
    @field:Min(1, message = "Quantity has to be greater than zero")
    @field:Max(
        MAX_INVENTORY_CAPACITY,
        message = "quantity can't exceed maximum inventory capacity of $MAX_INVENTORY_CAPACITY"
    )
    val quantity: Long,

    @JsonProperty("price")
    @field:NotNull(message = "Price can not be missing.")
    @field:Min(1, message = "Price can not be less than zero")
    @field:Max(
        MAX_WALLET_CAPACITY,
        message = "amount can't exceed maximum wallet capacity of $MAX_WALLET_CAPACITY"
    )
    val price: Long,

    @JsonProperty("esopType")
    @field:Pattern(
        regexp = "^((?i)NON_PERFORMANCE|(?i)PERFORMANCE)$",
        message = "esopType should be one of NON_PERFORMANCE or PERFORMANCE"
    )
    val esopType: String? = "NON_PERFORMANCE",

    val orderValue: Long = quantity * price
)
