package com.esom.bank.screens.login.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.pinCreate.data.LockType

@Keep
@Parcelize
data class LogInUiState(
    val pinCode: String = "",
    val mode: LockType = LockType.PIN,
    val biometricPromptShown: Boolean = false
) : Parcelable
