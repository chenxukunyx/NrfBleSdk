package com.miracle.lib_ble.internal

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.CountDownTimer
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import com.miracle.lib_ble.callback.BleCallback
import com.miracle.lib_ble.constants.*
import com.miracle.lib_ble.utils.BleLog
import com.miracle.lib_ble.utils.BleUtil
import com.miracle.lib_ble.utils.DataUtil
import com.miracle.lib_ble.utils.ThreadUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/24/21
 * @desc:
 */
internal class BluetoothInternal {
    private val TAG = "BluetoothInternal"

    private val STATE_NEED_RETRY = 1
    private val STATE_NOT_NEED_RETRY = 2
    private val DEFAULT_RETRY_COUNT = 5

    private val BLUETOOTH_SCAN_TIMEOUT = 16 * 1000L

    private var bleCallback: BleCallback? = null
    private var remoteMac: String? = null
    private var context: Context? = null
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var isDeviceInitial = false
    private var needInitDevice = false
    private var onlyScan = false
    private var state = STATE_NEED_RETRY
    private var retryCount = 0
    private val devicesMap = HashMap<String, ScanResult>()

    private var bluetoothGattService: BluetoothGattService? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val packetList = ArrayList<ByteArray>()

    private var currentOperation = -1
    private var password = ""
    private var oldPwd = ""
    private var aesKey: ByteArray? = null

    private fun sleep(time: Long = 20) {
        SystemClock.sleep(time)
    }

    private val timeoutTimer = object : CountDownTimer(BLUETOOTH_SCAN_TIMEOUT, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            BleLog.i(
                TAG,
                "ble scan Timer: $millisUntilFinished"
            )
        }

