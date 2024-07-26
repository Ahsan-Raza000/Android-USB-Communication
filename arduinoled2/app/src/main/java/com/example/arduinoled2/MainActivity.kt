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
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private val deviceFilterVendorId = 0x1A86 //  Arduino's vendor ID
    private val deviceFilterProductId = 0x7523   //  Arduino's product ID
    private val actionUsbPermission = "com.example.arduinoled2.USB_PERMISSION"
        private val TAG = "arduinoled2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton: Button = findViewById(R.id.connect_button)
        val disconnectButton: Button = findViewById(R.id.disconnect_button)
        val ledButton: Button = findViewById(R.id.led_button)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        connectButton.setOnClickListener { connectToArduino() }
        disconnectButton.setOnClickListener { disconnectFromArduino() }
        ledButton.setOnClickListener { toggleLED() }

        val filter = IntentFilter(actionUsbPermission)
        registerReceiver(usbReceiver, filter)
    }

    private fun connectToArduino() {
        try {
            val usbDevices = usbManager.deviceList
            val device = usbDevices.values.find {
                it.vendorId == deviceFilterVendorId && it.productId == deviceFilterProductId
            }

            device?.let {
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission).apply {
                    putExtra(UsbManager.EXTRA_DEVICE, it) // Add UsbDevice as an extra
                }, PendingIntent.FLAG_IMMUTABLE)
                usbManager.requestPermission(it, permissionIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Arduino", e)
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

                    runOnUiThread {
                        findViewById<Button>(R.id.connect_button).isEnabled = false
                        findViewById<Button>(R.id.disconnect_button).isEnabled = true
                        findViewById<Button>(R.id.led_button).isEnabled = true
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error setting up connection", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
        }
    }

    private fun disconnectFromArduino() {
        try {
            usbSerialPort?.close()
            usbSerialPort = null

            findViewById<Button>(R.id.connect_button).isEnabled = true
            findViewById<Button>(R.id.disconnect_button).isEnabled = false
            findViewById<Button>(R.id.led_button).isEnabled = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Arduino", e)
        }
    }

    private fun toggleLED() {
        usbSerialPort?.let {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    it.write("toggle".toByteArray(), 1000)
                } catch (e: IOException) {
                    Log.e(TAG, "Error toggling LED", e)
                }
            }
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


/*
package com.example.arduinoled2


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var usbManager: UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private val deviceFilterVendorId = 0x1A86 // Arduino vendor ID LOOK FOR HARWARES VENDOR AND PRODUCT ID AND UPDATE HERE
    private val deviceFilterProductId = 0x7523// Arduino product ID
    private val actionUsbPermission = "com.example.arduinoled2.USB_PERMISSION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton: Button = findViewById(R.id.connect_button)
        val disconnectButton: Button = findViewById(R.id.disconnect_button)
        val ledButton: Button = findViewById(R.id.led_button)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        connectButton.setOnClickListener { connectToArduino() }
        disconnectButton.setOnClickListener { disconnectFromArduino() }
        ledButton.setOnClickListener { toggleLED() }
    }

    private fun connectToArduino() {
        val usbDevices = usbManager.deviceList
        val device = usbDevices.values.find {
            it.vendorId == deviceFilterVendorId && it.productId == deviceFilterProductId
        }

        device?.let {
            val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(actionUsbPermission), 0)
            usbManager.requestPermission(it, permissionIntent)

            if (usbManager.hasPermission(it)) {
                setupConnection(it)
            }
        }
    }

    private fun setupConnection(device: UsbDevice) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        driver?.let {
            usbSerialPort = it.ports[0]

            usbSerialPort?.let { port ->
                try {
                    port.open(usbManager.openDevice(device))
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

                    runOnUiThread {
                        findViewById<Button>(R.id.connect_button).isEnabled = false
                        findViewById<Button>(R.id.disconnect_button).isEnabled = true
                        findViewById<Button>(R.id.led_button).isEnabled = true
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun disconnectFromArduino() {
        usbSerialPort?.close()
        usbSerialPort = null

        findViewById<Button>(R.id.connect_button).isEnabled = true
        findViewById<Button>(R.id.disconnect_button).isEnabled = false
        findViewById<Button>(R.id.led_button).isEnabled = false
    }

    private fun toggleLED() {
        usbSerialPort?.let {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    it.write("toggle".toByteArray(), 1000)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
*/
