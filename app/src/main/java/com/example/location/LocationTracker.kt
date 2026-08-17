package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.data.CubaGeographyData
import com.example.data.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GpsLocationData(
    val point: GeoPoint,
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val accuracyMeters: Float = 5f,
    val isMockOrSimulated: Boolean = false,
    val altitudeMeters: Double = 12.0
)

class LocationTracker(private val context: Context) : SensorEventListener {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _locationFlow = MutableStateFlow(
        GpsLocationData(
            point = CubaGeographyData.HAVANA_CENTER,
            speedKmh = 0f,
            bearing = 0f,
            accuracyMeters = 5f
        )
    )
    val locationFlow: StateFlow<GpsLocationData> = _locationFlow.asStateFlow()

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    private var isTracking = false

    // Sensor matrices for compass
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processAndroidLocation(location)
        }
    }

    private val legacyLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            processAndroidLocation(location)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    init {
        registerSensors()
    }

    private fun registerSensors() {
        sensorManager?.let { sm ->
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accel ->
                sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
            }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { mag ->
                sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealTracking() {
        isTracking = true

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1500L
            ).setMinUpdateIntervalMillis(800L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "Location permission missing: ${e.message}")
            fallbackToLegacyProvider()
        } catch (e: Exception) {
            fallbackToLegacyProvider()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLegacyProvider() {
        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 2f, legacyLocationListener, Looper.getMainLooper())
                } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, legacyLocationListener, Looper.getMainLooper())
                }
            }
        } catch (e: Exception) {
            Log.e("LocationTracker", "Legacy location listener failed: ${e.message}")
        }
    }

    fun stopTracking() {
        isTracking = false
        try {
            fusedClient.removeLocationUpdates(locationCallback)
            locationManager?.removeUpdates(legacyLocationListener)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun processAndroidLocation(location: Location) {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
        val bearing = if (location.hasBearing()) location.bearing else _compassHeading.value

        _locationFlow.value = GpsLocationData(
            point = GeoPoint(
                lat = location.latitude,
                lon = location.longitude,
                name = "Mi Ubicación",
                altitude = location.altitude
            ),
            speedKmh = (speedKmh * 10).roundToInt() / 10f,
            bearing = bearing,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 5f,
            altitudeMeters = location.altitude
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
            _compassHeading.value = azimuthDeg
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun cleanup() {
        stopTracking()
        sensorManager?.unregisterListener(this)
    }
}
