package com.android.servicedemo.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.android.servicedemo.LocationWithCounterActivity
import com.android.servicedemo.R
import com.android.servicedemo.receiver.StopServiceReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration.Companion.seconds

class LocationWithCounterService : Service() {

    private var counter = 0
    private val counterJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + counterJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupLocationUpdates()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                startForeground(NOTIFICATION_ID, createNotification(), FOREGROUND_SERVICE_TYPE_LOCATION)
            }else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            startLocationUpdates()
            startCounting()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CounterService", "onDestroy called")
        isRunning = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        counterJob.cancel()
        coroutineScope.coroutineContext.cancelChildren()
    }

    private fun setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    _locationFlow.value = location
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(
                LOCATION_UPDATES_INTERVAL_MS
            ).build(), locationCallback, Looper.getMainLooper()
        )
    }

    private fun startCounting()  {
        coroutineScope.launch {

            while (isActive) {
                delay(1000) // Delay for 1 second
                counter++

                getSystemService(NotificationManager::class.java)?.notify(
                    NOTIFICATION_ID,
                    createNotification()
                )
                _counterFlow.emit(counter)
                // Log or send a notification with the updated counter value
                println("Counter: $counter")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.counter_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.counter_channel_description)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }.also { channel ->
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(getString(R.string.counter_notification_title))
        .setContentText(getString(R.string.counter_value, counter))
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentIntent(createContentIntent())
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.stop),
            createStopIntent()
        )
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createContentIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, LocationWithCounterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createStopIntent() = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, StopServiceReceiver::class.java).apply {
            action = StopServiceReceiver.Companion.ACTION_STOP_SERVICE
        },
        PendingIntent.FLAG_IMMUTABLE
    )


    companion object {
        const val NOTIFICATION_CHANNEL_ID = "counter_service_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
            private set

        private val LOCATION_UPDATES_INTERVAL_MS = 1.seconds.inWholeMilliseconds

        private val _counterFlow = MutableSharedFlow<Int>(replay = 1)
        val counterFlow = _counterFlow.asSharedFlow()

        private val _locationFlow = MutableStateFlow<Location?>(null)
        var locationFlow: StateFlow<Location?> = _locationFlow
    }
}
