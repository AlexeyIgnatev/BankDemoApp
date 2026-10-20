package com.esom.bank.common.security

data class UnlockAttemptState(
    val failedAttempts: Int = 0,
    val blockedUntilMillis: Long = 0L
)

class LocalUnlockProtection(
    private val readState: () -> UnlockAttemptState,
    private val writeState: (UnlockAttemptState) -> Unit,
    private val now: () -> Long = System::currentTimeMillis
) {
    @Synchronized
    fun retryAfterMillis(): Long =
        (readState().blockedUntilMillis - now()).coerceIn(0L, MAX_DELAY_MILLIS)

    @Synchronized
    fun verify(matches: () -> Boolean): Boolean {
        if (retryAfterMillis() > 0L) return false
        if (matches()) {
            reset()
            return true
        }
        val failures = readState().failedAttempts.coerceIn(0, MAX_FAILURES) + 1
        val delay = if (failures < FREE_ATTEMPTS) 0L else {
            (BASE_DELAY_MILLIS shl (failures - FREE_ATTEMPTS).coerceAtMost(5))
                .coerceAtMost(MAX_DELAY_MILLIS)
        }
        writeState(UnlockAttemptState(failures.coerceAtMost(MAX_FAILURES), now() + delay))
        return false
    }

    @Synchronized
    fun reset() {
        writeState(UnlockAttemptState())
    }

    companion object {
        private const val FREE_ATTEMPTS = 5
        private const val MAX_FAILURES = 10
        private const val BASE_DELAY_MILLIS = 30_000L
        private const val MAX_DELAY_MILLIS = 900_000L
    }
}
