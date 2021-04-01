package com.miracle.lib_ble.constants

import com.miracle.lib_ble.utils.BleUtil
import com.miracle.lib_ble.utils.DataUtil

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/26/21
 * @desc:
 *   --- _COMMAND结尾的为命令行
 *   --- _DATA结尾的为数据
 */

const val OPERATION_GET_OTA_INFO = 0x01
const val OPERATION_OPEN_DOOR = 0x02
const val OPERATION_UPDATE_PASSWORD = 0x04
const val OPERATION_DEVICE_INITIAL = 0x08

val TEST_AES_KEY = DataUtil.stringSlice("b035f97b9cf45922bd52572bedabe2ed")

val DEFAULT_AES_KEY = byteArrayOf(
    0x98.toByte(),
    0x76.toByte(),
    0x23.toByte(),
    0xE8.toByte(),
    0xA9.toByte(),
    0x23.toByte(),
    0xA1.toByte(),
    0xBB.toByte(),
    0x3D.toByte(),
    0x9E.toByte(),
    0x7D.toByte(),
    0x03.toByte(),
    0x78.toByte(),
    0x12.toByte(),
    0x45.toByte(),
    0x88.toByte())

const val GET_RANDOM_NUMBER_COMMAND: Byte = 0x06.toByte()
val GET_RANDOM_NUMBER_DATA = byteArrayOf(0x06)

const val GET_AES_KEY_COMMAND = 0xa0.toByte()
val GET_AES_KEY_DATA = byteArrayOf(0x01)

const val GET_OTA_INFO_COMMAND = 0x03.toByte()
val GET_OTA_INFO_DATA = byteArrayOf(0x01)

const val GET_OPEN_DOOR_COMMAND = 0x30.toByte()

const val GET_UPDATE_PWD_COMMAND = 0x33.toByte()