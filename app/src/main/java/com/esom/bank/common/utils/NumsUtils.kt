package com.esom.bank.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun String.toMoneyDecimalOrNull(): BigDecimal? =
    replace(',', '.').trim().toBigDecimalOrNull()

fun String.toMoneyDecimalOrZero(): BigDecimal =
    toMoneyDecimalOrNull() ?: BigDecimal.ZERO

fun BigDecimal.format(digits: Int = 2): String =
    setScale(digits, RoundingMode.HALF_UP).toPlainString().removeTrailingZeros()

fun BigDecimal.formatBalanceNew(): String {
    val formatted = setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return if (formatted == "-0") "0" else formatted
}

fun String.removeTrailingZeros(): String {
    return if (!contains(".")) {
        this
    } else {
        replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")
    }
}

fun String.splitThreeChars(): String {
    return this.reversed().chunked(3).joinToString(" ").reversed()
}
