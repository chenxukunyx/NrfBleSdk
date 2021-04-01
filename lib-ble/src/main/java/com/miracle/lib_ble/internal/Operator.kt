package com.miracle.lib_ble.internal

import com.miracle.lib_ble.utils.AESEncryptUtil
import com.miracle.lib_ble.utils.BleLog
import com.miracle.lib_ble.utils.CRCUtil
import com.miracle.lib_ble.utils.DataUtil

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/25/21
 * @desc:
 */
internal object Operator {

    /**
     * 获取加密数据
     */
    fun getBasicData(operation: Byte, data: ByteArray, aesKey: ByteArray): ByteArray {
        val dataEncrypt = AESEncryptUtil.encrypt(data, aesKey)
        if (dataEncrypt == null) {
            BleLog.i("encrypt failed")
            throw IllegalStateException("aes encrypt failed")
        }

        val dataLen = dataEncrypt.size
        val ret = ByteArray(15 + dataLen)
        ret[0] = 0xf0.toByte() //起始符
        ret[1] = 0xa5.toByte() //起始符
        ret[2] = 0x01.toByte() //协议版本
        ret[3] = 0x00.toByte() //协议版本
        ret[4] = 0x00.toByte() //应用场景
        ret[5] = 0x00.toByte() //厂商代码
        ret[6] = 0x00.toByte() //保留字节
        ret[7] = 0x00.toByte() //保留字节
        ret[8] = 0x00.toByte() //保留字节
        ret[9] = operation //命令字
        ret[10] = 0xaa.toByte() //数据来源
        ret[11] = (dataLen and 0xff).toByte() //数据长度
        for (i in 0 until dataLen) {
            ret[12 + i] = dataEncrypt[i]
        }
        ret[11 + dataLen + 1] = CRCUtil.getCRCRet(ret, 12 + dataLen, 0).toByte()
        ret[11 + dataLen + 2] = 0x16
        ret[11 + dataLen + 3] = 0x0d
        return ret
    }

    /**
     * 获取未加密数据
     */
    fun getBasicData(operation: Byte, data: ByteArray): ByteArray {

        val dataLen = data.size
        val ret = ByteArray(15 + dataLen)
        ret[0] = 0xf0.toByte() //起始符
        ret[1] = 0xa5.toByte() //起始符
        ret[2] = 0x01.toByte() //协议版本
        ret[3] = 0x00.toByte() //协议版本
        ret[4] = 0x00.toByte() //应用场景
        ret[5] = 0x00.toByte() //厂商代码
        ret[6] = 0x00.toByte() //保留字节
        ret[7] = 0x00.toByte() //保留字节
        ret[8] = 0x00.toByte() //保留字节
        ret[9] = operation //命令字
        ret[10] = 0xaa.toByte() //数据来源
        ret[11] = (dataLen and 0xff).toByte() //数据长度
        for (i in 0 until dataLen) {
            ret[12 + i] = data[i]
        }
        ret[11 + dataLen + 1] = CRCUtil.getCRCRet(ret, 12 + dataLen, 0).toByte()
        ret[11 + dataLen + 2] = 0x16
        ret[11 + dataLen + 3] = 0x0d
        return ret
    }

    /**
     * 开门指令
     */
    fun getOpenDoorData(pwd: String, random: ByteArray, delayTime: Long): ByteArray {
        val ret = ByteArray(22)
        random.forEachIndexed { index, byte ->
            ret[index] = byte
        }
        val pwdByte = DataUtil.stringAllSlice(pwd)
        pwdByte.forEachIndexed { index, byte ->
            ret[index + 8] = byte
        }
        ret[16] = 0
        ret[17] = 0xa1.toByte()
        ret[18] = ((delayTime and 0xff000000) shr 24).toByte()
        ret[19] = ((delayTime and 0xff0000) shr 16).toByte()
        ret[20] = ((delayTime and 0xff00) shr 8).toByte()
        ret[21] = (delayTime and 0xff).toByte()
        return ret
    }

    /**
     * 更新密码指令
     */
    fun getUpdatePwdData(oldPwd: String, newPwd: String, random: ByteArray): ByteArray {
        val ret = ByteArray(24)
        random.forEachIndexed { index, byte ->
            ret[index] = byte
        }
        val oldPwdByte = DataUtil.stringAllSlice(oldPwd)
        oldPwdByte.forEachIndexed { index, byte ->
            ret[index + 8] = byte
        }
        val newPwdByte = DataUtil.stringAllSlice(newPwd)
        newPwdByte.forEachIndexed { index, byte ->
            ret[index + 16] = byte
        }
        return ret
    }
}