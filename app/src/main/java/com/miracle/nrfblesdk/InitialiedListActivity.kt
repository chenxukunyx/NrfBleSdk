package com.miracle.nrfblesdk

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.loadingview.LoadingDialog
import com.miracle.lib_ble.utils.BleLog
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_initialied_list.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.concurrent.thread


/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/2/21
 * @desc:
 */
class InitialiedListActivity : AppCompatActivity() {

    private val TAG = "InitialiedListActivity"

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, InitialiedListActivity::class.java))
        }
    }

    private val loading by lazy {
        LoadingDialog[this]
    }

    private val adapter by lazy {
        InitialiedListAdapter(this, CacheManager.getAllValue(),
            delete = {
                showDeleteConfirmDialog(it)
            },
            modify = { mac, _ ->
                showModifyDialog(mac)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialied_list)
        setSupportActionBar(toolbar)
        initDeviceList()
        inject()
    }

    private fun inject() {
        refresh.setOnRefreshListener {
            adapter.update(CacheManager.getAllValue())
            refresh.isRefreshing = false
        }

        iv_export.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun initDeviceList() {
        rv_device_list.layoutManager = LinearLayoutManager(this)
        rv_device_list.adapter = adapter
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showModifyDialog(mac: String) {

        val json = CacheManager.get(mac)
        val bean = JsonUtil.fromJson(json, DeviceBean::class.java)
        val dialog = ModifyDialog(this, bean.nickname) { nickname ->

            bean.nickname = nickname
            val ret = JsonUtil.toJson(bean)
            CacheManager.put(mac, ret)
            adapter.update(CacheManager.getAllValue())
        }
        dialog.show()
    }

    private fun showDeleteConfirmDialog(mac: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("是否要删除这条记录？")
            .setMessage("此操作会删除此设备的所有数据，为不可逆的，确认删除？")
            .setNegativeButton("删除") { dialog, _ ->
                CacheManager.remove(mac)
                adapter.update(CacheManager.getAllValue())
                dialog.cancel()
            }
            .setPositiveButton("取消") { dialog, _ ->
                dialog.cancel()
            }
            .create()
        dialog.show()
    }

    private fun checkLocationPermission() {
        if (AndPermission.hasPermissions(this, Permission.WRITE_EXTERNAL_STORAGE)) {
            writeData(CacheManager.getAllValue()) {
                runOnUiThread {
                    loading.hide()
                    if (it) {
                        showToast("导出成功")
                    } else {
                        showToast("导出失败")
                    }
                }

            }
        } else {
            AndPermission.with(this)
                .runtime()
                .permission(Permission.WRITE_EXTERNAL_STORAGE)
                .onDenied {
                    Toast.makeText(this, "储存权限已被禁止,请到权限管理授权", Toast.LENGTH_LONG).show()
                }
                .onGranted {
                    writeData(CacheManager.getAllValue()) {
                        runOnUiThread {
                            loading.hide()
                            if (it) {
                                showToast("导出成功")
                            } else {
                                showToast("导出失败")
                            }
                        }

                    }
                }
                .start()
        }
    }

    private fun writeData(list: ArrayList<String>, result: (success: Boolean) -> Unit) {
        loading.show()
        val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val path = if (sdCardExist) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Environment.getExternalStorageDirectory().absolutePath + "/A_LMH"
            } else {
                Environment.getExternalStorageDirectory().absolutePath + "/A_LMH"
            }
        } else {
            Environment.getRootDirectory().absolutePath + "/com.miracle.nrfbledemo/A_LMH"
        }
        BleLog.i(TAG, "sdcardExixt: $sdCardExist, storage path is: $path")
        thread {
            val dir = File(path)
            if (!dir.exists())
                dir.mkdirs()
            val file = File(dir, "lock.csv")
            if (file.exists()) file.delete()
            var bw: BufferedWriter? = null
            try {
                bw = BufferedWriter(FileWriter(file))
                list.forEach {
                    val bean = JsonUtil.fromJson(it, DeviceBean::class.java)
                    bw!!.append(bean.nickname).append(", ").append(bean.mac).append(", ")
                        .append(bean.aesKey).append("\r\n")
                }
                result.invoke(true)
            } catch (e: Exception) {
                e.printStackTrace()
                result.invoke(false)
            } finally {
                try {
                    bw?.close()
                    bw = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


    }
}

class InitialiedListAdapter(
    private val context: Context,
    private val list: ArrayList<String>,
    private val delete: (mac: String) -> Unit,
    private val modify: (mac: String, aesKey: String?) -> Unit
) : RecyclerView.Adapter<InitialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InitialViewHolder {
        return InitialViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_device_initial, null)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: InitialViewHolder, position: Int) {
        handleInitDevice(holder, position)
    }

    private fun handleInitDevice(holder: InitialViewHolder, position: Int) {
        val json = list[position]
        val bean = JsonUtil.fromJson(json, DeviceBean::class.java)
        holder.tv_mac.text = bean.aesKey
        holder.tv_name.text = bean.mac
        holder.tv_has_init.visibility = View.GONE
        holder.tv_open_door.visibility = View.GONE
        val aesKey = bean.aesKey
        holder.tv_nickname.text = bean.nickname

        holder.tv_delete.setOnClickListener {
            delete.invoke(bean.mac)
        }
        holder.tv_modify_device_nickname.setOnClickListener {
            modify.invoke(bean.mac, aesKey)
        }
    }

    fun update(l: ArrayList<String>) {
        list.clear()
        list.addAll(l)
        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

}
