package com.ex.franklinsamboni.mobilemouse

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.databinding.DataBindingUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.widget.ArrayAdapter
import com.ex.franklinsamboni.mobilemouse.databinding.ActivityMainBinding
import com.ex.franklinsamboni.mobilemouse.models.EchoClient
import com.ex.franklinsamboni.mobilemouse.models.Plane
import java.util.*
import android.os.AsyncTask
import android.os.AsyncTask.execute
import org.json.JSONObject


const val REQUEST_ENABLE_BT = 100
const val REQUEST_VISIBILITY_BT = 101
const val WINDOW_LEGTH = 20

class MainActivity : AppCompatActivity(), SensorEventListener {


    lateinit var mSensorManager: SensorManager
    lateinit var mSensorAcc: Sensor
    lateinit var mSensorGyr: Sensor
    lateinit var binding: ActivityMainBinding
    lateinit var mBluetoothAdapter: BluetoothAdapter
    lateinit var mArrayAdapter: ArrayAdapter<String>
    var knowDevices: MutableList<String> = mutableListOf()

    lateinit var mWindowManager: WindowManager
    lateinit var mDisplay: Display

    lateinit var echoClient : EchoClient

    var accX: Float = 0.0f
    var accY: Float = 0.0f
    var mPosX: Float = 0.0f
    var mPosY: Float = 0.0f
    var mVelX: Float = 0.0f
    var mVelY: Float = 0.0f
    var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.accelerometer = Plane()
        binding.gyroscope = Plane()

        mArrayAdapter = ArrayAdapter(this, R.layout.item_text, android.R.id.text1, knowDevices)
        binding.listView.adapter = mArrayAdapter

        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplay = mWindowManager.getDefaultDisplay()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //mSensorGyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        echoClient = EchoClient()

        /*mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(mBluetoothAdapter == null){
            print("El dispositivo no es compatible para usar Bluetooth")
        }
        else{
            /*if(!mBluetoothAdapter.isEnabled){
                var enableBt : Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBt,REQUEST_ENABLE_BT)
            }else{
                searchDevices()
            }*/
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivityForResult(discoverableIntent,REQUEST_VISIBILITY_BT)
        }

        //val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        //registerReceiver(receiver,filter)*/


    }


    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL)
        //mSensorManager.registerListener(this, mSensorGyr, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        echoClient.close()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
                return

            when (mDisplay.getRotation()) {
                Surface.ROTATION_0 -> {
                    accX = event.values[0]
                    accY = event.values[1]
                }
                Surface.ROTATION_90 -> {
                    accX = -event.values[1]
                    accY = event.values[0]
                }
                Surface.ROTATION_180 -> {
                    accX = -event.values[0]
                    accY = -event.values[1]
                }
                Surface.ROTATION_270 -> {
                    accX = event.values[1]
                    accY = -event.values[0]
                }
            }

            val date: Calendar = Calendar.getInstance()
            if (lastTime != 0L) {
                val currentTime = date.timeInMillis
                var dT: Float = (currentTime - lastTime) / 1000.0f

                mPosX = mVelX * dT + accX * dT * dT / 2
                mPosY = mVelY * dT + accY * dT * dT / 2

                if(mPosX > 1.0 || mPosY > 1.0){
                    print(mPosX)
                }
                //mVelX += accX * dT
                //mVelY += accY * dT

            }
            binding.gyroscope = Plane(mPosX, mPosY, 0.0f)
            binding.accelerometer = Plane(accX, accY, 0.0f)
            lastTime = date.timeInMillis

            val json = JSONObject()
            json.put( "accX",accX)
            json.put("accY",accY)
            SendMessageTask().execute(json.toString())
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            searchDevices()
        } else if (requestCode == REQUEST_VISIBILITY_BT) {
            print(resultCode)
        }
    }

    private fun searchDevices() {
        val pairedDevices: Set<BluetoothDevice> = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                mArrayAdapter.add(device.name + "\n" + device.address)
            }
        }

        val started = mBluetoothAdapter.startDiscovery()
        if (started) {
            print(started)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                val device: BluetoothDevice =
                    intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                mArrayAdapter.add(device.name + "\n" + device.address)
            }
        }
    }

    private inner class SendMessageTask : AsyncTask<String, Void, Void>() {
        override fun doInBackground(vararg jsonA: String): Void? {

            echoClient.sendEcho(jsonA[0])
            return null
        }
    }
}
