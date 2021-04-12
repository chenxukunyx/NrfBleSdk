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
import com.miracle.lib_ble.utils.BleLog
import com.miracle.lib_ble.utils.BleUtil
import com.miracle.lib_ble.utils.DataUtil
import com.tencent.mmkv.MMKV
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
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
            initClick = {
                loading.show()
                mac = it.address
                BluetoothManager.initialDevice(it.address)
            },
            openDoor = { mac, aesKey ->
                if (aesKey.isNullOrEmpty()) {
                    val msg = "设备不是在本机进行的初始化操作，未能获取到AesKey，不能进行开门操作"
                    showHintDialog(msg)
                    return@DeviceListAdapter
                }
                loading.show()
                BluetoothManager.openDoor(mac, "00000000", aesKey)
            },
            modify = { mac, aesKey ->
                if (aesKey.isNullOrEmpty()) {
                    val msg = "设备不是在本机进行的初始化操作，未能获取到AesKey，禁止此操作"
                    showHintDialog(msg)
                    return@DeviceListAdapter
                }
                showModifyDialog(mac)
            }
        )
    }

    private val animation by lazy {
        RotateAnimation(
            0f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
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

        checkLocationPermission()
    }

    private fun inject() {
        refresh.setOnClickListener {
            checkLocationPermission()
        }

        iv_initialied_list.setOnClickListener {
            InitialiedListActivity.start(this)
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
                    checkLocationPermission()
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
                val bean = DeviceBean(mac!!, DataUtil.bytes2Hex(byteArray), "未命名")
                val json = JsonUtil.toJson(bean)
                CacheManager.put(mac!!, json)
                runOnUiThread {
                    loading.hide()
                    showToast("初始化成功")
                    checkLocationPermission()
                }
            }

            override fun isDeviceInitial(init: Boolean) {

            }

            override fun onOpenDoorSuccess(data: ByteArray) {
                runOnUiThread {
                    loading.hide()
                    showToast("开门成功")
                    checkLocationPermission()
                }
            }

            override fun onConnectTimeout() {
                runOnUiThread {
                    loading.hide()
                    showToast("开门失败")
                }
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showHintDialog(msg: String) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.cancel()
            }
            .create()
        dialog.show()
    }

    private fun showModifyDialog(mac: String) {

        val json = CacheManager.get(mac)
        val bean = JsonUtil.fromJson(json, DeviceBean::class.java)
        val dialog = ModifyDialog(this, bean.nickname) { nickname ->

            bean.nickname = nickname
            val ret = JsonUtil.toJson(bean)
            CacheManager.put(mac, ret)
            startScan()
        }
        dialog.show()
    }

//    private fun showDeviceInfoDialog(device: BluetoothDevice, password: String) {
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("mac: ${device.address}")
//            .setMessage("aesKey: ${CacheManager.get(device.address)}")
//            .setPositiveButton("尝试开门") { dialog, which ->
//                if (CacheManager.get(device.address).isNullOrEmpty()) {
//                    val msg = "设备不是在本机进行的初始化操作，未能获取到AesKey，可尝试对设备进行Reset操作"
//                    showHintDialog(msg)
//                    return@setPositiveButton
//                }
//                dialog.cancel()
//                loading.show()
//                BluetoothManager.openDoor(
//                    device.address,
//                    password,
//                    CacheManager.get(device.address)!!
//                )
//            }
//            .setNegativeButton("复制信息") { dialog, which ->
//                if (CacheManager.get(device.address).isNullOrEmpty()) {
//                    val msg = "设备不是在本机进行的初始化操作，未能获取到AesKey，可尝试对设备进行Reset操作"
//                    showHintDialog(msg)
//                    return@setNegativeButton
//                }
//                dialog.cancel()
////                ClipManager.setTextToClip("${device.address}-${sp.getString(device.address, "")}")
////                showToast("复制成功")
//            }
//            .create()
//        dialog.show()
//    }

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

    private fun checkLocationPermission() {
        if (AndPermission.hasPermissions(this, Permission.ACCESS_FINE_LOCATION)) {
            startScan()
        } else {
            AndPermission.with(this)
                .runtime()
                .permission(Permission.ACCESS_FINE_LOCATION)
                .onDenied {
                    Toast.makeText(this, "储存权限已被禁止,请到权限管理授权", Toast.LENGTH_LONG).show()
                }
                .onGranted {
                    startScan()
                }
                .start()
        }
    }
}

