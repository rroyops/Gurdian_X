package com.example.notifications.email

import android.util.Base64
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
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Secure, production-ready SMTP email client specifically tailored for Gmail (smtp.gmail.com)
 * and standard STARTTLS / SSL SMTP mail submission ports (587, 465).
 *
 * It operates via secure TLS/SSL socket layer without heavy Jakarta/JavaMail dependencies,
 * ensuring robust Android compatibility, non-blocking coroutines, strict error diagnosis,
 * and zero mock behavior.
 */
class SecureSmtpEmailSender(
    private val configProvider: () -> SmtpConfig = { EmailConfigProvider.getSmtpConfig() },
    private val timeoutMs: Int = 20000
) : EmailSender {

    override suspend fun sendEmail(payload: EmailPayload): AppResult<EmailSendResult> = withContext(Dispatchers.IO) {
        val recipient = payload.to.trim()
        if (!SecuritySanitizer.isValidEmail(recipient)) {
            return@withContext AppResult.Error(
                AppError.ValidationError("to", "Invalid recipient email address: $recipient")
            )
        }

        val config = configProvider()
        if (config.username.isBlank() || config.appPassword.isBlank()) {
            return@withContext AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Gmail/SMTP credentials missing: GUARDIANX_GMAIL_USERNAME or GUARDIANX_GMAIL_APP_PASSWORD is not set. Configure your Google App Password in AI Studio Secrets.",
                    errorCode = "SMTP_AUTH_MISSING"
                )
            )
        }

        var rawSocket: Socket? = null
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null

        try {
            val host = config.host
            val port = config.port

            if (config.useSsl || port == 465) {
                // Direct SSL/TLS connection (Implicit TLS)
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket() as SSLSocket
                sslSocket.connect(InetSocketAddress(host, port), timeoutMs)
                sslSocket.soTimeout = timeoutMs
                sslSocket.startHandshake()
                rawSocket = sslSocket
            } else {
                // Plain socket with STARTTLS upgrade (Port 587)
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = timeoutMs
                rawSocket = socket
            }

            reader = BufferedReader(InputStreamReader(rawSocket.getInputStream(), Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(rawSocket.getOutputStream(), Charsets.UTF_8), true)

            // Read greeting (220)
            val greeting = readResponse(reader)
            if (!greeting.startsWith("220")) {
                throw SmtpProtocolException("Server rejected connection: $greeting")
            }

            // EHLO
            val localHost = "guardianx.client"
            sendCommand(writer, "EHLO $localHost")
            val ehloResp = readMultilineResponse(reader)
            if (!ehloResp.startsWith("250")) {
                throw SmtpProtocolException("EHLO failed: $ehloResp")
            }

            // Upgrade to STARTTLS if not already SSL
            if (!(config.useSsl || port == 465)) {
                sendCommand(writer, "STARTTLS")
                val startTlsResp = readResponse(reader)
                if (!startTlsResp.startsWith("220")) {
                    throw SmtpProtocolException("STARTTLS rejected by server: $startTlsResp")
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
                tlsSocket.soTimeout = timeoutMs
                tlsSocket.startHandshake()
                rawSocket = tlsSocket

                reader = BufferedReader(InputStreamReader(rawSocket.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(rawSocket.getOutputStream(), Charsets.UTF_8), true)

                // Repeat EHLO after TLS upgrade
                sendCommand(writer, "EHLO $localHost")
                val ehloPostTls = readMultilineResponse(reader)
                if (!ehloPostTls.startsWith("250")) {
                    throw SmtpProtocolException("Post-TLS EHLO failed: $ehloPostTls")
                }
            }

            // AUTH LOGIN
            sendCommand(writer, "AUTH LOGIN")
            val authResp = readResponse(reader)
            if (!authResp.startsWith("334")) {
                throw SmtpProtocolException("AUTH LOGIN command rejected: $authResp")
            }

            // Send base64 username
            val encodedUser = Base64.encodeToString(config.username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, encodedUser)
            val userResp = readResponse(reader)
            if (!userResp.startsWith("334")) {
                throw SmtpProtocolException("Username rejected by SMTP host: $userResp")
            }

            // Send base64 app password (strip spaces if user formatted like 'abcd efgh ijkl mnop')
            val cleanPassword = config.appPassword.replace(" ", "")
            val encodedPass = Base64.encodeToString(cleanPassword.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            sendCommand(writer, encodedPass)
            val passResp = readResponse(reader)
            if (!passResp.startsWith("235")) {
                throw SmtpProtocolException("SMTP authentication failed. For Gmail, verify 2-Step Verification is enabled and an 'App Password' is generated. Server reply: $passResp")
            }

            // MAIL FROM
            val sender = if (config.senderEmail.isNotBlank()) config.senderEmail else config.username
            sendCommand(writer, "MAIL FROM:<$sender>")
            val mailFromResp = readResponse(reader)
            if (!mailFromResp.startsWith("250")) {
                throw SmtpProtocolException("MAIL FROM rejected: $mailFromResp")
            }

            // RCPT TO
            sendCommand(writer, "RCPT TO:<$recipient>")
            val rcptResp = readResponse(reader)
            if (!rcptResp.startsWith("250")) {
                throw SmtpProtocolException("RCPT TO rejected for $recipient: $rcptResp")
            }

            // DATA
            sendCommand(writer, "DATA")
            val dataPrompt = readResponse(reader)
            if (!dataPrompt.startsWith("354")) {
                throw SmtpProtocolException("DATA initiation rejected: $dataPrompt")
            }

            // Construct RFC 5322 MIME message
            val boundary = "===GuardianX_Boundary_${System.currentTimeMillis()}==="
            val dateHeader = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val messageId = "<${System.currentTimeMillis()}.${SecureRandom().nextInt(100000)}@guardianx.safety>"

            writer.println("Date: $dateHeader")
            writer.println("From: GuardianX Defense <$sender>")
            writer.println("To: <$recipient>")
            if (!payload.replyTo.isNullOrBlank()) {
                writer.println("Reply-To: <${payload.replyTo}>")
            }
            writer.println("Subject: ${payload.subject}")
            writer.println("Message-ID: $messageId")
            writer.println("MIME-Version: 1.0")

            if (!payload.bodyHtml.isNullOrBlank()) {
                writer.println("Content-Type: multipart/alternative; boundary=\"$boundary\"")
                writer.println()
                writer.println("--$boundary")
                writer.println("Content-Type: text/plain; charset=UTF-8")
                writer.println("Content-Transfer-Encoding: 8bit")
                writer.println()
                writer.println(payload.bodyText)
                writer.println()
                writer.println("--$boundary")
                writer.println("Content-Type: text/html; charset=UTF-8")
                writer.println("Content-Transfer-Encoding: 8bit")
                writer.println()
                writer.println(payload.bodyHtml)
                writer.println()
                writer.println("--$boundary--")
            } else {
                writer.println("Content-Type: text/plain; charset=UTF-8")
                writer.println("Content-Transfer-Encoding: 8bit")
                writer.println()
                writer.println(payload.bodyText)
            }

            // End DATA transmission with <CRLF>.<CRLF>
            writer.println(".")
            val sendResult = readResponse(reader)
            if (!sendResult.startsWith("250")) {
                throw SmtpProtocolException("Message delivery rejected: $sendResult")
            }

            // QUIT politely
            try {
                sendCommand(writer, "QUIT")
            } catch (_: Throwable) {
                // Ignore QUIT errors
            }

            AppResult.Success(
                EmailSendResult(
                    isSuccess = true,
                    messageId = messageId,
                    statusCode = 250
                )
            )
        } catch (e: SmtpProtocolException) {
            AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Gmail/SMTP Protocol Error: ${e.message}",
                    errorCode = "SMTP_REJECTED",
                    cause = e
                )
            )
        } catch (e: java.net.SocketTimeoutException) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Connection timed out communicating with SMTP server (${config.host}:${config.port})",
                    isOffline = false,
                    cause = e
                )
            )
        } catch (e: java.io.IOException) {
            AppResult.Error(
                AppError.NetworkError(
                    message = "Network error during SMTP transmission: ${e.message}",
                    isOffline = true,
                    cause = e
                )
            )
        } catch (e: Exception) {
            AppResult.Error(
                AppError.EmailError(
                    recipient = recipient,
                    message = "Unexpected SMTP failure: ${e.message}",
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

    private fun sendCommand(writer: PrintWriter, command: String) {
        writer.print("$command\r\n")
        writer.flush()
    }

    private fun readResponse(reader: BufferedReader): String {
        return reader.readLine() ?: throw SmtpProtocolException("Connection closed by remote SMTP server")
    }

    private fun readMultilineResponse(reader: BufferedReader): String {
        val sb = StringBuilder()
        var line: String
        while (true) {
            line = reader.readLine() ?: throw SmtpProtocolException("Connection closed during multiline SMTP reply")
            sb.append(line).append("\n")
            // In RFC 5321 multiline replies, intermediate lines have a hyphen at index 3 (e.g. "250-SIZE"), last line has space (e.g. "250 OK")
            if (line.length >= 4 && line[3] == ' ') {
                break
            } else if (line.length < 4) {
                break
            }
        }
        return sb.toString().trim()
    }
}

class SmtpProtocolException(message: String) : Exception(message)
