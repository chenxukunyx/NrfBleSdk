package com.miracle.lib_ble.utils

/**
 * Created with Android Studio
 *
 * @author: chenxukun
 * @date: 2020/10/12
 * @time: 3:14 PM
 * @desc:
 */
object DataUtil {

    fun bytes2Hex(data: ByteArray, start: Int = 0, len: Int = data.size): String {
        val sb = StringBuilder()
        for (i in start until start + len) {
            sb.append(String.format("%02x", data[i]))
        }
        return sb.toString()
    }

    fun stringSlice(content: String): ByteArray {
        val totalLen = content.length
        val data = ByteArray(totalLen / 2)
        var i = 0
        while (i < totalLen) {
            data[i / 2] = (content.substring(i, i + 2).toInt(16) and 0xff).toByte()
            i += 2
        }
        return data
    }

    fun stringAllSlice(content: String): ByteArray {
        val totalLen = content.length
        val data = ByteArray(totalLen)
        var i = 0
        while (i < totalLen) {
            data[i] = (content.substring(i, i + 1).toInt(16) and 0xff).toByte()
            i += 1
        }
        return data
    }

    /**
     * 合并分包
     */
    fun combineByteArray(byteList: List<ByteArray>): ByteArray {
        var totalSize = 0
        byteList.forEach {
            totalSize += it.size
        }
        val ret = ByteArray(totalSize)
        var mod = 0
        byteList.forEach {
            it.forEach {byte ->
                ret[mod] = byte
                mod += 1
            }
        }
        return ret
    }

    /**
     * 分包
     */
    fun divideByteArray(srcData: ByteArray): List<ByteArray> {
        BleLog.i("原始数据：${bytes2Hex(srcData)}")
        val ret = ArrayList<ByteArray>()
        val dataLen = srcData.size
        if (dataLen <= 20) return arrayListOf(srcData)
        var isCompletePacket = dataLen % 20 == 0
        val packetCount = if (isCompletePacket) dataLen / 20 else dataLen / 20 + 1
        for (i in 0 until packetCount) {
            val tmpArray = if (isCompletePacket) {
                ByteArray(20)
            } else {
                if (i == packetCount - 1) {
                    ByteArray(dataLen % 20)
                } else {
                    ByteArray(20)
                }
            }

            if (i != packetCount - 1) {
                for (j in i * 20 until (i * 20 + 20)) {
                    tmpArray[j - i * 20] = srcData[j]
                }
                ret.add(tmpArray)
            } else {
                for (j in i * 20 until dataLen) {
                    tmpArray[j - i * 20] = srcData[j]
                }
                ret.add(tmpArray)
            }

        }
        ret.forEach {
            BleLog.i("分包数据：" + bytes2Hex(it))
        }
        return ret
    }
}