class DeviceListAdapter(
    private val context: Context,
    private val list: ArrayList<ScanResult>,
    private val openDoor: (mac: String, aesKey: String?) -> Unit,
    private val modify: (mac: String, aesKey: String?) -> Unit,
    private val initClick: (device: BluetoothDevice) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_UNINITIAL = 0
    private val TYPE_INITIAL = 1

    override fun getItemViewType(position: Int): Int {
        val scanResult = list[position]
//        val hasInit = scanResult.scanRecord!!.bytes[21] and 4.toByte() != 4.toByte()
        val hasInit = BleUtil.isDeviceInit(scanResult.scanRecord!!.bytes)
        return if (hasInit) TYPE_INITIAL else TYPE_UNINITIAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        BleLog.i("itemType: $viewType")
        return if (viewType == TYPE_UNINITIAL) UninitialViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_device_uninitial, null)
        )
        else InitialViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_device_initial, null)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is InitialViewHolder -> handleInitDevice(holder, position)
            is UninitialViewHolder -> handleUninitDevice(holder, position)
        }
    }

    private fun handleInitDevice(holder: InitialViewHolder, position: Int) {
        val scanResult = list[position]
        val device = scanResult.device
        val broadcast = scanResult.scanRecord!!.bytes
        Log.i(
            "DeviceListActivity",
            "mac: ${device.address}, broadcast: ${DataUtil.bytes2Hex(broadcast)}"
        )
        holder.tv_mac.text = device.address
        holder.tv_name.text = device.name
        holder.tv_has_init.text = "已初始化"
        holder.tv_delete.visibility = View.GONE
        var aesKey: String?
        if (CacheManager.containsKey(device.address)) {
            val json = CacheManager.get(device.address)
            val bean = JsonUtil.fromJson(json, DeviceBean::class.java)
            aesKey = bean.aesKey
            holder.tv_nickname.text = bean.nickname
        } else {
            aesKey = null
            holder.tv_nickname.text = "未命名"
        }
        holder.tv_open_door.setOnClickListener {
            openDoor.invoke(device.address, aesKey)
        }
        holder.tv_modify_device_nickname.setOnClickListener {
            modify.invoke(device.address, aesKey)
        }
    }

    private fun handleUninitDevice(holder: UninitialViewHolder, position: Int) {
        val scanResult = list[position]
        val device = scanResult.device
        val broadcast = scanResult.scanRecord!!.bytes
        BleLog.i(
            "DeviceListActivity",
            "mac: ${device.address}, broadcast: ${DataUtil.bytes2Hex(broadcast)}"
        )
        holder.tv_mac.text = device.address
        holder.tv_name.text = device.name
        CacheManager.remove(device.address)
        holder.tv_has_init.text = "初始化"

        holder.tv_has_init.setOnClickListener {
            initClick.invoke(device)
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

class UninitialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tv_mac = itemView.findViewById<TextView>(R.id.tv_device_mac)
    val tv_name = itemView.findViewById<TextView>(R.id.tv_device_name)
    val tv_has_init = itemView.findViewById<TextView>(R.id.tv_has_init)
}

class InitialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tv_mac = itemView.findViewById<TextView>(R.id.tv_device_mac)
    val tv_name = itemView.findViewById<TextView>(R.id.tv_device_name)
    val tv_has_init = itemView.findViewById<TextView>(R.id.tv_has_init)
    val tv_nickname = itemView.findViewById<TextView>(R.id.tv_nickname)
    val tv_open_door = itemView.findViewById<TextView>(R.id.tv_open_door)
    val tv_modify_device_nickname = itemView.findViewById<TextView>(R.id.tv_modify_device_nickname)
    val tv_delete = itemView.findViewById<TextView>(R.id.tv_delete)
}