package com.example.core.error

sealed class AppError(open val message: String, open val cause: Throwable? = null) {
    data class NetworkError(
        override val message: String,
        val isOffline: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class AuthError(
        override val message: String,
        val code: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class SecurityError(
        override val message: String,
        val threatLevel: String = "HIGH",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class EmergencyError(
        override val message: String,
        val sessionId: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class DeviceError(
        override val message: String,
        val deviceAddress: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class LocationError(
        override val message: String,
        val isPermissionDenied: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ValidationError(
        val field: String,
        override val message: String
    ) : AppError(message)

    data class SyncError(
        override val message: String,
        val retryable: Boolean = true,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class DeliveryError(
        val recipient: String,
        override val message: String,
        val channel: String = "EMAIL",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class EmailError(
        val recipient: String,
        override val message: String,
        val errorCode: String = "DELIVERY_FAILED",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
