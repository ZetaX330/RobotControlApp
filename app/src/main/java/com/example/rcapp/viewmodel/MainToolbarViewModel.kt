package com.example.rcapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object MainToolbarViewModel {
    private val _toolbarState = MutableLiveData(ToolbarState())
    val toolbarState: LiveData<ToolbarState> = _toolbarState

    fun setBluetoothStatus(deviceName: String?, status: Int) {
        _toolbarState.value = deviceName?.let {
            _toolbarState.value!!.copy(
                deviceName = it,
                bluetoothStatus = when (status) {
                    0 -> BluetoothStatus.CLOSED
                    1 -> BluetoothStatus.DISCONNECTED
                    2 -> BluetoothStatus.CONNECTED
                    3 -> BluetoothStatus.LOADING
                    else -> BluetoothStatus.DISCONNECTED
                },
                isLoading = when (status) {
                    3 -> true
                    else -> false
                }
            )
        }
    }
    data class ToolbarState(
        val deviceName: String = "",
        val bluetoothStatus: BluetoothStatus = BluetoothStatus.DISCONNECTED,
        val isLoading: Boolean = false
    )

    enum class BluetoothStatus {
        CLOSED, DISCONNECTED, CONNECTED, LOADING
    }
}