package com.rakshax.app.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.rakshax.app.data.model.ConnectionStatus
import com.rakshax.app.data.model.DeviceState
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleRepository(
    context: Context,
    private val notifyBackgroundService: Boolean = true,
    private val onSosSignal: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _scanResults = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var sosCharacteristic: BluetoothGattCharacteristic? = null
    private var selectedAddress: String? = null
    private var shouldReconnect = false
    private var scanJob: kotlinx.coroutines.Job? = null

    val scanResults: StateFlow<List<ScannedBleDevice>> = _scanResults.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startScan() {
        if (!hasPermissions()) {
            setError("Bluetooth scan permission is required")
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            setError("This phone does not support Bluetooth LE")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            setError("Turn on Bluetooth to find the RakshaX button")
            return
        }

        stopScan()
        _scanResults.value = emptyList()
        _errorMessage.value = null
        MockDataRepository.updateDeviceState(
            MockDataRepository.deviceState.value.copy(status = ConnectionStatus.SCANNING, lastError = null)
        )

        scanner = bluetoothAdapter.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(BleProtocol.SERVICE_UUID)))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching {
            scanner?.startScan(listOf(filter), settings, scanCallback)
        }.onFailure {
            setError("Unable to start Bluetooth scan")
            updateDisconnected()
        }

        scanJob = scope.launch {
            delay(SCAN_DURATION_MS)
            stopScan()
            if (MockDataRepository.deviceState.value.status == ConnectionStatus.SCANNING) {
                updateDisconnected("No RakshaX button found nearby")
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (hasPermissions()) {
            runCatching { scanner?.stopScan(scanCallback) }
        }
        scanner = null
    }

    fun connect(address: String) {
        if (!hasPermissions()) {
            setError("Bluetooth connect permission is required")
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            setError("Turn on Bluetooth before connecting")
            return
        }

        stopScan()
        selectedAddress = address
        BleServicePreferences.saveDeviceAddress(appContext, address)
        if (notifyBackgroundService) {
            com.rakshax.app.service.BleSosListenerService.refreshDevice(appContext)
        }
        shouldReconnect = true
        updateConnecting(address)
        runCatching {
            gatt?.close()
            gatt = bluetoothAdapter.getRemoteDevice(address).connectGatt(
                appContext,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }.onFailure {
            updateDisconnected("Could not connect to the selected button")
        }
    }

    fun disconnect() {
        shouldReconnect = false
        selectedAddress = null
        BleServicePreferences.clearDeviceAddress(appContext)
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        sosCharacteristic = null
        updateDisconnected()
    }

    fun close() {
        shouldReconnect = false
        stopScan()
        gatt?.close()
        gatt = null
        scope.cancel()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = result.scanRecord?.deviceName ?: device.name ?: "Unnamed BLE device"
            if (!name.startsWith(BleProtocol.DEVICE_NAME_PREFIX, ignoreCase = true)) return
            _scanResults.value = (_scanResults.value + ScannedBleDevice(
                name = name,
                address = device.address,
                rssi = result.rssi
            )).distinctBy { it.address }
        }

        override fun onScanFailed(errorCode: Int) {
            setError("Bluetooth scan failed (code $errorCode)")
            updateDisconnected()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                updateConnecting(gatt.device.address)
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                sosCharacteristic = null
                gatt.close()
                this@BleRepository.gatt = null
                updateDisconnected(if (status == BluetoothGatt.GATT_SUCCESS) null else "BLE connection lost")
                if (shouldReconnect && selectedAddress != null) {
                    scope.launch {
                        delay(RECONNECT_DELAY_MS)
                        selectedAddress?.let(::connect)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                setError("RakshaX GATT services were not discovered")
                return
            }
            val service = gatt.getService(UUID.fromString(BleProtocol.SERVICE_UUID))
            val characteristic = service?.getCharacteristic(UUID.fromString(BleProtocol.SOS_CHARACTERISTIC_UUID))
            if (characteristic == null) {
                setError("Selected device is not a RakshaX SOS button")
                return
            }
            sosCharacteristic = characteristic
            gatt.setCharacteristicNotification(characteristic, true)
            characteristic.getDescriptor(CLIENT_CONFIG_UUID)?.let { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
            updateConnected(gatt.device)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            handleCharacteristicValue(characteristic, characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicValue(characteristic, value)
        }
    }

    private fun handleCharacteristicValue(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (characteristic.uuid == UUID.fromString(BleProtocol.SOS_CHARACTERISTIC_UUID) &&
            BleProtocol.isSosSignal(value)
        ) {
            onSosSignal("ESP32-C3 GPIO4 long press")
        }
    }

    private fun updateConnecting(address: String) {
        MockDataRepository.updateDeviceState(
            MockDataRepository.deviceState.value.copy(
                macAddress = address,
                status = ConnectionStatus.CONNECTING,
                lastError = null
            )
        )
    }

    private fun updateConnected(device: BluetoothDevice) {
        val name = runCatching { device.name }.getOrNull() ?: BleProtocol.DEVICE_NAME_PREFIX
        MockDataRepository.updateDeviceState(
            MockDataRepository.deviceState.value.copy(
                name = name,
                macAddress = device.address,
                status = ConnectionStatus.CONNECTED,
                batteryLevel = MockDataRepository.deviceState.value.batteryLevel.coerceAtLeast(1),
                lastError = null
            )
        )
    }

    private fun updateDisconnected(message: String? = null) {
        MockDataRepository.updateDeviceState(
            MockDataRepository.deviceState.value.copy(
                status = ConnectionStatus.DISCONNECTED,
                lastError = message
            )
        )
        if (message != null) setError(message)
    }

    private fun setError(message: String) {
        _errorMessage.value = message
        MockDataRepository.updateDeviceState(
            MockDataRepository.deviceState.value.copy(lastError = message)
        )
    }

    private companion object {
        const val SCAN_DURATION_MS = 10_000L
        const val RECONNECT_DELAY_MS = 2_000L
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
