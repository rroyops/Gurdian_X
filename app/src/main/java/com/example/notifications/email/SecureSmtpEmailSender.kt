package com.example.notifications.email

import com.example.core.common.AppResult
import com.example.core.error.AppError
import com.example.core.security.SecuritySanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * High-performance, RFC 5321/5322 compliant SMTP email client specifically tailored
 * for Gmail (smtp.gmail.com) STARTTLS (port 587) and Direct SSL (port 465).
 *
 * Implements strict CRLF line endings, proper TLS socket upgrade with SNI,
 * multiline response consumption, and base64 authentication.
 */
class SecureSmtpEmailSender(
    private val configProvider: () -> SmtpConfig = { EmailConfigProvider.getSmtpConfig() },
    private val timeoutMs: Int = 20000
) : EmailSender {

    override suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult> = withContext(Dispatchers.IO) {
        val rawRecipient = payload.to.trim()
        val recipient = extractCleanEmail(rawRecipient)
        if (!SecuritySanitizer.isValidEmail(recipient)) {
            return@withContext AppResult.Error(
                AppError.ValidationError("to", "Invalid recipient email address: $rawRecipient")
            )
        }

        val config = configProvider()
        if (config.username.isBlank() || config.appPassword.isBlank()) {
            return@withContext AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Gmail SMTP credentials missing: Please configure GUARDIANX_ALERT_SEND / GUARDIANX_GMAIL_USERNAME and your 16-character Google App Password in the Secrets panel.",
                    errorCode = "SMTP_AUTH_MISSING"
                )
            )
        }

        // For Gmail (or when SSL is requested), prioritize Port 465 Direct SSL first.
        // Direct SSL is instantaneous (~800ms) and immune to plaintext STARTTLS firewall filters.
        val isGmail = config.host.contains("gmail.com", ignoreCase = true)
        val shouldPrioritizeDirectSsl = isGmail || config.useSsl || config.port == 465

        val (primaryPort, primaryUseSsl) = if (shouldPrioritizeDirectSsl) Pair(465, true) else Pair(config.port, false)
        val (fallbackPort, fallbackUseSsl) = if (shouldPrioritizeDirectSsl) Pair(587, false) else Pair(465, true)

        val primaryResult = tryDispatchSmtp(config, primaryPort, primaryUseSsl, recipient, payload)
        if (primaryResult is AppResult.Success) {
            return@withContext primaryResult
        }

        // If primary attempt was rejected for bad credentials or invalid format, do not retry
        if (primaryResult is AppResult.Error && primaryResult.error is AppError.EmailError) {
            val code = (primaryResult.error as AppError.EmailError).errorCode
            if (code == "SMTP_AUTH_FAILED" || code == "SMTP_AUTH_MISSING") {
                return@withContext primaryResult
            }
        }

        // Automatically attempt failover to alternate port (e.g. 465 -> 587 or 587 -> 465)
        val fallbackResult = tryDispatchSmtp(config, fallbackPort, fallbackUseSsl, recipient, payload)
        if (fallbackResult is AppResult.Success) {
            return@withContext fallbackResult
        }

        // If both failed, return fallback result or primary error
        fallbackResult
    }

    private fun tryDispatchSmtp(
        config: SmtpConfig,
        port: Int,
        useSsl: Boolean,
        recipient: String,
        payload: EmailPayload
    ): AppResult<EmailSendResult> {
        val host = config.host
        val connectTimeoutMs = 4000
        val socketReadTimeoutMs = 6000

        var rawSocket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        return try {
            val plainSocket = Socket()
            plainSocket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            plainSocket.soTimeout = socketReadTimeoutMs

            if (useSsl) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(
                    plainSocket,
                    host,
                    port,
                    true
                ) as SSLSocket
                sslSocket.soTimeout = socketReadTimeoutMs
                sslSocket.startHandshake()
                rawSocket = sslSocket
            } else {
                rawSocket = plainSocket
            }

            reader = BufferedReader(InputStreamReader(rawSocket.getInputStream(), Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(rawSocket.getOutputStream(), Charsets.UTF_8), true)

            // Read initial greeting (220)
            val greeting = readMultilineResponse(reader)
            if (!greeting.startsWith("220")) {
                throw SmtpProtocolException("Server rejected connection on port $port: $greeting")
            }

            // Send initial EHLO
            val localHost = "localhost"
            sendLine(writer, "EHLO $localHost")
            val ehloResp = readMultilineResponse(reader)
            if (!ehloResp.startsWith("250")) {
                throw SmtpProtocolException("EHLO failed on port $port: $ehloResp")
            }

            // Upgrade to STARTTLS if not implicit SSL
            if (!useSsl) {
                sendLine(writer, "STARTTLS")
                val startTlsResp = readMultilineResponse(reader)
                if (!startTlsResp.startsWith("220")) {
                    throw SmtpProtocolException("STARTTLS rejected on port $port: $startTlsResp")
                }

                // Upgrade socket to TLS
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, null, SecureRandom())
                val tlsSocket = sslContext.socketFactory.createSocket(
                    rawSocket,
                    host,
                    port,
                    true
                ) as SSLSocket
                tlsSocket.soTimeout = socketReadTimeoutMs
                tlsSocket.startHandshake()
                rawSocket = tlsSocket

                reader = BufferedReader(InputStreamReader(rawSocket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(rawSocket.getOutputStream(), Charsets.UTF_8), true)

                // Repeat EHLO after TLS upgrade
                sendLine(writer, "EHLO $localHost")
                val ehloPostTls = readMultilineResponse(reader)
                if (!ehloPostTls.startsWith("250")) {
                    throw SmtpProtocolException("Post-TLS EHLO failed on port $port: $ehloPostTls")
                }
            }

            // AUTH LOGIN
            sendLine(writer, "AUTH LOGIN")
            val authResp = readMultilineResponse(reader)
            if (!authResp.startsWith("334")) {
                throw SmtpProtocolException("AUTH LOGIN command rejected on port $port: $authResp")
            }

            // Send base64 username
            val cleanUsername = extractCleanEmail(config.username)
            val encodedUser = base64Encode(cleanUsername.toByteArray(Charsets.UTF_8))
            sendLine(writer, encodedUser)
            val userResp = readMultilineResponse(reader)
            if (!userResp.startsWith("334")) {
                throw SmtpProtocolException("Username rejected on port $port: $userResp")
            }

            // Send base64 app password (strip spaces e.g., 'qfuk tnsd fbgv jecw')
            val cleanPassword = config.appPassword.replace(" ", "").trim()
            val encodedPass = base64Encode(cleanPassword.toByteArray(Charsets.UTF_8))
            sendLine(writer, encodedPass)
            val passResp = readMultilineResponse(reader)
            if (!passResp.startsWith("235")) {
                throw SmtpProtocolException("SMTP Authentication failed for $cleanUsername: $passResp")
            }

            // MAIL FROM
            val mailFrom = cleanUsername
            sendLine(writer, "MAIL FROM:<$mailFrom>")
            val mailFromResp = readMultilineResponse(reader)
            if (!mailFromResp.startsWith("250")) {
                throw SmtpProtocolException("MAIL FROM rejected on port $port: $mailFromResp")
            }

            // RCPT TO
            sendLine(writer, "RCPT TO:<$recipient>")
            val rcptResp = readMultilineResponse(reader)
            if (!rcptResp.startsWith("250")) {
                throw SmtpProtocolException("RCPT TO rejected on port $port for $recipient: $rcptResp")
            }

            // DATA
            sendLine(writer, "DATA")
            val dataPrompt = readMultilineResponse(reader)
            if (!dataPrompt.startsWith("354")) {
                throw SmtpProtocolException("DATA initiation rejected on port $port: $dataPrompt")
            }

            // Construct RFC 5322 MIME message
            val boundary = "===GuardianX_${System.currentTimeMillis()}_${SecureRandom().nextInt(999999)}==="
            val dateHeader = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val messageId = "<${System.currentTimeMillis()}.${SecureRandom().nextInt(100000)}@${host}>"

            sendLine(writer, "Date: $dateHeader")
            sendLine(writer, "From: GuardianX Defense <$mailFrom>")
            sendLine(writer, "To: <$recipient>")
            if (!payload.replyTo.isNullOrBlank()) {
                val cleanReplyTo = extractCleanEmail(payload.replyTo)
                sendLine(writer, "Reply-To: <$cleanReplyTo>")
            }
            sendLine(writer, "Subject: ${payload.subject}")
            sendLine(writer, "Message-ID: $messageId")
            sendLine(writer, "MIME-Version: 1.0")

            if (!payload.bodyHtml.isNullOrBlank()) {
                sendLine(writer, "Content-Type: multipart/alternative; boundary=\"$boundary\"")
                sendLine(writer, "")
                sendLine(writer, "--$boundary")
                sendLine(writer, "Content-Type: text/plain; charset=UTF-8")
                sendLine(writer, "Content-Transfer-Encoding: 8bit")
                sendLine(writer, "")
                sendMultilineContent(writer, payload.bodyText)
                sendLine(writer, "")
                sendLine(writer, "--$boundary")
                sendLine(writer, "Content-Type: text/html; charset=UTF-8")
                sendLine(writer, "Content-Transfer-Encoding: 8bit")
                sendLine(writer, "")
                sendMultilineContent(writer, payload.bodyHtml)
                sendLine(writer, "")
                sendLine(writer, "--$boundary--")
            } else {
                sendLine(writer, "Content-Type: text/plain; charset=UTF-8")
                sendLine(writer, "Content-Transfer-Encoding: 8bit")
                sendLine(writer, "")
                sendMultilineContent(writer, payload.bodyText)
            }

            // End DATA transmission with strict CRLF . CRLF
            sendLine(writer, "")
            sendLine(writer, ".")
            val sendResult = readMultilineResponse(reader)
            if (!sendResult.startsWith("250")) {
                throw SmtpProtocolException("Message delivery rejected on port $port: $sendResult")
            }

            // QUIT politely
            try {
                sendLine(writer, "QUIT")
            } catch (_: Throwable) {}

            AppResult.Success(
                EmailSendResult(
                    isSuccess = true,
                    messageId = messageId,
                    statusCode = 250
                )
            )
        } catch (e: SmtpProtocolException) {
            val errorCode = if (e.message?.contains("Authentication failed", ignoreCase = true) == true) {
                "SMTP_AUTH_FAILED"
            } else {
                "SMTP_REJECTED"
            }
            AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Gmail SMTP rejected on port $port: ${e.message}",
                    errorCode = errorCode,
                    cause = e
                )
            )
        } catch (e: java.net.SocketTimeoutException) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Connection timed out communicating with Gmail SMTP ($host:$port)",
                    isOffline = false,
                    cause = e
                )
            )
        } catch (e: java.io.IOException) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Network error during SMTP transmission on port $port: ${e.message}",
                    isOffline = true,
                    cause = e
                )
            )
        } catch (e: Exception) {
            AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Unexpected SMTP failure on port $port: ${e.message}",
                    errorCode = "SMTP_UNKNOWN_FAILURE",
                    cause = e
                )
            )
        } finally {
            try { writer?.close() } catch (_: Throwable) {}
            try { reader?.close() } catch (_: Throwable) {}
            try { rawSocket?.close() } catch (_: Throwable) {}
        }
    }

    private fun sendLine(writer: PrintWriter, text: String) {
        writer.print("$text\r\n")
        writer.flush()
    }

    private fun sendMultilineContent(writer: PrintWriter, content: String) {
        content.lineSequence().forEach { line ->
            // RFC 5321 dot-stuffing: If a line starts with a dot, prepend an extra dot
            val safeLine = if (line.startsWith(".")) ".$line" else line
            writer.print("$safeLine\r\n")
        }
        writer.flush()
    }

    private fun readMultilineResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: throw SmtpProtocolException("Connection closed by remote SMTP host")
            sb.append(line).append("\n")
            if (line.length >= 4 && line[3] == ' ') {
                break
            } else if (line.length == 3) {
                break
            }
        }
        return sb.toString().trim()
    }

    private fun extractCleanEmail(raw: String): String {
        return if (raw.contains("<") && raw.contains(">")) {
            raw.substringAfter("<").substringBefore(">").trim()
        } else {
            raw.trim()
        }
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }
}

class SmtpProtocolException(message: String) : Exception(message)
