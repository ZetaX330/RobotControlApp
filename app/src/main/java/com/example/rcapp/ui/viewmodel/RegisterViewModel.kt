package com.example.rcapp.ui.viewmodel

import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rcapp.model.User

class RegisterViewModel : ViewModel() {
    val name = MutableLiveData("")
    val phone = MutableLiveData("")
    val password = MutableLiveData("")
    val passwordConfirm = MutableLiveData("")
    val email = MutableLiveData("")
    private val _nameVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val nameVisibility: LiveData<Int> get() = _nameVisibility
    private val _nameValidity = MutableLiveData<Boolean>().apply { value = false }
    val nameValidity: LiveData<Boolean> get() = _nameValidity

    private val _phoneVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val phoneVisibility: LiveData<Int> get() = _phoneVisibility
    private val _phoneValidity = MutableLiveData<Boolean>().apply { value = false }
    val phoneValidity: LiveData<Boolean> get() = _phoneValidity

    private val _passwordVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val passwordVisibility: LiveData<Int> get() = _passwordVisibility
    private val _passwordValidity = MutableLiveData<Boolean>().apply { value = false }
    val passwordValidity: LiveData<Boolean> get() = _passwordValidity

    private val _passwordConfirmVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val passwordConfirmVisibility: LiveData<Int> get() = _passwordConfirmVisibility
    private val _passwordConfirmValidity = MutableLiveData<Boolean>().apply { value = false }
    val passwordConfirmValidity: LiveData<Boolean> get() = _passwordConfirmValidity

    private val _emailVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val emailVisibility: LiveData<Int> get() = _emailVisibility
    private val _emailValidity = MutableLiveData<Boolean>().apply { value = false }
    val emailValidity: LiveData<Boolean> get() = _emailValidity
    init {
        name.observeForever { nameString ->
            _nameVisibility.value = if (nameString.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            _nameValidity.value = User.isNameValid(nameString)
        }
        phone.observeForever { phoneNumber ->
            _phoneVisibility.value = if (phoneNumber.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            _phoneValidity.value = User.isPhoneValid(phoneNumber)
        }
        password.observeForever { passwordNumber ->
            _passwordVisibility.value = if (passwordNumber.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            _passwordValidity.value = User.isPasswordValid(passwordNumber)
        }
        passwordConfirm.observeForever { passwordConfirmNumber ->
            _phoneVisibility.value = if (passwordConfirmNumber.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            _phoneValidity.value = User.isPasswordValid(passwordConfirmNumber)
        }
        email.observeForever { emailString ->
            _emailVisibility.value = if (emailString.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            _emailValidity.value = User.isEmailValid(emailString)
        }
    }
}