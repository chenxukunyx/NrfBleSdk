package com.miracle.nrfblesdk

import android.app.Dialog
import android.content.Context
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_modify_nickname.*

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/8/21
 * @desc:
 */
class ModifyDialog(context: Context, private val nickname: String?, private val confirm: (nickname: String) -> Unit): Dialog(context) {


    init {
        setContentView(R.layout.dialog_modify_nickname)
        et_nickname.setText(nickname)
        inject()
    }

    private fun inject() {
        tv_cancel.setOnClickListener {
            cancel()
        }

        tv_confirm.setOnClickListener {
            val nickname = et_nickname.text.toString()
            if (nickname.isNullOrEmpty()) {
                Toast.makeText(context, "内容为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirm.invoke(nickname)
            cancel()
        }
    }
}