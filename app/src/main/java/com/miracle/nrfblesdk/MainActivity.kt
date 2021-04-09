package com.miracle.nrfblesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.miracle.lib_ble.callback.BleCallback
import com.miracle.lib_ble.BluetoothManager
import com.miracle.lib_ble.constants.*
import com.miracle.lib_ble.utils.BleUtil
import com.miracle.lib_ble.utils.DataUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BluetoothManager.registerBleCallback(callback)

        scan.setOnClickListener {
//            BluetoothManager.startScan(this, "f171b32c92c0")
//            BluetoothManager.initialDevice(this, "f171b32c92c0")
//            BluetoothManager.startScan()
            startActivity(Intent(this, ScanResultActivity::class.java))
        }

        init.setOnClickListener {
//            val byteArray = DataUtil.divideByteArray(Operator.getBasicData(GET_AES_KEY_COMMAND.toByte(), GET_AES_KEY_DATA, DEFAULT_AES_KEY))
//            byteArray.apply {
//                forEach {
//                    BluetoothManager.writeData(it)
//                }
//            }
//            BluetoothManager.initialDevice( "f171b32c92c0")
        }

        sendRandom.setOnClickListener {
//            val byteArray = DataUtil.divideByteArray(Operator.getBasicData(GET_RANDOM_NUMBER_COMMAND, GET_RANDOM_NUMBER_DATA, TEST_AES_KEY))
//            byteArray.apply {
//                forEach {
//                    BluetoothManager.writeData(it)
//                }
//            }
        }

        opendoor.setOnClickListener {
//            val byteArray = DataUtil.divideByteArray(Operator.getBasicData(GET_OPEN_DOOR_COMMAND, Operator.getOpenDoorData("00000000", ParseDataStorage.getRandom()!!, 200), TEST_AES_KEY))
//
//            byteArray.apply {
//                forEach {
//                    BleLog.i("opendoor cmd " + DataUtil.bytes2Hex(it))
//                    BluetoothManager.writeData(it)
//                    SystemClock.sleep(500)
//                }
//            }
//            BluetoothManager.openDoor("f171b32c92c0","00000000", DataUtil.bytes2Hex(TEST_AES_KEY))
        }

        update_password.setOnClickListener {
//            BluetoothManager.updatePassword( "f171b32c92c0", "00000000", "11111111", DataUtil.bytes2Hex(TEST_AES_KEY))
        }

        get_ota_info.setOnClickListener {
//            BluetoothManager.getOtaInfo( "f171b32c92c0", DataUtil.bytes2Hex(TEST_AES_KEY))
        }
    }

    private val callback = object : BleCallback {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.i(TAG, "onScanResult: " + result?.device!!.address + " " + result?.device!!.name)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.i(TAG, "onScanFailed: ")
        }

        override fun onScanTimeout(isEmpty: Boolean) {
            Log.i(TAG, "onScanTimeout: ")
        }

        override fun onScanMatched(result: ScanResult) {
            Log.i(TAG, "onScanMatched: ")
        }

        override fun onScanMatched(result: HashMap<String, ScanResult>) {

        }

        override fun onStartConnectGatt(device: BluetoothDevice) {
            Log.i(TAG, "onStartConnectGatt: ")
        }

        override fun onBleConnected(gatt: BluetoothGatt) {
            Log.i(TAG, "onBleConnected: ")
        }

        override fun onBleDisconnected(gatt: BluetoothGatt) {
            Log.i(TAG, "onBleDisconnected: ")
        }

        override fun onBleServicesDiscovered(gatt: BluetoothGatt) {
            Log.i(TAG, "onBleServicesDiscovered: ")
        }

        override fun onCharacteristicWrite(success: Boolean, value: ByteArray) {
            Log.i(TAG, "onCharacteristicWrite: ")
        }

        override fun onFailed(code: Int, msg: String?) {
            Log.i(TAG, "onDataParseFailed: ")
        }

        override fun onGetAESKey(byteArray: ByteArray) {
            Log.i(TAG, "onGetAESKey: ")
            saveAESKey(byteArray)
        }

        override fun isDeviceInitial(init: Boolean) {
            Log.i(TAG, "isDeviceInitial: $init")
        }


        override fun onOpenDoorSuccess(data: ByteArray) {
            Log.i(TAG, "onOpenDoorSuccess: ")
        }

        override fun onConnectTimeout() {

        }

    }

    private fun saveAESKey(aes: ByteArray) {
        val aesStr = DataUtil.bytes2Hex(aes)
        val macAddress = "f171b32c92c0"
        if (macAddress.isNullOrEmpty()) {
            BleUtil.showToast("mac地址为空，保存aes失败")
            return
        }
        BleUtil.saveAESKey(macAddress!!, aesStr)
    }

}