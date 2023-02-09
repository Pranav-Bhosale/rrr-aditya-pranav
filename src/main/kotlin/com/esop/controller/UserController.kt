package com.esop.controller


import com.esop.HttpException
import com.esop.dto.AddInventoryDTO
import com.esop.dto.AddWalletDTO
import com.esop.dto.CreateOrderDTO
import com.esop.dto.UserCreationDTO
import com.esop.service.*
import com.fasterxml.jackson.core.JsonProcessingException
import io.micronaut.core.convert.exceptions.ConversionErrorException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.validation.Validated
import io.micronaut.web.router.exceptions.UnsatisfiedBodyRouteException
import jakarta.inject.Inject
import javax.validation.ConstraintViolationException
import javax.validation.Valid


@Validated
@Controller("/user")
class UserController {

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var orderService: OrderService

    @Error(exception = HttpException::class)
    fun onHttpException(exception: HttpException): HttpResponse<*> {
        return HttpResponse.status<Map<String, ArrayList<String>>>(exception.status)
            .body(mapOf("errors" to arrayListOf(exception.message)))
    }

    @Error(exception = JsonProcessingException::class)
    fun onJSONProcessingExceptionError(ex: JsonProcessingException): HttpResponse<Map<String, ArrayList<String>>> {
        return HttpResponse.badRequest(mapOf("errors" to arrayListOf("Invalid JSON format")))
    }

    @Error(exception = UnsatisfiedBodyRouteException::class)
    fun onUnsatisfiedBodyRouteException(
        request: HttpRequest<*>,
        ex: UnsatisfiedBodyRouteException
    ): HttpResponse<Map<String, List<*>>> {
        return HttpResponse.badRequest(mapOf("errors" to arrayListOf("Request body missing")))
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    fun onRouteNotFound(): HttpResponse<Map<String, List<*>>> {
        return HttpResponse.notFound(mapOf("errors" to arrayListOf("Route not found")))
    }

    @Error(exception = ConversionErrorException::class)
    fun onConversionErrorException(ex: ConversionErrorException): HttpResponse<Map<String, List<*>>> {
        return HttpResponse.badRequest(mapOf("errors" to arrayListOf(ex.message)))
    }

    @Error(exception = ConstraintViolationException::class)
    fun onConstraintViolationException(ex: ConstraintViolationException): HttpResponse<Map<String, List<*>>> {
        return HttpResponse.badRequest(mapOf("errors" to ex.constraintViolations.map { it.message }))
    }

    @Error(exception = RuntimeException::class)
    fun onRuntimeError(ex: RuntimeException): HttpResponse<Map<String, List<*>>> {
        return HttpResponse.serverError(mapOf("errors" to arrayListOf(ex.message)))
    }


    @Post(uri = "/register", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun register(@Body @Valid userData: UserCreationDTO): HttpResponse<*> {
        val newUser = this.userService.registerUser(userData)
        if (newUser["error"] != null) {
            return HttpResponse.badRequest(newUser)
        }
        return HttpResponse.ok(newUser)
    }

    @Post(uri = "/{userName}/order", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun order(userName: String, @Body @Valid orderData: CreateOrderDTO): Any? {
        val errorList = orderService.validateOrderRequest(userName, orderData)
        if (errorList.isNotEmpty())
            return HttpResponse.badRequest(mapOf("errors" to errorList))

        val order = orderService.createOrder(userName, orderData)

        return orderService.executeOrder(order)
    }

    @Get(uri = "/{userName}/accountInformation", produces = [MediaType.APPLICATION_JSON])
    fun getAccountInformation(userName: String): HttpResponse<*> {
        val userData = this.userService.accountInformation(userName)

        if (userData["error"] != null) {
            return HttpResponse.badRequest(userData)
        }

        return HttpResponse.ok(userData)
    }

    @Post(
        uri = "{userName}/inventory",
        consumes = [MediaType.APPLICATION_JSON],
        produces = [MediaType.APPLICATION_JSON]
    )
    fun addInventory(userName: String, @Body @Valid inventoryRequest: AddInventoryDTO): HttpResponse<*> {
        val errorList = userService.validateInventoryRequest(userName, inventoryRequest)
        if (errorList.isNotEmpty())
            return HttpResponse.badRequest(mapOf("errors" to errorList))

        val response = userService.addingInventoryToUser(userName, inventoryRequest)

        return HttpResponse.ok(mapOf("message" to response))
    }


    @Post(uri = "{userName}/wallet", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun addWallet(userName: String, @Body @Valid walletRequest: AddWalletDTO): HttpResponse<*> {
        val errorList = userService.validateWalletRequest(userName, walletRequest)
        if (errorList.isNotEmpty())
            return HttpResponse.badRequest(mapOf("errors" to errorList))

        val response = userService.addMoneyToUser(userName, walletRequest.price!!)

        return HttpResponse.ok(mapOf("message" to response))
    }

    @Get(uri = "/{userName}/orderHistory", produces = [MediaType.APPLICATION_JSON])
    fun orderHistory(userName: String): HttpResponse<*> {
        val orderHistoryData = orderService.orderHistory(userName)
        if (orderHistoryData is Map<*, *>) {
            return HttpResponse.badRequest(orderHistoryData)
        }
        return HttpResponse.ok(orderHistoryData)
    }
}