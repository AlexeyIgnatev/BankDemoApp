package com.esom.bank.common.session

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class SessionTokenGuardTest {
    @Test
    fun changedTokenWithoutGenerationChangeCannotBeCleared() {
        val guard = SessionTokenGuard()
        var token = "old"
        val request = guard.snapshot { token }
        token = "new"

        assertFalse(guard.invalidate(request, { token }) { token = "" })
        assertEquals("new", token)
    }

    @Test
    fun simultaneousUnauthorizedResponsesClearOnlyOnce() {
        val guard = SessionTokenGuard()
        var token: String? = "token"
        val request = guard.snapshot { token }
        val executor = Executors.newFixedThreadPool(4)
        val start = CountDownLatch(1)
        try {
            val results = List(16) {
                executor.submit<Boolean> {
                    check(start.await(5, TimeUnit.SECONDS))
                    guard.invalidate(request, { token }) { token = null }
                }
            }
            start.countDown()

            assertEquals(1, results.count { it.get(5, TimeUnit.SECONDS) })
            assertNull(guard.snapshot { token }.accessToken)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun replacementCannotInterleaveWithCompareAndClear() {
        val guard = SessionTokenGuard()
        var token: String? = "old"
        val request = guard.snapshot { token }
        val clearing = CountDownLatch(1)
        val releaseClear = CountDownLatch(1)
        val replacementStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val invalidation = executor.submit<Boolean> {
                guard.invalidate(request, { token }) {
                    clearing.countDown()
                    check(releaseClear.await(5, TimeUnit.SECONDS))
                    token = null
                }
            }
            assertTrue(clearing.await(5, TimeUnit.SECONDS))
            val replacement = executor.submit {
                replacementStarted.countDown()
                guard.change { token = "new" }
            }
            assertTrue(replacementStarted.await(5, TimeUnit.SECONDS))
            releaseClear.countDown()

            assertTrue(invalidation.get(5, TimeUnit.SECONDS))
            replacement.get(5, TimeUnit.SECONDS)
            assertEquals("new", guard.snapshot { token }.accessToken)
            assertFalse(guard.invalidate(request, { token }) { token = null })
        } finally {
            releaseClear.countDown()
            executor.shutdownNow()
        }
    }
}
