package com.miracle.lib_ble.utils

import android.util.Log

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/24/21
 * @desc:
 */
object BleLog {

    private const val TAG = "----BleLog----"
    var isDebug = true

    fun v(msg: String?) {
        v(null, msg)
    }

    fun v(tag: String?, msg: String?) {
        if (!isDebug) return
        Log.v(TAG, "${if (tag.isNullOrEmpty()) "----->" else "$tag----->"}: $msg")
    }

    fun d(msg: String?) {
        d(null, msg)
    }

    fun d(tag: String?, msg: String?) {
        if (!isDebug) return
        Log.d(TAG, "${if (tag.isNullOrEmpty()) "----->" else "$tag----->"}: $msg")
    }

    fun i(msg: String?) {
        i(null, msg)
    }

    fun i(tag: String?, msg: String?) {
        if (!isDebug) return
        Log.i(TAG, "${if (tag.isNullOrEmpty()) "----->" else "$tag----->"}: $msg")
    }

    fun w(msg: String?) {
        w(null, msg)
    }

    fun w(tag: String?, msg: String?) {
        if (!isDebug) return
        Log.w(TAG, "${if (tag.isNullOrEmpty()) "----->" else "$tag----->"}: $msg")
    }

    fun e(msg: String?) {
        e(null, msg)
    }

    fun e(tag: String?, msg: String?) {
        if (!isDebug) return
        Log.e(TAG, "${if (tag.isNullOrEmpty()) "----->" else "$tag----->"}: $msg")
    }
}