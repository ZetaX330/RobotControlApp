package com.example.rcapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rcapp.model.User
import com.example.rcapp.data.repository.UserRepository

class RegisterViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _registerResult = MutableLiveData<Pair<Boolean, String>>()
    val registerResult: LiveData<Pair<Boolean, String>> = _registerResult

    fun register(
        user: User
    ) {
        userRepository.register(user) { success, message ->
            _registerResult.postValue(Pair(success, message))
        }
    }
}