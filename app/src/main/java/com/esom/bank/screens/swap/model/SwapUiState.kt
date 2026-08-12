package com.esom.bank.screens.swap.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.swap.model.SwapTemplate

@Keep
@Parcelize
data class SwapUiState(
    val fromPanelShown: Boolean = false,
    val toPanelShown: Boolean = false,
    val fromCurrency: CurrencyEnum = CurrencyEnum.SOM,
    val toCurrency: CurrencyEnum = CurrencyEnum.ESOM,
    val fromPanelCurrencies: List<CurrencyEnum> = emptyList(),
    val toPanelCurrencies: List<CurrencyEnum> = emptyList(),
    val updatingAmounts: Boolean = false,
    val pendingTemplate: SwapTemplate? = null,
    val automaticRepeatStarted: Boolean = false
) : Parcelable
