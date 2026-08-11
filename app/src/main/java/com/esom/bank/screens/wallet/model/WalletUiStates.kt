package com.esom.bank.screens.wallet.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel

@Keep
@Parcelize
data class WalletUiState(
    val balancesVisible: Boolean = true,
    val historyExpanded: Boolean = true,
    val wallets: List<WalletModel> = emptyList()
) : Parcelable

@Keep
@Parcelize
data class WalletsUiState(
    val balancesVisible: Boolean = true,
    val totalBalanceInSom: Double = 0.0
) : Parcelable

@Keep
@Parcelize
data class WalletDetailUiState(
    val currency: CurrencyEnum = CurrencyEnum.SOM,
    val wallet: WalletModel? = null,
    val balanceVisible: Boolean = true
) : Parcelable
