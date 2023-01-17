package com.esop.controller

import com.esop.schema.User
import com.esop.service.*
import io.micronaut.http.HttpResponse
import com.esop.service.*
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Post
import io.micronaut.json.tree.JsonObject
import jakarta.inject.Inject
import org.json.JSONObject
import java.lang.Error


@Controller("/user")
class UserController {

    @Inject
    lateinit var orderService: OrderService

    @Inject
    lateinit var userService: UserService

    @Post(uri="/register", consumes = [MediaType.APPLICATION_JSON],produces=[MediaType.APPLICATION_JSON])
    open fun register(@Body response: JsonObject): HttpResponse<*> {
        val newUser = this.userService.registerUser(response)
        return HttpResponse.ok(newUser)
    }

    @Post(uri="/{userName}/order", consumes = [MediaType.APPLICATION_JSON],produces=[MediaType.APPLICATION_JSON])

    fun order(userName: String, @Body body: JsonObject){

        this.orderService.placeOrder(body, userName)
    }

    @Get(uri = "/{userName}/accountInformation", produces = [MediaType.APPLICATION_JSON])
    fun getAccountInformation(userName: String): HttpResponse<*> {
        val userData = this.userService.accountInformation(userName)
        return HttpResponse.ok(userData)
    }

    @Post(uri = "{userName}/inventory", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun addInventory(userName: String, @Body body: JsonObject){
        return this.userService.adding_inventory(body,userName)
    }

    @Post(uri = "{userName}/wallet", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun addWallet(userName: String, @Body body: JsonObject) {
        return this.userService.adding_Money(body,userName)
    }

    @Get(uri = "/{userName}/order", produces = [MediaType.APPLICATION_JSON])
    fun getOrder(userName: String): String {
        return "Some response"
    }
}