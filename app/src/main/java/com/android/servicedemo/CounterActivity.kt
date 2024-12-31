package com.android.servicedemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CounterActivity : AppCompatActivity() {
    private val counterTextView by lazy { findViewById<TextView>(R.id.counterTextView) }
    private val stopButton by lazy { findViewById<Button>(R.id.stopButton) }
    private val viewModel by lazy { CounterViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter)
        stopButton.setOnClickListener { stopCounter() }
        observeCounter()
    }

    override fun onResume() {
        super.onResume()
        if (!CounterService.isRunning) {
            finish()
            return
        }
    }

    private fun observeCounter() {
        lifecycleScope.launch {
            viewModel.counterFlow.collect { count ->
                updateCounterDisplay(count)
            }
        }
    }

    private fun updateCounterDisplay(count: Int) {
        counterTextView.text = count.toString()
    }

    private fun stopCounter() {
        stopService(Intent(this, CounterService::class.java))
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}
