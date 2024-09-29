package com.example.blescanner

data class BluetoothDeviceData(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean
)