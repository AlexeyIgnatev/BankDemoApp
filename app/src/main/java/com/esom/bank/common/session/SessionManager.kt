package com.esom.bank.common.session

import androidx.annotation.MainThread
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SessionManager @Inject constructor() {
    private val logoutCounter = AtomicLong(0L)
    private val _loggedOutAt = MutableStateFlow(0L)
    val loggedOutAt: StateFlow<Long> = _loggedOutAt.asStateFlow()

    @MainThread
    fun notifyLoggedOut() {
        _loggedOutAt.value = logoutCounter.incrementAndGet()
    }

    @MainThread
    fun consumeLoggedOut() {
        _loggedOutAt.value = 0L
    }
}
