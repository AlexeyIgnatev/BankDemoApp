package com.esom.bank.screens.actions.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.enums.CurrencyEnum

@Keep
@Parcelize
data class ActionsUiState(
    val selectedCurrency: CurrencyEnum = CurrencyEnum.SOM,
    val templatesExpanded: Boolean = true,
    val contactsExpanded: Boolean = true
) : Parcelable
