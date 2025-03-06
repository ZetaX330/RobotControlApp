package com.example.rcapp.model
import android.util.Log
import com.example.rcapp.BR

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import java.math.BigDecimal

data class LoginResponse(
    val message: String,
    val user: User
)
data class User(
    val id: Int = -1,
    val name: String = "",
    var phone: String = "",
    var password: String = "",
    val email: String = "",
    val avatarBase64: String? = null,
    val balance: BigDecimal
) {
    companion object {
        fun isUserValid(user: User):Boolean{

            return isNameValid(user.name)
                    &&isPhoneValid(user.phone)
                    &&isPasswordValid(user.password)
                    &&isEmailValid(user.email)
        }

        fun isNameValid(name: String): Boolean {
            return name.isNotBlank() && name.length < 18 && !name.contains(" ")
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
            return email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }
}

