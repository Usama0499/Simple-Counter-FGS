package com.android.servicedemo

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.servicedemo.permissions.PermissionHandler
import com.android.servicedemo.service.LocationWithCounterService

class MainActivity : AppCompatActivity() {
    private lateinit var toggleButton: Button
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        permissionHandler = PermissionHandler(this)

        toggleButton = findViewById(R.id.toggleButton)
        updateButtonState()

        toggleButton.setOnClickListener {
            checkPermissions()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun updateButtonState() {
        toggleButton.text = if (isServiceRunning()) {
            "Resume Service"
        } else {
            "Start Service"
        }
    }

    private fun isServiceRunning(): Boolean {
        return LocationWithCounterService.Companion.isRunning
    }

    private fun checkPermissions() {
        // Check location permission
        permissionHandler.requestLocationPermission { isLocationGranted ->
            if (isLocationGranted) {
                if (permissionHandler.isLocationEnabled()) {
                    // Proceed to check notification permission
                    checkNotificationPermission()
                } else {
                    Toast.makeText(this, "Enable GPS Location", Toast.LENGTH_SHORT).show()
                    permissionHandler.openLocationSettings()
                }
            } else {
                handlePermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun checkNotificationPermission() {
        permissionHandler.requestNotificationPermission { isNotificationGranted ->
            if (isNotificationGranted) {
                startOrResumeService()
            } else {
                handlePermissionDenied(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handlePermissionDenied(permission: String) {
        if (PermissionHandler.Companion.shouldShowPermissionRationale(this, permission)) {
            // Show rationale to user
            showPermissionRationale(permission)
        } else {
            permissionHandler.openAppSettings()
        }
    }

    private fun showPermissionRationale(permission: String) {
        val message = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location permission is required to access GPS features."
            Manifest.permission.POST_NOTIFICATIONS -> "Notification permission is required to show notifications."
            else -> "This permission is required for the app to function properly."
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                permissionHandler.openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun startOrResumeService() {
        val serviceIntent = Intent(this, LocationWithCounterService::class.java).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        startActivity(Intent(this, LocationWithCounterActivity::class.java))
    }

}