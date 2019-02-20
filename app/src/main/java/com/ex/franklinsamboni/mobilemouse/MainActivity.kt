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
import android.widget.ArrayAdapter
import com.ex.franklinsamboni.mobilemouse.databinding.ActivityMainBinding
import com.ex.franklinsamboni.mobilemouse.models.Plane
import java.util.*

const val REQUEST_ENABLE_BT = 100
const val REQUEST_VISIBILITY_BT = 101
const val WINDOW_LEGTH = 20

class MainActivity : AppCompatActivity(), SensorEventListener {


    lateinit var mSensorManager: SensorManager
    lateinit var mSensorAcc: Sensor
    lateinit var mSensorGyr: Sensor
    lateinit var binding: ActivityMainBinding
    lateinit var mBluetoothAdapter : BluetoothAdapter
    lateinit var mArrayAdapter: ArrayAdapter<String>
    var knowDevices: MutableList<String> = mutableListOf()

    var accList : MutableList<Plane> = mutableListOf()
    var timeList : MutableList<Long> = mutableListOf()
    var distList : MutableList<Plane> = mutableListOf()
    var gravity :  MutableList<Float> = mutableListOf(0.0f,0.0f,9.8f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.accelerometer = Plane()
        binding.gyroscope = Plane()

        mArrayAdapter = ArrayAdapter(this,R.layout.item_text, android.R.id.text1,knowDevices)
        binding.listView.adapter = mArrayAdapter


        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //mSensorGyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event != null){
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

                val alpha: Float = 0.8f
                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                // Remove the gravity contribution with the high-pass filter.
                val linear_acceleration : MutableList<Float> = mutableListOf()
                linear_acceleration.add(event.values[0] - gravity[0])
                linear_acceleration.add(event.values[1] - gravity[1])
                linear_acceleration.add(event.values[2] - gravity[2])


                val acc = Plane(event.values[0],event.values[1],event.values[2])
                val date: Calendar = Calendar.getInstance()
                timeList.add(date.timeInMillis)
                accList.add(acc)

                if(accList.size == timeList.size && accList.size > (WINDOW_LEGTH +1)){
                    var size = accList.size
                    for( i in accList.indices ){
                        val iio = (accList.size - (WINDOW_LEGTH +1) )
                        val iif = iio + WINDOW_LEGTH
                        if(i in iio..(iif - 1)){
                            var deltaT = timeList[i+1] - timeList[i]
                            var deltaD = deltaT.toFloat()
                            deltaD = (deltaD/1000.0f)
                            val distX = getDistance(accList[i].x,deltaD)
                            val distY = getDistance(accList[i].y,deltaD)
                            val distZ = getDistance(accList[i].z,deltaD)
                            val dist = Plane(distX,distY,distZ)
                            distList.add(dist)
                            binding.gyroscope = dist
                        }
                    }
                }

                binding.accelerometer = acc
            }
            else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                binding.gyroscope = Plane(event.values[0],event.values[1],event.values[2])
            }
        }
    }

    fun getDistance(acc: Float, time: Float): Float {
        val time2 = (time * time)
        val res = acc * time2
        val to = (0.5f) * res
        return  to
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT){
            searchDevices()
        }
        else if(requestCode == REQUEST_VISIBILITY_BT){
            print(resultCode)
        }
    }

    private fun searchDevices() {
        val pairedDevices: Set<BluetoothDevice> = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0){
            for(device in pairedDevices){
                mArrayAdapter.add(device.name+ "\n"+device.address)
            }
        }

        val started = mBluetoothAdapter.startDiscovery()
        if(started){
            print(started)
        }
    }

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = intent.action
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                val device : BluetoothDevice = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                mArrayAdapter.add(device.name+ "\n"+device.address)
            }
        }
    }
}
