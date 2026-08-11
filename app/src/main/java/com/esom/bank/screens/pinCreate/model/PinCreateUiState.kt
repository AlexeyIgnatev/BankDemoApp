package com.esom.bank.screens.pinCreate.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.pinCreate.data.LockType
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class PinCreateUiState(
    val mode: LockType = LockType.PIN,
    val currentPin: String = "",
    val firstPin: String? = null,
    val firstPattern: List<Int>? = null
) : Parcelable
