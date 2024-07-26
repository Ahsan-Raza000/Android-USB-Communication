package com.example.arduinoled2

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var readingsTextView: TextView
    private lateinit var usbManager: UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private val actionUsbPermission = "com.example.arduinoled2.USB_PERMISSION"
    private val TAG = "arduinoled2"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        readingsTextView = findViewById(R.id.readings_text_view)
        val connectButton: Button = findViewById(R.id.connect_button)
        val disconnectButton: Button = findViewById(R.id.disconnect_button)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        connectButton.setOnClickListener { connectToDevice() }
        disconnectButton.setOnClickListener { disconnectFromDevice() }

        val filter = IntentFilter(actionUsbPermission)
        registerReceiver(usbReceiver, filter)
    }

    private fun connectToDevice() {
        try {
            val usbDevices = usbManager.deviceList
            if (usbDevices.isEmpty()) {
                Log.e(TAG, "No USB devices connected")
                return
            }

            val device = usbDevices.values.firstOrNull()
            device?.let {
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission), PendingIntent.FLAG_MUTABLE)
                usbManager.requestPermission(it, permissionIntent)
            } ?: run {
                Log.e(TAG, "No suitable USB device found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to USB device", e)
        }
    }


private fun setupConnection(device: UsbDevice) {
    try {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        driver?.let {
            usbSerialPort = it.ports[0]

            usbSerialPort?.let { port ->
                port.open(usbManager.openDevice(device))
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

                // Start reading from Arduino
                CoroutineScope(Dispatchers.IO).launch {
                    readFromArduino()
                }

                runOnUiThread {
                    findViewById<Button>(R.id.connect_button).isEnabled = false
                    findViewById<Button>(R.id.disconnect_button).isEnabled = true
                }
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error setting up connection", e)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error", e)
    }
}
    private suspend fun readFromArduino() {
        val buffer = ByteArray(1024)
        while (usbSerialPort != null) {
            try {
                val numBytesRead = usbSerialPort!!.read(buffer, 1000)
                if (numBytesRead > 0) {
                    val data = String(buffer, 0, numBytesRead).trim()
                    Log.d(TAG, "Received data: $data")
                    runOnUiThread {
                        readingsTextView.text = data
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading from Arduino", e)
            }
        }
    }






    private fun disconnectFromDevice() {
        try {
            usbSerialPort?.close()
            usbSerialPort = null

            runOnUiThread {
                findViewById<Button>(R.id.connect_button).isEnabled = true
                findViewById<Button>(R.id.disconnect_button).isEnabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from USB device", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == actionUsbPermission) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device == null) {
                        Log.e(TAG, "UsbDevice is null")
                        return
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        setupConnection(device)
                    } else {
                        Log.e(TAG, "Permission denied for device $device")
                    }
                }
            }
        }
    }
}

