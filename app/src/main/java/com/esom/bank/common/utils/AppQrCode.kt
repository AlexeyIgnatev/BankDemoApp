package com.esom.bank.common.utils

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.common.model.AppQrPayload

object AppQrCode {
    private const val PREFIX = "ESOM_BANK_QR"
    private const val VERSION_KEY = "v"
    private const val CURRENCY_KEY = "currency"
    private const val CONTACT_KEY = "contact"

    fun buildContent(contact: String, currency: CurrencyEnum): String {
        val normalizedContact = normalizeContact(contact)
        return buildString {
            append(PREFIX)
            append("|")
            append(VERSION_KEY)
            append("=1")
            append("|")
            append(CURRENCY_KEY)
            append("=")
            append(currency.name)
            append("|")
            append(CONTACT_KEY)
            append("=")
            append(normalizedContact)
        }
    }

    fun parsePayload(rawContent: String): AppQrPayload? {
        val map = parseKeyValuePairs(rawContent) ?: return null
        val contact = normalizeContact(map[CONTACT_KEY].orEmpty())
        val currency = map[CURRENCY_KEY]
            ?.trim()
            ?.let { runCatching { CurrencyEnum.valueOf(it) }.getOrNull() }
        val version = map[VERSION_KEY]?.toIntOrNull() ?: 1

        if (contact.isBlank() || currency == null || isMalformedPhone(contact)) return null

        return AppQrPayload(
            contact = contact,
            currency = currency,
            version = version
        )
    }

    private fun normalizeContact(value: String): String {
        val raw = value.trim()
        val phoneCharactersOnly = raw.all {
            it.isDigit() || it == '+' || it == ' ' || it == '(' || it == ')' || it == '-'
        }
        if (!phoneCharactersOnly) return raw

        val digits = raw.filter(Char::isDigit)
        return when {
            digits.startsWith("996") && digits.length == 12 -> "+" + digits
            digits.startsWith("0") && digits.length == 10 -> "+996" + digits.drop(1)
            digits.length == 9 -> "+996" + digits
            raw.startsWith("+") -> "+" + digits
            else -> raw
        }
    }

    private fun isMalformedPhone(value: String): Boolean {
        val phoneCharactersOnly = value.all {
            it.isDigit() || it == '+' || it == ' ' || it == '(' || it == ')' || it == '-'
        }
        return phoneCharactersOnly && value.count(Char::isDigit) !in 9..12
    }

    fun parseContact(rawContent: String): String? {
        val map = parseKeyValuePairs(rawContent) ?: return null
        return map[CONTACT_KEY]
            ?.let(::normalizeContact)
            ?.takeIf { it.isNotBlank() && !isMalformedPhone(it) }
    }

    private fun parseKeyValuePairs(rawContent: String): Map<String, String>? {
        val value = rawContent.trim()
        if (!value.startsWith(PREFIX)) return null

        return value
            .split("|")
            .drop(1)
            .mapNotNull { token ->
                val index = token.indexOf("=")
                if (index <= 0) return@mapNotNull null
                token.substring(0, index).trim() to token.substring(index + 1).trim()
            }
            .toMap()
    }
}
