package com.tripfair.app.data.auth

import com.tripfair.app.ui.sample.SampleData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SampleAuthRepositoryTest {

    private var now = 0L
    private val repository = SampleAuthRepository(clock = { now })
    private val code = SampleAuthRepository.SAMPLE_CODE

    @Test
    fun logsInWithEmailOrPhoneAndRejectsWrongPassword() = runBlocking {
        assertEquals(AuthError.WrongCredentials, repository.login(SampleData.USER_EMAIL, "wrongpass1"))
        assertNull(repository.currentAccount.value)

        assertNull(repository.login(" ${SampleData.USER_EMAIL.uppercase()} ", SampleData.PASSWORD))
        assertEquals(SampleData.USER_NAME, repository.currentAccount.value?.fullName)

        repository.logout()
        assertNull(repository.currentAccount.value)
        assertNull(repository.login("+84 901 234 567", SampleData.PASSWORD))
    }

    @Test
    fun registrationCreatesTheAccountOnlyAfterTheCode() = runBlocking {
        assertNull(repository.startRegistration("Lan", "lan@gmail.com", "lanlan123"))
        assertEquals("lan@gmail.com", repository.pendingCode.value?.contact)
        assertEquals(AuthError.WrongCredentials, repository.login("lan@gmail.com", "lanlan123"))

        assertEquals(AuthError.WrongCode, repository.verifyCode("000000"))
        assertNull(repository.verifyCode(code))
        assertEquals("Lan", repository.currentAccount.value?.fullName)
        assertNull(repository.pendingCode.value)

        repository.logout()
        assertNull(repository.login("lan@gmail.com", "lanlan123"))
    }

    @Test
    fun registrationRejectsAnExistingContact() = runBlocking {
        assertEquals(AuthError.AccountExists, repository.startRegistration("Đức", "0901234567", "tripfair99"))
    }

    @Test
    fun passwordResetChangesThePassword() = runBlocking {
        assertEquals(AuthError.AccountNotFound, repository.startPasswordReset("nobody@gmail.com"))
        assertEquals(AuthError.NoPendingCode, repository.setNewPassword("newpass123"))

        assertNull(repository.startPasswordReset(SampleData.USER_EMAIL))
        assertEquals(AuthError.NoPendingCode, repository.setNewPassword("newpass123"))
        assertNull(repository.verifyCode(code))
        assertNull(repository.setNewPassword("newpass123"))

        assertEquals(AuthError.WrongCredentials, repository.login(SampleData.USER_EMAIL, SampleData.PASSWORD))
        assertNull(repository.login(SampleData.USER_EMAIL, "newpass123"))
    }

    @Test
    fun codeExpiresAfterFifteenMinutesAndResendingRenewsIt() = runBlocking {
        assertNull(repository.startPasswordReset(SampleData.USER_EMAIL))
        now += SampleAuthRepository.CODE_VALID_MILLIS + 1
        assertEquals(AuthError.CodeExpired, repository.verifyCode(code))

        assertNull(repository.resendCode())
        assertNull(repository.verifyCode(code))
    }
}
