package com.android.servicedemo.receiver

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import com.android.servicedemo.service.LocationWithCounterService
import com.android.servicedemo.MainActivity

class StopServiceReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP_SERVICE = "com.android.servicedemo.STOP_SERVICE"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_STOP_SERVICE) {
            context?.let {
                // Stop the service
                val serviceIntent = Intent(context, LocationWithCounterService::class.java)
                context.stopService(serviceIntent)

                // Start MainActivity with clear top flag
                val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainActivityIntent)
            }
        }
    }
}
