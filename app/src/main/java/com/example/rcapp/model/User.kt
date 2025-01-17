package com.example.rcapp.model
data class User(
    val id: Int?=null,
    val name: String?=null,
    val account:String,
    var password:String,
    val phone:String,
    val gender:Int,
    val email: String? = null,
){
    // Validate email format
    fun validateEmail(): Boolean? {
        return email?.let { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }
    }

    // Validate phone format
    fun validatePhone(): Boolean {
        return android.util.Patterns.PHONE.matcher(phone).matches()
    }

    // Encrypt password (simple example)
    fun encryptPassword() {
//        password = android.util.Base64.encodeToString(password.toByteArray(), android.util.Base64.DEFAULT)
    }

    // Decrypt password (simple example)
    fun decryptPassword() {
//        password = String(android.util.Base64.decode(password, android.util.Base64.DEFAULT))
    }

}
