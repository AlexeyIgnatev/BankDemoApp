package com.esom.bank.common.session

class SessionSnapshot internal constructor(
    internal val generation: Long,
    val accessToken: String?
)

class SessionTokenGuard {
    private var generation = 0L

    @Synchronized
    fun snapshot(readToken: () -> String?): SessionSnapshot =
        SessionSnapshot(generation, readToken()?.trim()?.takeIf { it.isNotEmpty() })

    @Synchronized
    fun <T> change(action: () -> T): T {
        generation++
        return action()
    }

    @Synchronized
    fun invalidate(
        session: SessionSnapshot,
        readToken: () -> String?,
        clear: () -> Unit
    ): Boolean {
        if (session.accessToken == null || session.generation != generation ||
            session.accessToken != readToken()?.trim()
        ) return false
        change(clear)
        return true
    }
}
