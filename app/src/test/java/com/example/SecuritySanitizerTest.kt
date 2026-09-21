package com.example

import com.example.core.security.SecuritySanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySanitizerTest {

    @Test
    fun testValidEmail() {
        assertTrue(SecuritySanitizer.isValidEmail("user@guardianx.safety"))
        assertTrue(SecuritySanitizer.isValidEmail("contact.team+alert@example.com"))
        assertFalse(SecuritySanitizer.isValidEmail("invalid-email"))
        assertFalse(SecuritySanitizer.isValidEmail(null))
        assertFalse(SecuritySanitizer.isValidEmail(""))
    }

    @Test
    fun testPhoneNumberSanitizationAndValidation() {
        assertEquals("+1234567890", SecuritySanitizer.sanitizePhoneNumber("+1 (234) 567-890"))
        assertEquals("1234567890", SecuritySanitizer.sanitizePhoneNumber("123-456-7890"))
        assertTrue(SecuritySanitizer.isValidPhoneNumber("+1234567890"))
        assertFalse(SecuritySanitizer.isValidPhoneNumber("123")) // Too short
        assertFalse(SecuritySanitizer.isValidPhoneNumber(null))
    }

    @Test
    fun testMacAddressValidation() {
        assertTrue(SecuritySanitizer.isValidMacAddress("AA:BB:CC:DD:EE:FF"))
        assertTrue(SecuritySanitizer.isValidMacAddress("00-11-22-33-44-55"))
        assertFalse(SecuritySanitizer.isValidMacAddress("INVALID:MAC"))
        assertFalse(SecuritySanitizer.isValidMacAddress(null))
    }

    @Test
    fun testUrlValidation() {
        assertTrue(SecuritySanitizer.isValidUrl("https://guardianx.safety"))
        assertTrue(SecuritySanitizer.isValidUrl("http://portal.example.com/alerts"))
        assertFalse(SecuritySanitizer.isValidUrl("javascript:alert(1)"))
        assertFalse(SecuritySanitizer.isValidUrl("not a url"))
    }
}
