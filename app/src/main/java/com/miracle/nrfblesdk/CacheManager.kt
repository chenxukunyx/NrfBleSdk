package com.miracle.nrfblesdk

import android.content.Context
import android.util.Log
import com.miracle.lib_ble.utils.BleLog
import com.tencent.mmkv.MMKV

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/8/21
 * @desc:
 */
object CacheManager {

    private const val TAG = "CacheManager"

    private val mmkv by lazy {
        MMKV.defaultMMKV()!!
    }

    fun init(context: Context) {
        val rootDir = MMKV.initialize(context)
        BleLog.i(TAG, "CacheManager init: $rootDir")
    }

    fun put(key: String, value: String) {
//        val mmkv = MMKV.defaultMMKV()
        mmkv.encode(key, value)
    }

    fun get(key: String): String {
//        val mmkv = MMKV.defaultMMKV()
        val value = mmkv.decodeString(key)
        return value ?: ""
    }

    fun remove(key: String) {
//        val mmkv = MMKV.defaultMMKV()
        mmkv.removeValueForKey(key)
    }

    fun containsKey(key: String): Boolean {
//        val mmkv = MMKV.defaultMMKV()
        return mmkv.containsKey(key) ?: false
    }

    fun getAllValue(): ArrayList<String> {
        val allKeys = mmkv.allKeys()
        if (allKeys == null || allKeys?.isEmpty()) return arrayListOf()
        val ret = ArrayList<String>()
        allKeys.forEach {
            ret.add(get(it))
        }
        return ret
    }
}