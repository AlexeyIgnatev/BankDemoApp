package com.esom.bank.common.utils

import com.esom.bank.screens.main.enums.CurrencyEnum

data class AppQrPayload(
    val contact: String,
    val currency: CurrencyEnum?,
    val version: Int
)

object AppQrCode {
    private const val PREFIX = "ESOM_BANK_QR"
    private const val VERSION_KEY = "v"
    private const val CURRENCY_KEY = "currency"
    private const val CONTACT_KEY = "contact"

    fun buildContent(contact: String, currency: CurrencyEnum): String {
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
            append(contact.trim())
        }
    }

    fun parsePayload(rawContent: String): AppQrPayload? {
        val map = parseKeyValuePairs(rawContent) ?: return null
        val contact = map[CONTACT_KEY].orEmpty().trim()
        val currency = map[CURRENCY_KEY]
            ?.trim()
            ?.let { runCatching { CurrencyEnum.valueOf(it) }.getOrNull() }
        val version = map[VERSION_KEY]?.toIntOrNull() ?: 1

        if (contact.isBlank() || currency == null) return null

        return AppQrPayload(
            contact = contact,
            currency = currency,
            version = version
        )
    }

    fun parseContact(rawContent: String): String? {
        val map = parseKeyValuePairs(rawContent) ?: return null
        return map[CONTACT_KEY]?.trim()?.takeIf { it.isNotBlank() }
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
