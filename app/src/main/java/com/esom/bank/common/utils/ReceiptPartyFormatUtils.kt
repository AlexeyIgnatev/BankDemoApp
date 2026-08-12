package com.esom.bank.common.utils

import java.util.Locale

fun formatReceiptPersonName(fullName: String): String {
    val parts = fullName
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
    if (parts.size <= 1) return parts.firstOrNull().orEmpty()

    val surname = parts.first()
    val initials = parts.drop(1)
        .take(2)
        .mapNotNull { part -> part.firstOrNull(Char::isLetter)?.uppercaseChar() }
        .joinToString(".", postfix = ".")
    return listOf(surname, initials)
        .filter(String::isNotBlank)
        .joinToString(" ")
}

fun formatReceiptAccountTail(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.isNotBlank()) {
        return RECEIPT_ACCOUNT_MASK + digits.takeLast(RECEIPT_ACCOUNT_VISIBLE_LENGTH)
    }

    val tail = value
        .uppercase(Locale.getDefault())
        .filter(Char::isLetterOrDigit)
        .takeLast(RECEIPT_ACCOUNT_VISIBLE_LENGTH)
    return if (tail.isBlank()) "" else RECEIPT_ACCOUNT_MASK + tail
}

private const val RECEIPT_ACCOUNT_VISIBLE_LENGTH = 8
private const val RECEIPT_ACCOUNT_MASK = "****"
