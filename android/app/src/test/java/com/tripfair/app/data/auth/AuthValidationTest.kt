package com.tripfair.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun acceptsEmailsAndVietnamesePhoneNumbers() {
        assertTrue(AuthValidation.isContact("doduyduc.1102@gmail.com"))
        assertTrue(AuthValidation.isContact("0901234567"))
        assertTrue(AuthValidation.isContact("090 123 4567"))
        assertTrue(AuthValidation.isContact("+84 901 234 567"))
    }

    @Test
    fun rejectsMalformedContacts() {
        assertFalse(AuthValidation.isContact(""))
        assertFalse(AuthValidation.isContact("duc@gmail"))
        assertFalse(AuthValidation.isContact("duc gmail.com"))
        assertFalse(AuthValidation.isContact("090123456"))
        assertFalse(AuthValidation.isContact("1901234567"))
        assertFalse(AuthValidation.isContact("09012345ab"))
    }

    @Test
    fun normalizesContactsToOneForm() {
        assertEquals("duc@gmail.com", AuthValidation.normalizeContact("  Duc@Gmail.com "))
        assertEquals("0901234567", AuthValidation.normalizeContact("+84 901-234-567"))
        assertEquals("0901234567", AuthValidation.normalizeContact("(090) 123.4567"))
    }

    @Test
    fun passwordNeedsEightCharactersWithLettersAndDigits() {
        assertTrue(AuthValidation.isPassword("tripfair26"))
        assertFalse(AuthValidation.isPassword("trip26"))
        assertFalse(AuthValidation.isPassword("tripfairr"))
        assertFalse(AuthValidation.isPassword("12345678"))
    }

    @Test
    fun codeHasExactlySixDigits() {
        assertTrue(AuthValidation.isCode("482915"))
        assertFalse(AuthValidation.isCode("48291"))
        assertFalse(AuthValidation.isCode("4829150"))
        assertFalse(AuthValidation.isCode("48291a"))
    }
}
