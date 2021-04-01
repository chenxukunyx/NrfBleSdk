package com.miracle.lib_ble.utils

import android.os.Handler
import android.os.Looper

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/30/21
 * @desc:
 */
internal object ThreadUtil {

    private val handler = Handler(Looper.getMainLooper())

    fun assertMainThread() {
        if (!isOnMainThread()) throw IllegalArgumentException("You must call this method on main thread")
    }

    fun assertBackgroundThread() {
        if (!isOnBackgroundTHread()) throw IllegalArgumentException("You must call this method on new thread")
    }

    fun isOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    fun isOnBackgroundTHread(): Boolean {
        return !isOnMainThread()
    }

    fun runOnUiThread(call: () -> Unit) {
        if (isOnMainThread())
            call.invoke()
        else
            handler.post {
                call.invoke()
            }
    }

    fun postDelay(delayTime: Long = 1300L, call: () -> Unit) {
        assertMainThread()
        handler.postDelayed({
            call.invoke()
        }, delayTime)
    }
}