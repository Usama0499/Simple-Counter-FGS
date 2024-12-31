package com.android.servicedemo

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class CounterService : Service(), CoroutineScope {
    private var counter = 0
    private val job = Job()
    private var isCountingActive = false
    private val viewModel by lazy { CounterViewModel.getInstance() }
    
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            if (!isCountingActive) {
                isCountingActive = true
                startCounting()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isCountingActive = false
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCounting() = launch {
        while (isActive && isCountingActive) {
            counter++
            updateCounter()
            delay(1000)
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
        .build()

    private fun createContentIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, CounterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createStopIntent() = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, StopServiceReceiver::class.java).apply {
            action = StopServiceReceiver.ACTION_STOP_SERVICE
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun updateCounter() {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, createNotification())
        launch {
            viewModel.updateCounter(counter)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "counter_service_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
            private set
    }
}
