package com.esom.bank.screens.main.enums

enum class CurrencyEnum {
    SOM,
    ESOM,
    USDT_TRC20
    ;

    companion object {
        val supportedValues: List<CurrencyEnum> = listOf(SOM, ESOM, USDT_TRC20)

        fun fromNameOrNull(value: String?): CurrencyEnum? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
