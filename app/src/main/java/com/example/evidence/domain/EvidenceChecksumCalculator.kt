package com.example.evidence.domain

import java.security.MessageDigest

object EvidenceChecksumCalculator {

    fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun calculateSha256ForText(content: String): String {
        return calculateSha256(content.toByteArray(Charsets.UTF_8))
    }
}
