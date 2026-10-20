package com.esom.bank.common.security

import org.junit.Assert.*
import org.junit.Test

class LocalUnlockProtectionTest {
    private var persisted = UnlockAttemptState()
    private var time = 1_000_000L

    private fun protection() = LocalUnlockProtection({ persisted }, { persisted = it }, { time })

    @Test
    fun fifthFailureBlocksEvenCorrectPinAndDoesNotEvaluateIt() {
        val protection = protection()
        repeat(4) {
            assertFalse(protection.verify { false })
            assertEquals(0L, protection.retryAfterMillis())
        }
        assertFalse(protection.verify { false })
        assertEquals(30_000L, protection.retryAfterMillis())

        assertFalse(protection.verify { fail("Blocked credentials must not be evaluated"); true })
        assertEquals(5, persisted.failedAttempts)
    }

    @Test
    fun failureCountAndBackoffSurviveRecreation() {
        repeat(4) { protection().verify { false } }
        protection().verify { false }

        assertEquals(30_000L, protection().retryAfterMillis())
        time += 30_000L
        assertFalse(protection().verify { false })
        assertEquals(60_000L, protection().retryAfterMillis())
        assertEquals(6, persisted.failedAttempts)
    }

    @Test
    fun successiveFailuresIncreaseDelayWithFifteenMinuteCap() {
        val protection = protection()
        repeat(4) { protection.verify { false } }
        for (expected in listOf(30_000L, 60_000L, 120_000L, 240_000L, 480_000L, 900_000L, 900_000L)) {
            assertFalse(protection.verify { false })
            assertEquals(expected, protection.retryAfterMillis())
            time += expected
        }
    }

    @Test
    fun successfulCredentialAfterDelayResetsFailures() {
        val protection = protection()
        repeat(5) { protection.verify { false } }
        time += 30_000L

        assertTrue(protection.verify { true })
        assertEquals(UnlockAttemptState(), persisted)
        assertEquals(0L, protection.retryAfterMillis())
    }

    @Test
    fun trustedBiometricOrFullLoginResetIsAllowedDuringBackoff() {
        repeat(5) { protection().verify { false } }

        protection().reset()

        assertEquals(UnlockAttemptState(), persisted)
        assertTrue(protection().verify { true })
    }

    @Test
    fun pinAndPatternUseSamePersistedBudget() {
        val pin = protection()
        val pattern = protection()
        repeat(3) { pin.verify { false } }
        repeat(2) { pattern.verify { false } }

        assertEquals(30_000L, pin.retryAfterMillis())
        assertEquals(30_000L, pattern.retryAfterMillis())
        assertFalse(pattern.verify { true })
    }

    @Test
    fun clockRollbackDoesNotEnableCredentials() {
        repeat(5) { protection().verify { false } }
        time -= 60_000L

        assertTrue(protection().retryAfterMillis() > 0L)
        assertFalse(protection().verify { true })
    }
}
