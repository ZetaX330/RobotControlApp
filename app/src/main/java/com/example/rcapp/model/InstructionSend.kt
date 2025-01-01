package com.example.rcapp.model

import com.example.rcapp.viewmodel.MainToolbarViewModel.BluetoothStatus
import java.time.LocalTime
data class InstructionSend(
    @JvmField val time: String,
    @JvmField var instruction: String
)


