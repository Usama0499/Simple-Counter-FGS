package com.android.servicedemo

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.servicedemo.service.LocationWithCounterService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationWithCounterActivity : AppCompatActivity() {
    private val counterTextView by lazy { findViewById<TextView>(R.id.counterTextView) }
    private val latLngTextView by lazy { findViewById<TextView>(R.id.latLngTextView) }
    private val stopButton by lazy { findViewById<Button>(R.id.stopButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)
        stopButton.setOnClickListener { stopService() }
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        if (!LocationWithCounterService.Companion.isRunning) {
            finish()
            return
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            LocationWithCounterService.Companion.counterFlow.collect { count ->
                updateCounterDisplay(count)
            }
        }
        lifecycleScope.launch {
            LocationWithCounterService.Companion.locationFlow.collectLatest { location ->
                updateLatLngDisplay(location)
            }
        }
    }

    private fun updateCounterDisplay(count: Int) {
        counterTextView.text = count.toString()
    }

    private fun updateLatLngDisplay(location: Location?) {
        latLngTextView.text = location?.longitude.toString() + ", " + location?.latitude.toString()
    }

    private fun stopService() {
        stopService(Intent(this, LocationWithCounterService::class.java))
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}
