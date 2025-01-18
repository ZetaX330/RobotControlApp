package com.example.rcapp.model
import com.example.rcapp.BR

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
data class User(
    val id: Int? = 0,
    val name: String = "",
    var phone: String = "",
    var password: String = "",
    val email: String = ""
) {
    companion object {
        fun isNameValid(phone: String): Boolean {
            return phone.length <= 11
        }
        fun isPhoneValid(phone: String): Boolean {
            return phone.length == 11
        }
        /**
         * (?=.*[A-Za-z]): 至少包含一个字母。
         * (?=.*\\d): 至少包含一个数字。
         * [A-Za-z\\d]{9,}: 总长度至少为 9
         */
        fun isPasswordValid(password: String): Boolean {
            val passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{9,}$"
            return password.matches(Regex(passwordPattern))
        }
        fun isEmailValid(email: String): Boolean {
            return email.let { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }
        }
    }
}

