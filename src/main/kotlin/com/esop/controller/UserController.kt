package com.esop.controller

import com.esop.schema.User
import com.esop.service.check_email
import com.esop.service.check_phonenumber
import com.esop.service.check_username
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Post
import io.micronaut.json.tree.JsonObject
import org.json.JSONObject
import java.lang.Error


val all_usernames= mutableSetOf<String>()
val all_emails= mutableSetOf<String>()
val all_numbers= mutableSetOf<String>()


@Controller("/user")
class UserController {


    @Post(uri="/register", consumes = [MediaType.APPLICATION_JSON],produces=[MediaType.APPLICATION_JSON])
    open fun register(@Body response: JsonObject) {


        var v1=response.get("firstname").toString()
        var v2=response.get("lastName").toString()
        var v3=response.get("phoneNumber").toString()
        var v4=response.get("email").toString()
        var v5=response.get("username").toString()


        val x=check_username(all_usernames,v5)
        val y=check_email(all_emails,v4);
        val z=check_phonenumber(all_usernames,v3);

        if (x==true and y==true and z)
        {
            // constructor call to set properties of object
            val user= User(v1,v2,v3,v4,v5);

            // adding in different sets for checking unique feature
            all_emails.add(v4)
            all_numbers.add(v3)
            all_usernames.add(v5)
        }
        else
        {
            // error raise

        }




    }

    @Post(uri="/{userName}/order", consumes = [MediaType.APPLICATION_JSON],produces=[MediaType.APPLICATION_JSON])
    fun order(@Body body: JsonObject) {

    }

    @Get(uri = "/{userName}/accountInformation", produces = [MediaType.APPLICATION_JSON])
    fun getAccountInformation(userName: String): String {
        return "Some response"
    }

    @Post(uri = "{userName}/inventory", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun addInventory(userName: String): String {
        return "Some response"
    }

    @Post(uri = "{userName}/wallet", consumes = [MediaType.APPLICATION_JSON], produces = [MediaType.APPLICATION_JSON])
    fun addWallet(userName: String): String {
        return "Some response"
    }

    @Get(uri = "/{userName}/order", produces = [MediaType.APPLICATION_JSON])
    fun getOrder(userName: String): String {
        return "Some response"
    }
}