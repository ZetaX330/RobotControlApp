package com.example.rcapp.ui.viewmodel

import com.example.rcapp.data.repository.UserRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<Pair<Boolean, String>>()
    val loginResult: LiveData<Pair<Boolean, String>> = _loginResult

//    private val _user = MutableLiveData<UserModel?>()
//    val user: LiveData<UserModel?> = _user

    fun login(username: String, password: String) {
        userRepository.login(username, password) { success, message ->
            _loginResult.postValue(Pair(success, message))
        }
    }
}