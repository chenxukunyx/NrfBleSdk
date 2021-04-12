package com.miracle.lib_ble.utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.widget.Toast
import com.miracle.lib_ble.internal.UUIDHelper
import java.util.*
import kotlin.experimental.and

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/24/21
 * @desc:
 */
object BleUtil {
    private const val TAG = "BleUtil"

    private lateinit var context: Context
    private const val NAME = "aes_key"

    internal fun enableNotification(
        gatt: BluetoothGatt,
        enable: Boolean,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        if (gatt == null || characteristic == null) {
            return false
        }
        if (!gatt.setCharacteristicNotification(characteristic, enable)) {
            return false
        }
        //获取到Notify当中的Descriptor通道  然后再进行注册
        val clientConfig =
            characteristic.getDescriptor(UUID.fromString(UUIDHelper.NOTIFY_DESCRIPTOR))
                ?: return false
        if (enable) {
            clientConfig.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            clientConfig.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        return gatt.writeDescriptor(clientConfig)
    }

    internal fun enableIndications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        if (gatt == null || characteristic == null) {
            return false
        }
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return false
        }
        //获取到Notify当中的Descriptor通道  然后再进行注册
        val clientConfig = characteristic.getDescriptor(UUID.fromString(UUIDHelper.NOTIFY_DESCRIPTOR)) ?: return false
        clientConfig.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        return gatt.writeDescriptor(clientConfig)
    }

    private val sp by lazy {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        this.context = context
    }

    fun saveAESKey(key: String, aesKey: String) {
        val editor = sp.edit()
        editor.putString(key, aesKey)
        editor.commit()
    }

    fun getAESKey(key: String): ByteArray? {
        val aesStr = sp.getString(key, null) ?: return null
        return DataUtil.stringSlice(aesStr)
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun convertMac(mac: String): String {
        var ret = mac
        if (ret.contains(":")) {
            ret = ret.replace(":", "")
        }
        return StringBuilder(ret).reverse().toString()
    }

    fun isDeviceInit(byteArray: ByteArray): Boolean {
        if (byteArray.isEmpty()) return false
        for (i in byteArray.indices) {
            if (byteArray[i] == 0xff.toByte()) {
                if (i + 9 < byteArray.size - 1) {
                    val ret = (byteArray[i + 9] and 4.toByte()) != 4.toByte()
                    return ret
                }
                return false
            }
        }
        return false
    }
}