package com.miracle.lib_ble.callback

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/24/21
 * @desc:
 */
interface BleCallback {

    fun onScanResult(callbackType: Int, result: ScanResult?)

    fun onScanFailed(errorCode: Int)

    fun onScanTimeout(isEmpty: Boolean)

    fun onScanMatched(result: HashMap<String, ScanResult>)

    fun onScanMatched(result: ScanResult)

    fun onStartConnectGatt(device: BluetoothDevice)

    fun onBleConnected(gatt: BluetoothGatt)

    fun onBleDisconnected(gatt: BluetoothGatt)

    fun onBleServicesDiscovered(gatt: BluetoothGatt)

    fun onCharacteristicWrite(success: Boolean, value: ByteArray)

    fun onFailed(code: Int, msg: String?)

    fun onGetAESKey(byteArray: ByteArray)

    fun isDeviceInitial(init: Boolean)

    fun onOpenDoorSuccess(data: ByteArray)
}