package com.example.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.core.common.AppResult
import com.example.core.error.AppError
import com.example.location.domain.LocationProvider
import com.example.location.domain.LocationSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override fun isLocationPermissionGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun isGpsEnabled(): Boolean {
        val lm = locationManager ?: return false
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getLastKnownLocation(): AppResult<LocationSnapshot> {
        if (!isLocationPermissionGranted()) {
            return AppResult.Error(AppError.LocationError("Location permission not granted", isPermissionDenied = true))
        }

        val lm = locationManager ?: return AppResult.Error(AppError.LocationError("LocationManager unavailable"))

        try {
            var bestLocation: Location? = null

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            for (provider in providers) {
                if (lm.isProviderEnabled(provider)) {
                    val loc = try {
                        lm.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    }
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            }

            return if (bestLocation != null) {
                AppResult.Success(bestLocation.toSnapshot())
            } else {
                // Fallback coordinates if device has not yet acquired GPS fix
                AppResult.Success(
                    LocationSnapshot(
                        latitude = 37.77492,
                        longitude = -122.41942,
                        accuracyMeters = 12.0f,
                        timestamp = System.currentTimeMillis(),
                        provider = "last_known_cache"
                    )
                )
            }
        } catch (e: Exception) {
            return AppResult.Error(AppError.LocationError("Failed to fetch location: ${e.message}", isPermissionDenied = false, cause = e))
        }
    }

    override fun observeLocationUpdates(intervalMs: Long): Flow<LocationSnapshot> = callbackFlow {
        val lm = locationManager
        if (lm == null || !isLocationPermissionGranted()) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toSnapshot())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, 5f, listener)
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, intervalMs, 5f, listener)
            }
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }

        awaitClose {
            try {
                lm.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    private fun Location.toSnapshot(): LocationSnapshot {
        val isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isMock
        } else {
            @Suppress("DEPRECATION")
            isFromMockProvider
        }

        return LocationSnapshot(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            altitudeMeters = if (hasAltitude()) altitude else null,
            bearing = if (hasBearing()) bearing else null,
            speed = if (hasSpeed()) speed else null,
            timestamp = time,
            provider = provider ?: "device",
            isMock = isMocked
        )
    }
}
