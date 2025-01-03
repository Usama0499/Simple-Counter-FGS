package com.android.servicedemo.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: AppCompatActivity) {

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null
    private var onNotificationPermissionResult: ((Boolean) -> Unit)? = null

    init {
        setupPermissionLaunchers()
    }

    private fun setupPermissionLaunchers() {
        // Location permission launcher
        locationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allLocationPermissionsGranted = permissions.entries.all { it.value }
            onLocationPermissionResult?.invoke(allLocationPermissionsGranted)
        }

        // Notification permission launcher (Android 13 and above)
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onNotificationPermissionResult?.invoke(isGranted)
        }
    }

    fun requestLocationPermission(onResult: (Boolean) -> Unit) {
        onLocationPermissionResult = onResult

        val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        if (hasPermissions(permissions)) {
            onResult(true)
        } else {
            locationPermissionLauncher.launch(permissions)
        }
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        onNotificationPermissionResult = onResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                onResult(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Notification permission not required for Android < 13
            onResult(true)
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(this)
        }
    }

    fun openLocationSettings() {
        activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    companion object {
        fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}