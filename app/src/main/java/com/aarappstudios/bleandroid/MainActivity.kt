package com.aarappstudios.bleandroid

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aarappstudios.bleandroid.databinding.ActivityMainBinding
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val blescanner by lazy { bluetoothAdapter.bluetoothLeScanner }
    private val scanSettings=ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    private val scanCallbacks=object :ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
//                val indexQuery=scanResults.indexOfFirst { it.device.address == result.device.address }
//                if (indexQuery != -1) { // A scan result already exists with the same address
////                    scanResults[indexQuery] = result
////                    scanResultAdapter.not(indexQuery)
//                } else {
                with(result.device) {

//                    Log.i("TAG", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i("ScanCallback", "onScanFailed: $errorCode")
        }
    }

    //
    private val scanResults= mutableListOf<ScanResult>()
    private val scanResultStringList= mutableListOf<String>()
    private lateinit var  scanResultAdapter:ArrayAdapter<*>

    fun addToList()
    {
        for (i in scanResults.indices)
        {
            val device=scanResults[i].device
            scanResultStringList.add(" Name ${device.name}  Adress ${device.address}")
        }
    }
//    private val scanre

    private var isScanning=false
    set(value) {
        field=value
        runOnUiThread {
            binding.scanButton.text=if (field) "Stop scan" else "Start scan"
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val isLocationPermissionGranted get() = hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private fun Context.hasPermission(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(this,permission)==PackageManager.PERMISSION_GRANTED
    }


    override fun onResume() {
        super.onResume()
        if(!bluetoothAdapter.isEnabled)
        {
            promtEnableBlutooth()
        }
    }

    private fun promtEnableBlutooth() {
        if (!bluetoothAdapter.isEnabled) {
            val btIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getResult.launch(btIntent)
        }
    }

    private val getResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode!= RESULT_OK)
        {
            promtEnableBlutooth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanResultAdapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,scanResultStringList)

        binding.scanButton.setOnClickListener {
            if (isScanning)
            {
                stopBleScanning()
            }else {

                startBle()
            }
        }
    }

    private fun startBle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        }else
        {
            scanResults.clear()
            scanResultStringList.clear()
            scanResultAdapter.notifyDataSetChanged()
            blescanner.startScan(null,scanSettings,scanCallbacks)
            isScanning=true
        }
    }

    private fun stopBleScanning()
    {
        blescanner.startScan(scanCallbacks)
        isScanning=false
    }
//    val filter = ScanFilter.Builder().setServiceUuid(
//        ParcelUuid.fromString(ENVIRONMENTAL_SERVICE_UUID.toString())
//    ).build()
    private fun requestLocationPermission() {
        if (isLocationPermissionGranted)
        {
            return
        }else
        {
            runOnUiThread {
                AlertDialog.Builder(this).setTitle("Location permission required")
                    .setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                            "location access in order to scan for BLE devices.")
                    .setPositiveButton(android.R.string.ok,DialogInterface.OnClickListener { dialogInterface, i ->
                        requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION,
                            REQUEST_LOCATION_PERMISSION_CODE)
                    })
                    .create()
                    .show()
            }
        }
    }
    private fun Activity.requestPermission(permission: String,requestCode:Int) {

        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== REQUEST_LOCATION_PERMISSION_CODE)
        {
            if (grantResults.firstOrNull()==PackageManager.PERMISSION_DENIED)
            {
                requestLocationPermission()
            }else
            {
                startBle()
            }
        }
    }

    companion object {
        private const val ENABLE_BLUTOOTH_REQUEST_CODE=1;
        private const val REQUEST_LOCATION_PERMISSION_CODE=3;
    }
}