package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private companion object {
        const val REQUEST_CODE_BLUETOOTH_PERMISSIONS = 1
        const val SCAN_PERIOD: Long = 5000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothLEScannerScreen()
        }
    }

    @Composable
    fun BluetoothLEScannerScreen() {
        val context = LocalContext.current
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        val permissionsGranted = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (permissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }
            ) {
                permissionsGranted.value = true
            } else {
                ActivityCompat.requestPermissions(
                    context as ComponentActivity,
                    permissions,
                    REQUEST_CODE_BLUETOOTH_PERMISSIONS
                )
            }
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            permissionsGranted.value = granted.all { it.value }
        }

        LaunchedEffect(permissions) {
            launcher.launch(permissions)
        }

        if (permissionsGranted.value) {
            BluetoothScanner()
        } else {
            Text("Permissions not granted")
        }
    }

    @Composable
    fun BluetoothScanner() {
        val context = LocalContext.current
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val scanResults = remember { mutableStateListOf<ScanResult>() }
        val isScanning = remember { mutableStateOf(false) }

        // Set the Bluetooth adapter name
        setBluetoothAdapterName(bluetoothAdapter, "MyEmulator")

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!scanResults.contains(result)) {
                    scanResults.add(result)
                    val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        result.isConnectable
                    } else {
                        true // Assume true for older versions
                    }
                    Log.d("DBG", "Device address: ${result.device.address} ($isConnectable)")
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = {
                    if (isScanning.value) {
                        try {
                            bluetoothLeScanner.stopScan(scanCallback)
                        } catch (e: SecurityException) {
                            Log.e("DBG", "SecurityException stopping scan", e)
                        }
                        isScanning.value = false
                    } else {
                        try {
                            bluetoothLeScanner.startScan(scanCallback)
                        } catch (e: SecurityException) {
                            Log.e("DBG", "SecurityException starting scan", e)
                        }
                        isScanning.value = true
                    }
                }
            ) {
                Text(if (isScanning.value) "Stop Scanning" else "Start Scanning")
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(scanResults) { result ->
                    val deviceName = if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        result.device.name ?: "Unknown"
                    } else {
                        "Permission required"
                    }
                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && result.isConnectable) Color.Black else Color.Gray
                    Text(text = "Device: $deviceName - MAC: ${result.device.address} - RSSI: ${result.rssi}", color = color)
                }
            }

            val bluetoothName = remember { bluetoothAdapter.name ?: "Unknown" }
            Text(text = "BLE: $bluetoothName")
        }
    }

    private fun setBluetoothAdapterName(bluetoothAdapter: BluetoothAdapter, name: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (bluetoothAdapter.name != name) {
            bluetoothAdapter.name = name
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            setContent {
                BluetoothLEScannerScreen()
            }
        }
    }
}