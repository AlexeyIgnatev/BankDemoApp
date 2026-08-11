package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import android.os.Bundle
import com.esom.bank.R

@Keep
@Parcelize
data class MainContainerUiState(
    val destinationId: Int = R.id.walletFragment,
    val keyboardVisible: Boolean = false
) : Parcelable

@Keep
@Parcelize
data class TransferConfirmationUiState(val data: Bundle = Bundle.EMPTY) : Parcelable
