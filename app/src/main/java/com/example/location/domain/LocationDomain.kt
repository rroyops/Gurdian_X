package com.example.location.domain

import com.example.core.common.AppResult
import kotlinx.coroutines.flow.Flow

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val timestamp: Long,
    val provider: String,
    val isMock: Boolean = false
) {
    val isFresh: Boolean get() = (System.currentTimeMillis() - timestamp) < 60_000L
}

interface LocationProvider {
    suspend fun getLastKnownLocation(): AppResult<LocationSnapshot>
    fun observeLocationUpdates(intervalMs: Long = 5000L): Flow<LocationSnapshot>
    fun isLocationPermissionGranted(): Boolean
    fun isGpsEnabled(): Boolean
}
