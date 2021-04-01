package com.miracle.lib_ble.utils

import java.io.UnsupportedEncodingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/26/21
 * @desc:
 */
object AESEncryptUtil {

    private const val CipherMode = "AES/CBC/PKCS7Padding"

    private fun createKey(byteArray: ByteArray): SecretKeySpec {
        return SecretKeySpec(byteArray, "AES")
    }

    private fun createIV(byteArray: ByteArray): IvParameterSpec {
        return IvParameterSpec(byteArray)
    }

    fun encrypt(content: ByteArray, password: ByteArray): ByteArray? {
        val secretKeySpec = createKey(password)
        try {
            val cipher = Cipher.getInstance(CipherMode)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, createIV(password))
            return cipher.doFinal(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun decrypt(content: ByteArray?, password: ByteArray): ByteArray? {
        val secretKeySpec = createKey(password)
        try {
            val cipher = Cipher.getInstance(CipherMode)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, createIV(byteArrayOf()))
            return cipher.doFinal(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}