        override fun onFinish() {
            BleLog.i(TAG, "ble scan Timer: finished")
            stopScan()
            bleCallback?.onScanTimeout(devicesMap.isEmpty())
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            bleCallback?.onScanResult(callbackType, result)
            scanResult(result)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            bleCallback?.onScanFailed(errorCode)
        }
    }

    /**
     * 解析维度为单一设备，匹配mac地址
     */
    private fun parseDevice(identifier: String, result: ScanResult): Boolean {
        val bytes = result.scanRecord?.bytes
        if (bytes == null) {
            BleLog.w(TAG, "scanRecord.bytes is null")
            return false
        }
        BleLog.i(TAG, "scanResult: ${result.device.name}, ${DataUtil.bytes2Hex(bytes)}")
        //是否是设备端发出的广播包
        if (bytes[5] == 0x10.toByte() &&
            bytes[6] == 0x19.toByte() &&
            bytes[7] == 0x0f.toByte() &&
            bytes[8] == 0x18.toByte()
        ) {
            //解析mac地址
            val ret = DataUtil.bytes2Hex(bytes, 23, 6)
            BleLog.i(TAG, "parseDevice---> find a device: ${result.device.name}, mac: $ret")
            return identifier == ret
        }
        return false
    }

    /**
     * 解析维度为系列产品
     */
    private fun parseDevice(result: ScanResult): Boolean {
        val bytes = result.scanRecord?.bytes
        if (bytes == null) {
            BleLog.w(TAG, "scanRecord.bytes is null")
            return false
        }
        BleLog.i(TAG, "scanResult: ${result.device.name}, ${DataUtil.bytes2Hex(bytes)}")
        //是否是设备端发出的广播包
        if (bytes[5] == 0x10.toByte() &&
            bytes[6] == 0x19.toByte() &&
            bytes[7] == 0x0f.toByte() &&
            bytes[8] == 0x18.toByte()
        ) {
            return true
        }
        return false
    }

    /**
     * 解析扫描结果
     */
    private fun scanResult(result: ScanResult?) {
        if (result == null) return
        if (onlyScan) {
            val isMatch = parseDevice(result)
            if (isMatch) {
                devicesMap[result.device.address] = result
                bleCallback?.onScanMatched(devicesMap)
            }
        } else {
            val isMatch = parseDevice(remoteMac!!, result)
            if (isMatch) {
                matched(result)
                bleCallback?.onScanMatched(result)
            }
        }
    }

    /**
     * 找到匹配的设备
     */
    private fun matched(result: ScanResult) {
        stopScan()
        BleLog.i(TAG, "find a matched device: ${result.device.name}, ${result.device.address}")
        val broadcast = result.scanRecord!!.bytes
        BleLog.i(TAG, "broadcast: ${DataUtil.bytes2Hex(broadcast)}")
        isDeviceInitial = (broadcast[19] and 4.toByte()) != 4.toByte()
        if (!isDeviceInitial) {
            BleLog.i(TAG, "device not initial")
            if (!needInitDevice) {
                bleCallback?.isDeviceInitial(false)
                bleCallback?.onFailed(0x10001, UKLockError.getErrorMessage(0x10001))
                timeoutTimer.cancel()
                return
            }
        }
        connectGatt(result.device)
        timeoutTimer.cancel()
    }

    /**
     * 建立连接
     */
    private fun connectGatt(device: BluetoothDevice) {
        sleep()
        bleCallback?.onStartConnectGatt(device)
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                BleLog.i(TAG, "onConnectionStateChange: " + gatt!!.device.name + ", " + newState)
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        devicesMap.clear()
                        state = STATE_NOT_NEED_RETRY
                        retryCount = 0
                        bleCallback?.onBleConnected(gatt)
                        bluetoothGatt!!.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        internalRelease()
                        if (state == STATE_NEED_RETRY ) {
                            if (retryCount < DEFAULT_RETRY_COUNT) {
                                BleLog.i("连接断开，开始重试")
                                sleep()
//                                startScan(remoteMac)
                                connectGatt(device)
                                retryCount += 1
                            }
                        } else {
                            bleCallback?.onBleDisconnected(gatt)
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLog.i(TAG, "onServicesDiscovered")
                    bluetoothGattService =
                        gatt!!.getService(UUID.fromString(UUIDHelper.SERVICE_UUID))
                    BleLog.i("bluetoothGattService: $bluetoothGattService")
                    bleCallback?.onBleServicesDiscovered(gatt!!)
                    val characteristic =
                        bluetoothGattService!!.getCharacteristic(UUID.fromString(UUIDHelper.NOTIFY_UUID))
                    val enableNotification = BleUtil.enableNotification(gatt, true, characteristic)
                    BleLog.i(TAG, "enableNotification: $enableNotification")
                    BleLog.i("currentOperation: $currentOperation, currentThread: ${Thread.currentThread().name}")
                    when (currentOperation) {
                        OPERATION_DEVICE_INITIAL -> {
                            sendInitDeviceCmd()
                        }
                        OPERATION_OPEN_DOOR, OPERATION_UPDATE_PASSWORD -> {
                            sendRandomNumberCmd(aesKey!!)
                        }
                        OPERATION_GET_OTA_INFO -> {
                            sendGetOTAInfoCmd(aesKey!!)
                        }
                    }
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLog.i(
                        TAG,
                        "onCharacteristicWrite success " + DataUtil.bytes2Hex(characteristic!!.value)
                    )
                    bleCallback?.onCharacteristicWrite(true, characteristic.value)
                } else {
                    BleLog.i(
                        TAG,
                        "onCharacteristicWrite failed " + DataUtil.bytes2Hex(characteristic!!.value)
                    )
                    bleCallback?.onCharacteristicWrite(false, characteristic.value)
                }
            }


            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                BleLog.i(
                    TAG,
                    "onCharacteristicChanged: ${DataUtil.bytes2Hex(characteristic!!.value)}"
                )
                combinePacket(characteristic!!.value)
            }
        })
    }

    /**
     * 合并分包
     */
    private fun combinePacket(byteArray: ByteArray) {
        // 情况一：一帧完整的数据包，直接处理
        if (byteArray[0] == 0xf0.toByte() &&
            byteArray[1] == 0xa5.toByte() &&
            byteArray[byteArray.size - 2] == 0x16.toByte() &&
            byteArray[byteArray.size - 1] == 0x0d.toByte()
        ) {
            packetList.clear()
            packetList.add(byteArray)
            val data = DataUtil.combineByteArray(packetList)
            // 收到通知，解析数据
            parseData(data)
        } else if (byteArray[0] == 0xf0.toByte() && //头数据包
            byteArray[1] == 0xa5.toByte()
        ) {
            packetList.clear()
            packetList.add(byteArray)
        } else if (byteArray[byteArray.size - 2] == 0x16.toByte() &&  //尾数据包
            byteArray[byteArray.size - 1] == 0x0d.toByte()
        ) {
            packetList.add(byteArray)
            val data = DataUtil.combineByteArray(packetList)
            // 收到通知，解析数据
            parseData(data)
        } else { // 中间数据包
            packetList.add(byteArray)
        }
    }

    /**
     * 解析Notify数据
     */
    private fun parseData(data: ByteArray) {
        val errorCode = data[12].toInt()
        val hasError = handleErrorCode(errorCode)
        if (hasError) {
            internalRelease()
            val msg = UKLockError.getErrorMessage(errorCode)
            BleLog.i("解析回包出错, $msg")
            bleCallback?.onFailed(errorCode, msg)
            return
        }
        when (data[9]) {
            0xa0.toByte() -> getAESKeyResponse(data)
            0x03.toByte() -> getOTAInfoResponse(data)
            0x06.toByte() -> getRandomNumberResponse(data)
            0x30.toByte() -> getOpenDoorResponse(data)
            0x33.toByte() -> getModifyPwdResponse(data)
        }
    }

    /**
     * 初始化指令回复
     */
    private fun getAESKeyResponse(data: ByteArray) {
        internalRelease()
        val len = data[11] - 1
        val aesKey = ByteArray(len)
        for (i in 13 until 13 + len) {
            aesKey[i - 13] = data[i]
        }
        BleLog.i("getAesKey: ${DataUtil.bytes2Hex(aesKey)}")
        bleCallback?.onGetAESKey(aesKey)
    }

    /**
     * 固件信息指令回复
     */
    private fun getOTAInfoResponse(data: ByteArray) {
        internalRelease()

    }

    /**
     * 随机数指令回复
     */
    private fun getRandomNumberResponse(data: ByteArray) {
        val len = data[11] - 1
        val random = ByteArray(len)
        for (i in 13 until 13 + len) {
            random[i - 13] = data[i]
        }
        BleLog.i("random: ${DataUtil.bytes2Hex(random)}")
        when (currentOperation) {
            OPERATION_OPEN_DOOR -> {
                sleep()
                sendOpenDoorCmd(random)
            }
            OPERATION_UPDATE_PASSWORD -> {
                sleep()
                sendUpdatePwdCmd(random)
            }
        }
    }

    /**
     * 开门指令回复
     */
    private fun getOpenDoorResponse(data: ByteArray) {
        internalRelease()
        bleCallback?.onOpenDoorSuccess(data)
    }

    /**
     * 更新密码指令回复
     */
    private fun getModifyPwdResponse(data: ByteArray) {
        internalRelease()
    }

    private fun handleErrorCode(errorCode: Int): Boolean {
        return when (errorCode) {
            0x00, 0x06 -> {
                false
            }
            else -> {
                true
            }
        }
    }

    /**
     * 扫描超时
     */
    private fun startScanTimer() {
        timeoutTimer.cancel()
        timeoutTimer.start()
    }

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * 蓝牙是否可用
     */
    fun isBluetoothEnable() = bluetoothAdapter.enable()

    fun getDeviceMap() = devicesMap

    /**
     * 注册ble扫描连接过程监听
     */
    fun registerBleCallback(callback: BleCallback) {
        bleCallback = callback
    }

    fun unRegisterBleCallback() {
        bleCallback = null
    }

    fun startScan() {
        onlyScan = true
        stopScan()
        startScanTimer()
        devicesMap.clear()
        val filters = ArrayList<ScanFilter>()
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(UUIDHelper.SERVICE_UUID))).build()
        filters.add(filter)
        val scanSettings = ScanSettings.Builder().build()
        bluetoothAdapter.bluetoothLeScanner.startScan(filters, scanSettings, scanCallback)
    }

    /**
     * 开始扫描设备
     * timeout 超时时间，单位ms
     */
    private fun startScan(mac: String?, needInitDevice: Boolean = false) {
        if (mac.isNullOrEmpty()) throw NullPointerException("mac must not empty")
        state = STATE_NEED_RETRY
        remoteMac = mac
        onlyScan = false
        this.needInitDevice = needInitDevice
        //开启计时器
        startScanTimer()
        stopScan()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private fun writeData(data: ByteArray) {
        ThreadUtil.runOnUiThread {
            Log.i(TAG,"bluetoothGattService: $bluetoothGattService")
            val writeCharacteristic =
                bluetoothGattService?.getCharacteristic(UUID.fromString(UUIDHelper.WRITE_UUID))
            Log.i(TAG,"writeCharacteristic: $writeCharacteristic")
            writeCharacteristic?.value = data
            sleep(80)
            bluetoothGatt?.writeCharacteristic(writeCharacteristic)
        }
    }

    /**
     * 初始化设备
     */
    fun initialDevice(mac: String?) {
        currentOperation = OPERATION_DEVICE_INITIAL
        startScan(mac, true)
    }

    fun initialDevice(device: BluetoothDevice) {
        currentOperation = OPERATION_DEVICE_INITIAL
        connectGatt(device)
    }

    /**
     * 获取固件信息
     */
    fun getOTAInfo(mac: String?, aesKey: String) {
        currentOperation = OPERATION_GET_OTA_INFO
        this.aesKey = DataUtil.stringSlice(aesKey)
        startScan(mac)
    }

    fun getOTAInfo(device: BluetoothDevice, aesKey: String) {
        currentOperation = OPERATION_GET_OTA_INFO
        this.aesKey = DataUtil.stringSlice(aesKey)
        connectGatt(device)
    }

    /**
     * 开门
     */
    fun openDoor(mac: String?, password: String, aesKey: String) {
        if (password.length != 8) throw IllegalArgumentException("password length must be 8")
        currentOperation = OPERATION_OPEN_DOOR
        this.password = password
        this.aesKey = DataUtil.stringSlice(aesKey)
        startScan(mac)
    }

    fun openDoor(device: BluetoothDevice, password: String, aesKey: String) {
        stopScan()
        timeoutTimer.cancel()
        if (password.length != 8) throw IllegalArgumentException("password length must be 8")
        currentOperation = OPERATION_OPEN_DOOR
        state = STATE_NEED_RETRY
        this.password = password
        this.aesKey = DataUtil.stringSlice(aesKey)
        remoteMac = StringBuilder(device.address.replace(":", "")).reverse().toString().toLowerCase()
        connectGatt(device)
    }

    fun updatePassword(mac: String?, oldPassword: String, newPassword: String, aesKey: String) {
        if (oldPassword.length != 8 || newPassword.length != 8) throw IllegalArgumentException("password length must be 8")
        currentOperation = OPERATION_UPDATE_PASSWORD
        this.password = newPassword
        this.oldPwd = oldPassword
        this.aesKey = DataUtil.stringSlice(aesKey)
        startScan(mac)
    }

    fun updatePassword(device: BluetoothDevice, oldPwd: String, newPwd: String, aesKey: String) {
        if (oldPwd.length != 8 || newPwd.length != 8) throw IllegalArgumentException("password length must be 8")
        currentOperation = OPERATION_UPDATE_PASSWORD
        this.password = newPwd
        this.oldPwd = oldPwd
        this.aesKey = DataUtil.stringSlice(aesKey)
        connectGatt(device)
    }

    private fun sendInitDeviceCmd() {
        BleLog.i("sendInitDeviceCmd")
        val basicData =
            Operator.getBasicData(GET_AES_KEY_COMMAND, GET_AES_KEY_DATA, DEFAULT_AES_KEY)
        val divideData = DataUtil.divideByteArray(basicData)
        divideData.forEach {
            writeData(it)
        }
    }

    /**
     * 获取随机数
     */
    private fun sendRandomNumberCmd(aesKey: ByteArray) {
        BleLog.i("sendRandomNumberCmd,  aesKey: ${DataUtil.bytes2Hex(aesKey)}")
        val basicData =
            Operator.getBasicData(GET_RANDOM_NUMBER_COMMAND, GET_RANDOM_NUMBER_DATA, aesKey)
        val divideData = DataUtil.divideByteArray(basicData)
        divideData.forEach {
            writeData(it)
        }
    }

    private fun sendGetOTAInfoCmd(aesKey: ByteArray) {
        val basicData = Operator.getBasicData(GET_OTA_INFO_COMMAND, GET_OTA_INFO_DATA, aesKey)
        val divideData = DataUtil.divideByteArray(basicData)
        divideData.forEach {
            writeData(it)
        }
    }

    private fun sendOpenDoorCmd(random: ByteArray) {
        BleLog.i("sendOpenDoorCmd, aesKey: ${DataUtil.bytes2Hex(aesKey!!)}")
        val basicData = Operator.getBasicData(
            GET_OPEN_DOOR_COMMAND,
            Operator.getOpenDoorData(password, random, 200),
            aesKey!!
        )
        val openDoorData = DataUtil.divideByteArray(basicData)
        openDoorData.forEach {
            writeData(it)
            BleLog.i("opendoor cmd " + DataUtil.bytes2Hex(it))
        }
    }

    private fun sendUpdatePwdCmd(random: ByteArray) {
        BleLog.i("sendUpdatePwdCmd, aesKey: ${DataUtil.bytes2Hex(aesKey!!)}")
        val basicData = Operator.getBasicData(
            GET_UPDATE_PWD_COMMAND,
            Operator.getUpdatePwdData(oldPwd, password, random)
        )
        DataUtil.divideByteArray(basicData).apply {
            forEach {
                writeData(it)
            }
        }
    }

    fun release() {
        stopScan()
        timeoutTimer.cancel()
        unRegisterBleCallback()
        internalRelease()
    }

    private fun internalRelease() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        bluetoothGattService = null
    }

}