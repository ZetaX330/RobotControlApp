package com.example.rcapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object MainToolbarViewModel {
    private val _toolbarState = MutableLiveData(ToolbarState())
    val toolbarState: LiveData<ToolbarState> = _toolbarState

    /**
     * 暂时只看这个方法，其他地方调用setBluetoothStatus，更新ViewModel中的deviceName和status
     * ble基类Activity观察LiveData，从而更新UI
     */
    fun setBluetoothStatus(deviceName: String?, status: Int) {
        _toolbarState.value = _toolbarState.value?.copy(
            deviceName = deviceName ?: "",
            bluetoothStatus = when (status) {
                0 -> BluetoothStatus.CLOSED
                1 -> BluetoothStatus.DISCONNECTED
                2 -> BluetoothStatus.CONNECTED
                3 -> BluetoothStatus.LOADING
                else -> BluetoothStatus.DISCONNECTED
            },
            isLoading = status == 3
        )
    }

    data class ToolbarState(
        val deviceName: String = "",
        val bluetoothStatus: BluetoothStatus = BluetoothStatus.DISCONNECTED,
        val isLoading: Boolean = false,
        val isDrop:Boolean = false
    )

    enum class BluetoothStatus {
        CLOSED, DISCONNECTED, CONNECTED, LOADING
    }
}