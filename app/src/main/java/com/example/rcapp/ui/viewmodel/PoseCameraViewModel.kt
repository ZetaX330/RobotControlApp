package com.example.rcapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PoseCameraViewModel:ViewModel() {
    private val _isCameraOn = MutableLiveData<Boolean>()
    val isCameraOn: LiveData<Boolean> get() = _isCameraOn
    //设置相机开关状态
    fun setCameraStatus(cameraStatus: Boolean) {
        _isCameraOn.value = cameraStatus
    }
}