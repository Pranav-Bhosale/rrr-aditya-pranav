package com.esop.service

import com.esop.schema.User
import io.micronaut.json.tree.JsonObject
import com.esop.constant.errors
import com.esop.constant.success_response
import jakarta.inject.Singleton

@Singleton
class UserService {
    val all_emails = mutableSetOf<String>()
    val all_numbers = mutableSetOf<String>()
    val all_usernames = mutableSetOf<String>()
    var all_users = HashMap<String, User>()

    fun check_inventory(quantity: Long, userName: String): Boolean{
        if(user_exists(userName) && all_users[userName]?.inventory?.free!! >= quantity){
            return true
        }
        return false
    }
    fun check_wallet(amount: Long, userName: String): Boolean{
        if(user_exists(userName) && all_users[userName]?.wallet?.free!! >= amount){
            return true
        }
        return false
    }
    fun orderCheckBeforePlace(userName: String, quantity: Long, type: String, price: Long): Map<String, MutableList<String>>{
        var userErrors = mutableListOf<String>()
        if(!all_users.containsKey(userName)){
            userErrors.add("User doesn't exist")
            return mapOf("errors" to userErrors)
        }
        if(type == "BUY"){
            if(!check_wallet(price*quantity, userName)){
                userErrors.add("Insufficient funds")
            }
        }
        else if(type == "SELL"){
            if(!check_inventory(quantity, userName)){
                userErrors.add("Insufficient inventory")
            }
        }
        if(userErrors.isEmpty()){
            if(type == "BUY"){
                all_users[userName]?.buyAndUpdateWallet(price*quantity)
            }
            if(type == "SELL"){
                all_users[userName]?.sellAndUpdateInventory(quantity)
            }
        }
        return mapOf("errors" to userErrors)
    }
    fun user_exists(userName: String): Boolean{
        return all_users.containsKey(userName)
    }
    fun check_username(username_set: MutableSet<String>, search_value: String): Boolean {
        return username_set.contains(search_value);
    }

    fun check_phonenumber(usernumber_set: MutableSet<String>, search_value: String): Boolean {
        return usernumber_set.contains(search_value);
    }

    fun check_email(useremail_set: MutableSet<String>, search_value: String): Boolean {
        return useremail_set.contains(search_value);
    }


    val PHONENUMBER_REGEX = "^(\\+91[\\-\\s]?)?[0]?(91)?[789]\\d{9}\$"
//var PATTERN: Pattern = Pattern.compile(REG)
//fun CharSequence.isPhoneNumber() : Boolean = PATTERN.matcher(this).find()

    val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})";

    fun isEmailValid(email: String): Boolean {
        return EMAIL_REGEX.toRegex().matches(email);
    }

    fun isPhoneNumber(pNumber: String): Boolean {
        return PHONENUMBER_REGEX.toRegex().matches(pNumber);
    }

    fun registerUser(userData: JsonObject): Map<String,Any> {
    var firstName = userData.get("firstName").stringValue
    var lastName = userData.get("lastName").stringValue
    var phoneNumber = userData.get("phoneNumber").stringValue
    var email = userData.get("email").stringValue
    var username = userData.get("username").stringValue


    if(check_username(all_usernames, username)){
        return mapOf("error" to errors["USERNAME_EXISTS"].toString())
    }
    else if(check_email(all_emails, email)){
        return mapOf("error" to errors["EMAIL_EXISTS"].toString())
    }
    else if(check_phonenumber(all_numbers, phoneNumber)){
        return mapOf("error" to errors["PHONENUMBER_EXISTS"].toString())
    }
//    else if(!isEmailValid(v4)){
//        return errors["INVALID_EMAIL"].toString()
//    }
//    else if(!isPhoneNumber(v3)){
//        return errors["INVALID_PHONENUMBER"].toString()
//    }
        else {
        val user = User(firstName, lastName, phoneNumber, email, username);

        all_users[username] = user
        all_emails.add(email)
        all_numbers.add(phoneNumber)
        all_usernames.add(username)


        val newUser = mapOf("firstName" to user.firstName.toString(), "lastName" to user.lastName.toString(), "phoneNumber" to user.phoneNumber.toString(), "email" to user.email.toString(), "userName" to user.username.toString() )

        return mapOf("user" to newUser, "message" to success_response["USER_CREATED"].toString())
        }
}
    fun accountInformation(userName: String): Any {
        val user = all_users[userName.toString()]

        var accountErrors = mutableListOf<String>()

        var userData= mapOf("firstName" to user?.firstName.toString() ,"lastName" to user?.lastName.toString(), "phoneNumber" to user?.phoneNumber.toString(), "email" to user?.email.toString(), "wallet" to user?.wallet, "inventory" to user?.inventory)
        if(user!=null){
            //val newUser = "{\"firstName\": ${user?.firstName.toString()}, \"lastName\": ${user?.lastName}, \"phoneNumber\": ${user?.phoneNumber}, \"email\": ${user?.email}, \"username\": ${user?.username}"
            //println(newUser)
            return userData
        }

        accountErrors.add("USER_DOES_NOT_EXISTS")

        return mapOf("errors" to accountErrors)
    }


    fun adding_inventory(body: JsonObject, userName: String): Any
    {
        var quant=body.get("quantity").longValue

        var accountErrors =mutableListOf<String>()

        var usr1= all_users[userName]

        if (usr1 != null) {
            usr1.addInventory(quant)
            return mapOf("message" to "${quant} ESOPS added to inventory")
        }
        accountErrors.add("USER_DOES_NOT_EXIST")
        return mapOf("errors" to accountErrors)
    }

    fun adding_Money(body: JsonObject, userName: String): Any
    {
        var amt=body.get("amount").longValue
        var accountErrors =mutableListOf<String>()
        var usr1= all_users[userName]

        if (usr1 != null) {
            usr1.addWallet(amt)
            return mapOf("message" to "${amt} amount added to account");
        }
        accountErrors.add("USER_DOES_NOT_EXIST")
        return mapOf("errors" to accountErrors)

        //return mapOf("errors" to errors["USER_DOES_NOT_EXISTS"]).toString()
    }
}