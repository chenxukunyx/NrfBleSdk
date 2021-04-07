package com.miracle.nrfblesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.loadingview.LoadingDialog
import com.miracle.lib_ble.BluetoothManager
import com.miracle.lib_ble.callback.BleCallback
import com.miracle.lib_ble.utils.BleUtil
import com.miracle.lib_ble.utils.DataUtil
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlin.experimental.and

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 4/2/21
 * @desc:
 */
class DeviceListActivity : AppCompatActivity() {

    private val TAG = "DeviceListActivity"

    private val loading by lazy {
        LoadingDialog[this]
    }

    private val adapter by lazy {
        DeviceListAdapter(this, arrayListOf(),
            itemClick = {device, password, isInit ->
                Log.i(TAG, "itemClick")
                if (!isInit) {
                    showToast("设备还未初始化，请先进行初始化操作")
                    return@DeviceListAdapter
                }
                showDeviceInfoDialog(device, password)
            },
            initClick = {
                loading.show()
                mac = it.address
                BluetoothManager.initialDevice(it.address)
            }
        )
    }

    private val sp by lazy {
        getSharedPreferences("aeskey_sharedpref", Context.MODE_PRIVATE)
    }

    private val animation by lazy {
        RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            fillAfter = true
            repeatCount = Animation.INFINITE
            duration = 1000
            repeatMode = Animation.RESTART
            interpolator = LinearInterpolator()
        }
    }

    private var mac: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        setSupportActionBar(toolbar)
        registerCallback()
        initDeviceList()
        inject()

        startScan()
    }

    private fun inject() {
        refresh.setOnClickListener {
            startScan()
        }
    }

    private fun initDeviceList() {
        rv_device_list.layoutManager = LinearLayoutManager(this)
        rv_device_list.adapter = adapter
    }

    private fun registerCallback() {
        BluetoothManager.registerBleCallback(object : BleCallback {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {

            }

            override fun onScanFailed(errorCode: Int) {
                runOnUiThread {
                    endScan()
                }
            }

            override fun onScanTimeout(isEmpty: Boolean) {
                runOnUiThread {
                    endScan()
                }
            }

            override fun onScanMatched(result: ScanResult) {

            }

            override fun onScanMatched(result: HashMap<String, ScanResult>) {
                adapter.update(result)
            }

            override fun onStartConnectGatt(device: BluetoothDevice) {

            }

            override fun onBleConnected(gatt: BluetoothGatt) {

            }

            override fun onBleDisconnected(gatt: BluetoothGatt) {
                runOnUiThread {
                    startScan()
                }
            }

            override fun onBleServicesDiscovered(gatt: BluetoothGatt) {

            }

            override fun onCharacteristicWrite(success: Boolean, value: ByteArray) {

            }

            override fun onFailed(code: Int, msg: String?) {
                runOnUiThread {
                    loading.hide()
                    showToast("$code, $msg")
                }
            }

            override fun onGetAESKey(byteArray: ByteArray) {
                saveAes(byteArray)
                runOnUiThread {
                    loading.hide()
                    showToast("初始化成功")
                    startScan()
                }
            }

            override fun isDeviceInitial(init: Boolean) {

            }

            override fun onOpenDoorSuccess(data: ByteArray) {
                runOnUiThread {
                    loading.hide()
                    showToast("开门成功")
                    startScan()
                }
            }

        })
    }

    private fun saveAes(byteArray: ByteArray) {
        val editor = sp.edit()
        editor.putString(mac, DataUtil.bytes2Hex(byteArray))
        editor.commit()
    }

    private fun getAes(mac: String): String {
        return sp.getString(mac, "")?:""
//        return "b035f97b9cf45922bd52572bedabe2ed"
    }

    fun delAes(mac: String) {
        val editor = sp.edit()
        editor.remove(mac)
        editor.commit()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showDeviceInfoDialog(device: BluetoothDevice, password: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("mac: ${device.address}")
            .setMessage("aesKey: ${getAes(device.address)}")
            .setNegativeButton("尝试开门") { dialog, which ->
                if (sp.getString(device.address, null).isNullOrEmpty()) {
                    val msg = "设备不是在本机进行的初始化操作，未能获取到AesKey，可尝试对设备进行Reset操作"
                    showToast(msg)
                    return@setNegativeButton
                }
                dialog.cancel()
                loading.show()
                BluetoothManager.openDoor(device.address, password, getAes(device.address))
            }
            .create()
        dialog.show()
    }

    private fun startScan() {
        adapter.clear()
        endScan()
        refresh.animation = animation
        animation.start()
        BluetoothManager.startScan()
    }

    private fun endScan() {
        animation.cancel()
        refresh.clearAnimation()
    }
}

class DeviceListAdapter(
    private val context: Context,
    private val list: ArrayList<ScanResult>,
    private val itemClick: (device: BluetoothDevice, password: String, isInit: Boolean) -> Unit,
    private val initClick: (device: BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceListViewHolder {
        return DeviceListViewHolder(LayoutInflater.from(context).inflate(R.layout.item_device, null))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: DeviceListViewHolder, position: Int) {
        val scanResult = list[position]
        val device = scanResult.device
        val broadcast = scanResult.scanRecord!!.bytes
        Log.i("DeviceListActivity", "mac: ${device.address}, broadcast: ${DataUtil.bytes2Hex(broadcast)}")
        val hasInit = (broadcast[19] and 4.toByte()) != 4.toByte()
        holder.tv_mac.text = device.address
        holder.tv_name.text = device.name
        if (hasInit) {
            holder.tv_has_init.text = "已初始化"
            holder.tv_has_init.background = context.getDrawable(R.drawable.button_initial_bg)
            holder.tv_has_init.setTextColor(Color.parseColor("#666666"))
        } else {
            if (context is DeviceListActivity) {
                context.delAes(device.address)
            }
            holder.tv_has_init.text = "初始化"
            holder.tv_has_init.background = context.getDrawable(R.drawable.button_uninitial_bg)
            holder.tv_has_init.setTextColor(Color.WHITE)
        }
        holder.itemView.setOnClickListener {
            itemClick.invoke(device, "00000000", hasInit)
        }

        holder.tv_has_init.setOnClickListener {
            if (hasInit) {
                itemClick.invoke(device, "00000000", hasInit)
            } else {
                initClick.invoke(device)
            }
        }
    }

    fun update(map: HashMap<String, ScanResult>) {
        list.clear()
        map.forEach {
            list.add(it.value)
        }
        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

}

class DeviceListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tv_mac = itemView.findViewById<TextView>(R.id.tv_device_mac)
    val tv_name = itemView.findViewById<TextView>(R.id.tv_device_name)
    val tv_has_init = itemView.findViewById<TextView>(R.id.tv_has_init)
}