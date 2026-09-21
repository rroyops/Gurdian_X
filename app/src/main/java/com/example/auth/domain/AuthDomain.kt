package com.example.auth.domain

import com.example.core.common.AppResult
import kotlinx.coroutines.flow.Flow

data class UserSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
)

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Authenticating : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    val currentSession: UserSession?
    fun observeAuthState(): Flow<AuthState>
    suspend fun loginWithEmail(email: String, password: String):AppResult<UserSession>
    suspend fun registerWithEmail(email: String, password: String, displayName: String): AppResult<UserSession>
    suspend fun logout(): AppResult<Unit>
    suspend fun sendPasswordReset(email: String): AppResult<Unit>
}
