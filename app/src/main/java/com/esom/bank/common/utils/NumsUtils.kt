package com.esom.bank.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.format(digits: Int = 2) =
    "%.${digits}f".format(this).replace(",", ".").removeTrailingZeros()

fun Double.formatBalanceNew(): String {
    val formatted = BigDecimal.valueOf(this)
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    if ("." !in formatted) {
        return formatted
    }

    return formatted.trimEnd('0')
        .trimEnd('.').ifEmpty { "0" }
}

fun Double.toMoneyAmount(): Double = BigDecimal.valueOf(this)
    .setScale(2, RoundingMode.HALF_UP)
    .toDouble()

fun Double.round(digits: Int = 2) =
    format(digits).toDoubleOrNull() ?: this

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

fun Double.formatBalanceNumber(): String {
    val n = this
    var balanceDigitsStr =
        n.toInt().toString().reversed().chunked(3).joinToString(",").reversed()
    if (n % 1 > 0) {
        balanceDigitsStr += (n % 1).format(2).replace("0.", ".")
    }
    return "${balanceDigitsStr}₽"
}

fun Int.formatBalanceNumber() = toDouble().formatBalanceNumber()
