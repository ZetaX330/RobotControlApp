package com.example.rcapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PoseCameraViewModel:ViewModel() {
    private val _isCameraOn = MutableLiveData<Boolean>()
    val isCameraOn: LiveData<Boolean> get() = _isCameraOn
    fun setCameraStatus(cameraStatus: Boolean) {
        _isCameraOn.value = cameraStatus
    }
}