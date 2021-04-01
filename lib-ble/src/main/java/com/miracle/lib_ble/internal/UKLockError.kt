package com.miracle.lib_ble.internal

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/29/21
 * @desc:
 */
internal object UKLockError {

    private val errorMap = HashMap<Int, String>().apply {
        put(0x00, "success")
        put(0x01, "通用错误")
        put(0x02, "存储错误")
        put(0x03, "模式错误")
        put(0x04, "参数错误")
        put(0x05, "操作不允许")
        put(0x06, "操作完成")
        put(0x07, "版本不支持")
        put(0x0b, "等待内部执行")
        put(0x0c, "指令不支持")
        put(0x0d, "秘钥或密码错误")
        put(0x10001, "设备未初始化")
    }

    fun getErrorMessage(errorCode: Int): String? {
        if (errorMap.containsKey(errorCode)) {
            return errorMap[errorCode]
        }
        return null
    }


}