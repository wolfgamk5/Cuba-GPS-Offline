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
import com.example.data.RouteResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
            accuracyMeters = 5f,
            isMockOrSimulated = false
        )
    )
    val locationFlow: StateFlow<GpsLocationData> = _locationFlow.asStateFlow()

    private val _compassHeading = MutableStateFlow(0f)
    val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

    private var isTracking = false
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Sensor matrices for compass
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processAndroidLocation(location, isSimulated = false)
        }
    }

    private val legacyLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            processAndroidLocation(location, isSimulated = false)
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
        stopSimulation()
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
        stopSimulation()
    }

    private fun processAndroidLocation(location: Location, isSimulated: Boolean) {
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
            isMockOrSimulated = isSimulated,
            altitudeMeters = location.altitude
        )
    }

    /**
     * GPS Route Simulation: Drives smoothly along a polyline route for testing navigation anywhere
     */
    fun startSimulationAlongRoute(route: RouteResult, speedMultiplier: Float = 1.0f) {
        stopTracking()
        stopSimulation()

        val polyline = route.polyline
        if (polyline.size < 2) return

        simulationJob = scope.launch {
            var currentSegment = 0
            var progress = 0.0

            while (currentSegment < polyline.size - 1) {
                val p1 = polyline[currentSegment]
                val p2 = polyline[currentSegment + 1]
                val segDistance = p1.distanceTo(p2)
                val bearing = p1.bearingTo(p2)

                // Simulated speed: 85 km/h on highways (~23.6 m/s)
                val targetSpeedKmh = 85f * speedMultiplier
                val metersPerSecond = (targetSpeedKmh / 3.6f)
                val updateIntervalSec = 0.5f // update every 500ms
                val stepMeters = metersPerSecond * updateIntervalSec

                val totalSteps = (segDistance / stepMeters).coerceAtLeast(2.0)
                var step = 0

                while (step <= totalSteps) {
                    val ratio = (step / totalSteps).coerceIn(0.0, 1.0)
                    val simLat = p1.lat + (p2.lat - p1.lat) * ratio
                    val simLon = p1.lon + (p2.lon - p1.lon) * ratio

                    // Add slight realistic GPS jitter/speed variation
                    val currentSpeed = (targetSpeedKmh + (Math.sin(step.toDouble()) * 4)).toFloat().coerceAtLeast(30f)

                    _locationFlow.value = GpsLocationData(
                        point = GeoPoint(simLat, simLon, "Vehículo Simulado"),
                        speedKmh = (currentSpeed * 10).roundToInt() / 10f,
                        bearing = bearing,
                        accuracyMeters = 3.5f,
                        isMockOrSimulated = true,
                        altitudeMeters = 24.0 + (step % 5)
                    )

                    delay(500)
                    step++
                }

                currentSegment++
            }

            // Arrived at destination
            val dest = polyline.last()
            _locationFlow.value = GpsLocationData(
                point = dest,
                speedKmh = 0f,
                bearing = _locationFlow.value.bearing,
                accuracyMeters = 2f,
                isMockOrSimulated = true
            )
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun isSimulating(): Boolean = simulationJob?.isActive == true

    fun setManualLocation(point: GeoPoint) {
        _locationFlow.value = GpsLocationData(
            point = point,
            speedKmh = 0f,
            bearing = _locationFlow.value.bearing,
            accuracyMeters = 5f,
            isMockOrSimulated = true
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
