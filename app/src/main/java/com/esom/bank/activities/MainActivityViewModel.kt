package com.esom.bank.activities

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.activities.model.MainActivityUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainActivityUiState())
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    private val inactivityChecks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            inactivityChecks.collectLatest {
                val state = _uiState.value
                if (!state.isResumed || state.isLockDestination || state.lockRequested) return@collectLatest
                val remaining = INACTIVITY_TIMEOUT_MS -
                    (SystemClock.elapsedRealtime() - state.lastInteractionAt)
                if (remaining > 0L) delay(remaining)
                requestLock()
            }
        }
    }

    fun isAuthenticated(): Boolean = repository.isAuthenticated()

    fun hasLock(): Boolean = repository.hasLock()
    fun getThemeMode(): Int = repository.getThemeMode()

    fun handleSessionExpired() {
        repository.clearAllLocalData()
        _uiState.update {
            it.copy(
                lastInteractionAt = SystemClock.elapsedRealtime(),
                isLockDestination = false,
                lockRequested = false,
                shouldLock = false
            )
        }
    }

    fun onActivityResumed(isLockDestination: Boolean) {
        _uiState.update {
            it.copy(isResumed = true, isLockDestination = isLockDestination)
        }
        scheduleInactivityCheck()
    }

    fun onActivityPaused() {
        _uiState.update { it.copy(isResumed = false) }
        inactivityChecks.tryEmit(Unit)
    }

    fun onUserInteraction(isLockDestination: Boolean) {
        _uiState.update {
            it.copy(
                lastInteractionAt = SystemClock.elapsedRealtime(),
                isLockDestination = isLockDestination,
                lockRequested = false,
                shouldLock = false
            )
        }
        scheduleInactivityCheck()
    }

    fun markUserAuthenticated() {
        onUserInteraction(isLockDestination = false)
    }

    fun onLockHandled() {
        _uiState.update { it.copy(shouldLock = false, lockRequested = true) }
    }

    private fun scheduleInactivityCheck() {
        val state = _uiState.value
        if (!state.isResumed || state.isLockDestination || state.lockRequested) return
        inactivityChecks.tryEmit(Unit)
    }

    private fun requestLock() {
        val state = _uiState.value
        if (!state.isResumed || state.isLockDestination || state.lockRequested) return
        if (!repository.hasLock()) return
        _uiState.update { it.copy(lockRequested = true, shouldLock = true) }
    }

    private companion object {
        const val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
