package com.miracle.nrfblesdk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/7/21
 * @desc:
 */
object ClipManager {

    private val clipboardManager by lazy {
        MyApp.getContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    fun getTextFromClip(): String? {
        if (!clipboardManager.hasPrimaryClip())
            return null
        val clipData = clipboardManager.primaryClip
        return if (clipData!!.getItemAt(0).text == null) "" else clipData!!.getItemAt(0).text.toString()
    }


    fun setTextToClip(string: String) {
        val clipData = ClipData.newPlainText("Label", string)
        clipboardManager.setPrimaryClip(clipData)
    }
}