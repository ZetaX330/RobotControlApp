package com.example.rcapp.ui.viewmodel

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PoseCameraViewModel:ViewModel(){

    private val _isCameraOn = MutableLiveData<Boolean>()
    val isCameraOn: LiveData<Boolean> get() = _isCameraOn
    private val _isCameraFacingBack = MutableLiveData<Boolean>()
    val isCameraFacingBack: LiveData<Boolean> get() = _isCameraFacingBack

    private val _cameraTimeVisibility = MutableLiveData<Int>().apply { value = View.INVISIBLE }
    val cameraTimeVisibility: LiveData<Int> get() = _cameraTimeVisibility
    init {
        isCameraOn.observeForever { isOn ->
            _cameraTimeVisibility.value = if (isOn) View.VISIBLE else View.INVISIBLE
        }
    }
    //设置相机开关状态
    fun setCameraStatus(cameraStatus: Boolean) {
        _isCameraOn.value = cameraStatus
    }
    fun setCameraSelector(cameraSelector: Boolean) {
        _isCameraFacingBack.value = cameraSelector
    }
}