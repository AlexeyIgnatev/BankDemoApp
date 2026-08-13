package com.esom.bank.common.utils

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
    val tail = value
        .filter(Char::isLetterOrDigit)
        .takeLast(RECEIPT_ACCOUNT_VISIBLE_LENGTH)
    return if (tail.isBlank()) "" else RECEIPT_ACCOUNT_MASK + tail
}

private const val RECEIPT_ACCOUNT_VISIBLE_LENGTH = 8
private const val RECEIPT_ACCOUNT_MASK = "****"
