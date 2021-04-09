package com.miracle.nrfblesdk

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/8/21
 * @desc:
 */
data class DeviceBean(
    val mac: String,
    val aesKey: String?,
    var nickname: String?
) {
    var deviceName: String? = null
}