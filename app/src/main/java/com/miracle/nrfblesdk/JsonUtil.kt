package com.miracle.nrfblesdk

import com.google.gson.Gson

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/8/21
 * @desc:
 */
object JsonUtil {

    private val gson by lazy {
        Gson()
    }

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson<T>(json, clazz)
    }
}