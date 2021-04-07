package com.miracle.nrfblesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.miracle.lib_ble.BluetoothManager
import com.miracle.lib_ble.callback.BleCallback
import com.miracle.lib_ble.constants.TEST_AES_KEY
import com.miracle.lib_ble.utils.DataUtil
import kotlinx.android.synthetic.main.activity_scan_reslut.*

/**
 * Created with Android Studio
 * Talk is Cheap
 *
 * @author: chenxukun
 * @date: 3/31/21
 * @desc:
 */
class ScanResultActivity: AppCompatActivity() {

    private val adapter by lazy {
        MyAdapter(this, arrayListOf()) { device, password ->
            BluetoothManager.openDoor(device.address, password, DataUtil.bytes2Hex(TEST_AES_KEY))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_reslut)

        rv_list.layoutManager = LinearLayoutManager(this)
        rv_list.adapter = adapter

        BluetoothManager.registerBleCallback(object : BleCallback {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {

            }

            override fun onScanFailed(errorCode: Int) {

            }

            override fun onScanTimeout(isEmpty: Boolean) {

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

            }

            override fun onBleServicesDiscovered(gatt: BluetoothGatt) {

            }

            override fun onCharacteristicWrite(success: Boolean, value: ByteArray) {

            }

            override fun onFailed(code: Int, msg: String?) {

            }

            override fun onGetAESKey(byteArray: ByteArray) {

            }

            override fun isDeviceInitial(init: Boolean) {

            }

            override fun onOpenDoorSuccess(data: ByteArray) {

            }

        })

        BluetoothManager.startScan()
    }
}

class MyAdapter(private val context: Context, private val list: ArrayList<ScanResult>, private val itemClick: (device: BluetoothDevice, password: String) -> Unit) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_scan_result, null))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val device = list[position].device
        holder.tv_mac.text = device.address
        holder.tv_name.text = device.name
        holder.itemView.setOnClickListener {
            val mac = device.address.replace(":", "")
            itemClick.invoke(device, "00000000")
        }
    }

    fun update(map: HashMap<String, ScanResult>) {
        list.clear()
        map.forEach {
            list.add(it.value)
        }
        notifyDataSetChanged()
    }

}

class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {
    val tv_mac = itemView.findViewById<TextView>(R.id.tv_mac)
    val tv_name = itemView.findViewById<TextView>(R.id.tv_name)
}