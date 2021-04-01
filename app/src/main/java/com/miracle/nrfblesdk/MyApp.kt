package com.miracle.nrfblesdk

import android.app.Application
import com.miracle.lib_ble.BluetoothManager
import com.miracle.lib_ble.utils.BleUtil

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/26/21
 * @desc:
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        BluetoothManager.init(this)
        BleUtil.init(this)
    }
}