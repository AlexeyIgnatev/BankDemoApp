package com.esom.bank.screens.transfer.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.actions.model.PaymentContact
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.TransferTemplate

@Keep
@Parcelize
data class TransferUiState(
    val currencyPanelShown: Boolean = false,
    val fromCurrency: CurrencyEnum = CurrencyEnum.SOM,
    val toPhoneNumber: Boolean = true,
    val currencyPanelOptions: List<CurrencyEnum> = emptyList(),
    val pendingTemplate: TransferTemplate? = null
) : Parcelable

@Keep
@Parcelize
data class TransferRecipientUiState(val contacts: List<PaymentContact> = emptyList()) : Parcelable

@Keep
@Parcelize
data class SuccessTransferUiState(
    val operation: SuccessOperationModel? = null,
    val shareAfterReceiptLoaded: Boolean = false
) : Parcelable
