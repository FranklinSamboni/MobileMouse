package com.ex.franklinsamboni.mobilemouse

import android.content.Context
import android.databinding.DataBindingUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ex.franklinsamboni.mobilemouse.databinding.ActivityMainBinding
import com.ex.franklinsamboni.mobilemouse.models.Plane

class MainActivity : AppCompatActivity(), SensorEventListener {

    lateinit var mSensorManager: SensorManager
    lateinit var mSensorAcc: Sensor
    lateinit var mSensorGyr: Sensor
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.accelerometer = Plane()
        binding.gyroscope = Plane()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorGyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mSensorGyr, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event != null){
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                binding.accelerometer = Plane(event.values[0],event.values[1],event.values[2])
            }
            else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                binding.gyroscope = Plane(event.values[0],event.values[1],event.values[2])
            }
        }

    }

}
