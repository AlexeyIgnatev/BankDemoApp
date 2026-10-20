package com.esom.bank.screens.swap.model

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.main.enums.CurrencyEnum

internal fun conversionReceiptSide(
    from: CurrencyEnum,
    to: CurrencyEnum
): ConversionSide? = if (from == to) null else ConversionSide.OUT
