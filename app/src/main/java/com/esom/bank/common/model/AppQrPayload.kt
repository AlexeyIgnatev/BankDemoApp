package com.esom.bank.common.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.enums.CurrencyEnum

@Keep

@Parcelize

data class AppQrPayload(
    val contact: String,
    val currency: CurrencyEnum?,
    val version: Int
) : Parcelable
