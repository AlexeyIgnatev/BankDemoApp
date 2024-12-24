package com.esom.bank.common.utils

fun Double.format(digits: Int = 6) =
    "%.${digits}f".format(this).replace(",", ".").removeTrailingZeros()

fun Double.round(digits: Int = 6) =
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