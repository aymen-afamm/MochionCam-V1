package com.example.motioncam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * GPS Speed Tracker - Monitors vehicle speed using GPS
 *
 * Features:
 * - Real-time speed updates from GPS
 * - Movement detection for auto-start recording
 * - Location tracking with coordinates
 * - Speed accuracy information
 * - Trip distance calculation
 */
class GPSSpeedTracker(private val context: Context) {

    companion object {
        private const val TAG = "GPSSpeedTracker"
        private const val MOVEMENT_THRESHOLD_KMH = 5 // Speed threshold to detect movement
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second updates
        private const val MIN_DISTANCE_M = 1f // Minimum distance for updates
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Current speed in MPH (converted from m/s)
    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // Current speed in KM/H
    private val _currentSpeedKmh = MutableStateFlow(0)
    val currentSpeedKmh: StateFlow<Int> = _currentSpeedKmh.asStateFlow()

    // GPS accuracy
    private val _gpsAccuracy = MutableStateFlow(0f)
    val gpsAccuracy: StateFlow<Float> = _gpsAccuracy.asStateFlow()

    // GPS Active state
    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    // Movement detection
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()

    // Current location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Trip distance in miles
    private var tripStartLocation: Location? = null
    private val _tripDistance = MutableStateFlow(0.0)
    val tripDistance: StateFlow<Double> = _tripDistance.asStateFlow()

    // Max speed during trip
    private val _maxSpeed = MutableStateFlow(0)
    val maxSpeed: StateFlow<Int> = _maxSpeed.asStateFlow()

    // Location listener
    private var locationListener: LocationListener? = null

    // Tracking state
    private var isTracking = false

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if GPS is enabled
     */
    fun isGPSEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Start tracking GPS speed
     */
    fun startTracking() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        if (isTracking) return

        try {
            // Request location updates from GPS
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    updateLocation(location)
                }

                override fun onProviderEnabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        _isGpsActive.value = true
                    }
                }

                override fun onProviderDisabled(provider: String) {
                    if (provider == LocationManager.GPS_PROVIDER) {
                        _isGpsActive.value = false
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }

            // Request GPS updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_M,
                locationListener!!,
                Looper.getMainLooper()
            )

            // Also request network location as backup
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_M,
                locationListener!!,
                Looper.getMainLooper()
            )

            // Get last known location immediately
            val lastLocation = getLastKnownLocation()
            lastLocation?.let { updateLocation(it) }

            isTracking = true
            _isGpsActive.value = true

            Log.d(TAG, "GPS tracking started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting GPS", e)
            _isGpsActive.value = false
        }
    }

    /**
     * Stop tracking GPS
     */
    fun stopTracking() {
        locationListener?.let { listener ->
            locationManager.removeUpdates(listener)
        }
        locationListener = null
        isTracking = false
        _isGpsActive.value = false

        Log.d(TAG, "GPS tracking stopped")
    }

    /**
     * Update location data
     */
    private fun updateLocation(location: Location) {
        _currentLocation.value = location

        // Speed from GPS (m/s) - convert to MPH and KM/H
        val speedMps = location.speed // meters per second
        val speedKmh = (speedMps * 3.6).toInt() // km/h
        val speedMph = (speedMps * 2.237).toInt() // mph

        _currentSpeed.value = speedMph
        _currentSpeedKmh.value = speedKmh
        _gpsAccuracy.value = location.accuracy

        // Update max speed
        if (speedMph > _maxSpeed.value) {
            _maxSpeed.value = speedMph
        }

        // Movement detection
        _isMoving.value = speedKmh > MOVEMENT_THRESHOLD_KMH

        // Calculate trip distance
        if (tripStartLocation == null) {
            tripStartLocation = location
        } else {
            val distance = tripStartLocation!!.distanceTo(location) // meters
            _tripDistance.value = distance / 1609.34 // convert to miles
        }

        Log.d(TAG, "Speed: ${speedMph}mph, Accuracy: ${location.accuracy}m, Moving: ${_isMoving.value}")
    }

    /**
     * Get last known location
     */
    private fun getLastKnownLocation(): Location? {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Reset trip statistics
     */
    fun resetTrip() {
        tripStartLocation = null
        _tripDistance.value = 0.0
        _maxSpeed.value = 0
    }

    /**
     * Get speed as flow
     * Note: StateFlow already has built-in distinctUntilChanged behavior
     */
    fun getSpeedFlow(): Flow<Int> = currentSpeed

    /**
     * Get movement state as flow
     * Note: StateFlow already has built-in distinctUntilChanged behavior
     */
    fun getMovementFlow(): Flow<Boolean> = isMoving

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopTracking()
    }
}

/**
 * Data class representing speed info
 */
data class SpeedInfo(
    val speedMph: Int = 0,
    val speedKmh: Int = 0,
    val accuracy: Float = 0f,
    val isMoving: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing trip info
 */
data class TripInfo(
    val distanceMiles: Double = 0.0,
    val maxSpeedMph: Int = 0,
    val durationSeconds: Int = 0,
    val avgSpeedMph: Int = 0
)
