package com.esom.bank.screens.chat.enums

enum class SupportRole {
    ASSISTANT, USER;

    companion object {
        fun fromRaw(value: String): SupportRole {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: ASSISTANT
        }
    }
}
