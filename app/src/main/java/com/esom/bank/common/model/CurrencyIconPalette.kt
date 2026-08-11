package com.esom.bank.common.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep

@Parcelize

data class CurrencyIconPalette(val highlight: Int, val base: Int, val shadow: Int) : Parcelable
