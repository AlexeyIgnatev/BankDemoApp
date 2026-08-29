package com.esom.bank.screens.transfer.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Keep
@Parcelize
data class TransferTemplate(
    val amount: BigDecimal,
    val currency: String,
    val recipient: String,
    val isPhone: Boolean,
    val name: String? = null
) : Parcelable
