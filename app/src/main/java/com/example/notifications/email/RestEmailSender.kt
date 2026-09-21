package com.example.notifications.email

import com.example.core.common.AppResult
import com.example.core.error.AppError
import com.example.core.security.SecuritySanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RestEmailSender(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val senderEmailProvider: () -> String = { "alerts@guardianx.safety" },
    private val apiKeyProvider: () -> String = { "" },
    private val apiEndpointProvider: () -> String = { "https://api.resend.com/emails" }
) : EmailSender {

    override suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult> = withContext(Dispatchers.IO) {
        val recipient = payload.to.trim()
        if (!SecuritySanitizer.isValidEmail(recipient)) {
            return@withContext AppResult.Error(
                AppError.ValidationError("to", "Invalid recipient email address: $recipient")
            )
        }

        val apiKey = apiKeyProvider().trim()
        val senderEmail = senderEmailProvider().trim()

        // If no API key configured, provide a clear honest diagnostic instead of faking success
        if (apiKey.isBlank()) {
            return@withContext AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Email service unconfigured: GUARDIANX_ALERT_API_KEY is not set in Secrets panel. Real HTTP delivery requires an active email API key.",
                    errorCode = "CREDENTIALS_MISSING"
                )
            )
        }

        try {
            val jsonBody = JSONObject().apply {
                put("from", "GuardianX Defense <$senderEmail>")
                put("to", JSONArray().apply { put(recipient) })
                put("subject", payload.subject)
                put("text", payload.bodyText)
                if (!payload.bodyHtml.isNullOrBlank()) {
                    put("html", payload.bodyHtml)
                }
                if (!payload.replyTo.isNullOrBlank()) {
                    put("reply_to", payload.replyTo)
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(apiEndpointProvider())
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val messageId = try {
                    val obj = JSONObject(responseBody)
                    if (obj.has("id")) obj.getString("id") else null
                } catch (_: Exception) {
                    null
                }
                AppResult.Success(
                    EmailSendResult(
                        isSuccess = true,
                        messageId = messageId,
                        statusCode = response.code
                    )
                )
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    jsonObj.optString("message", jsonObj.optString("error", responseBody))
                } catch (_: Exception) {
                    responseBody.ifBlank { "HTTP error ${response.code}" }
                }

                AppResult.Error(
                    AppError.EmailError(
                        recipient = recipient,
                        message = "Email delivery service rejected dispatch (HTTP ${response.code}): $errorMsg",
                        errorCode = "HTTP_${response.code}"
                    )
                )
            }
        } catch (e: java.io.IOException) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Network failure during emergency email dispatch to $recipient: ${e.message}",
                    isOffline = true,
                    cause = e
                )
            )
        } catch (e: Exception) {
            AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Failed to dispatch email to $recipient: ${e.message}",
                    errorCode = "UNKNOWN_DISPATCH_FAILURE",
                    cause = e
                )
            )
        }
    }
}
