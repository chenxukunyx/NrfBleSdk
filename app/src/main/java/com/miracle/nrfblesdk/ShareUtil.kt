package com.miracle.nrfblesdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/9/21
 * @desc:
 */
object ShareUtil {

    fun share(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_STREAM, getUri(context, file))
        intent.type = "*/*"
        context.startActivity(Intent.createChooser(intent, "Share to..."))
    }

    private fun getUri(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //如果在Android7.0以上,使用FileProvider获取Uri
            //7.0以上的读取文件uri要用这种方式了
            FileProvider.getUriForFile(
                context.applicationContext,
                "com.miracle.nrfblesdk.fileprovider",
                file
            )
        } else {
            //否则使用Uri.fromFile(file)方法获取Uri
            Uri.fromFile(file)
        }
    }
}