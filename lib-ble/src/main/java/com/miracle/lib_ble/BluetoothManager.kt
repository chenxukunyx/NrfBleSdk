package com.miracle.lib_ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.miracle.lib_ble.callback.BleCallback
import com.miracle.lib_ble.internal.BluetoothInternal
import com.miracle.lib_ble.utils.BleLog

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/24/21
 * @desc:
 */
object BluetoothManager {

    private val bleInternal by lazy {
        BluetoothInternal()
    }

    fun init(context: Context) {
        bleInternal.init(context)
    }

    fun setDebug(debug: Boolean) {
        BleLog.isDebug = debug
    }

    fun startScan() {
        bleInternal.startScan()
    }

    /**
     * 蓝牙是否可用
     */
    fun isBluetoothEnable() = bleInternal.isBluetoothEnable()

    /**
     * 注册ble扫描连接过程监听
     */
    fun registerBleCallback(callback: BleCallback) {
        bleInternal.registerBleCallback(callback)
    }

    fun unRegisterBleCallback() {
        bleInternal.unRegisterBleCallback()
    }

    fun initialDevice(mac: String) {
        assertDeviceNull(mac)
        bleInternal.initialDevice(bleInternal.getDeviceMap()[mac]!!)
    }

    fun getOtaInfo(mac: String, aes: String) {
        assertDeviceNull(mac)
        bleInternal.getOTAInfo(bleInternal.getDeviceMap()[mac]!!, aes)
    }


    fun openDoor(mac: String, password: String, aes: String) {
        assertDeviceNull(mac)
        bleInternal.openDoor(bleInternal.getDeviceMap()[mac]!!, password, aes)
    }

    fun updatePassword(mac: String, oldPwd: String, newPwd: String, aes: String) {
        assertDeviceNull(mac)
        bleInternal.updatePassword(bleInternal.getDeviceMap()[mac]!!, oldPwd, newPwd, aes)
    }

    private fun assertDeviceNull(mac: String) {
        bleInternal.getDeviceMap()[mac] ?: throw NullPointerException("bluetooth device is null")
    }

    fun release() {
        unRegisterBleCallback()
        bleInternal.release()
    }
}