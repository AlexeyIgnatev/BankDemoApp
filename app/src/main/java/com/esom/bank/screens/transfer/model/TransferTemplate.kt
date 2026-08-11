package com.esom.bank.screens.transfer.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class TransferTemplate(
    val amount: Double,
    val currency: String,
    val recipient: String,
    val isPhone: Boolean
) : Parcelable
