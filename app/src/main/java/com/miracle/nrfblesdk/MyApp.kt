package com.miracle.nrfblesdk

import android.app.Application
import android.content.Context
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

    companion object{
        private var context: Context? = null

        fun getContext() = context!!
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        BluetoothManager.init(this)
        BleUtil.init(this)
        CacheManager.init(this)
    }
}