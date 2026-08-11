package com.esom.bank.activities.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import android.os.SystemClock

@Keep

@Parcelize

data class MainActivityUiState(
    val lastInteractionAt: Long = SystemClock.elapsedRealtime(),
    val isResumed: Boolean = false,
    val isLockDestination: Boolean = false,
    val lockRequested: Boolean = false,
    val shouldLock: Boolean = false
) : Parcelable
