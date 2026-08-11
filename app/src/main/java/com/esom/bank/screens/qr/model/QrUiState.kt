package com.esom.bank.screens.qr.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.enums.CurrencyEnum

@Keep
@Parcelize
data class QrUiState(
    val phone: String = "",
    val salamAddress: String = "",
    val usdtAddress: String = "",
    val primaryCurrency: CurrencyEnum = CurrencyEnum.SOM,
    val torchEnabled: Boolean = false,
    val scanHandled: Boolean = false,
    val cameraRequestInFlight: Boolean = false,
    val renderGeneration: Int = 0
) : Parcelable
