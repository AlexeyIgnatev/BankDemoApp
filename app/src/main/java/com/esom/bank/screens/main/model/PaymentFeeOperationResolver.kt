package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum

object PaymentFeeOperationResolver {
    const val WALLET_TRANSFER_SOM = "WALLET_TRANSFER_SOM"
    const val WALLET_TRANSFER_SALAM = "WALLET_TRANSFER_SALAM"
    const val WALLET_TRANSFER_ESOM = "WALLET_TRANSFER_ESOM"
    const val WALLET_TRANSFER_USDT_TRC20 = "WALLET_TRANSFER_USDT_TRC20"

    const val CONVERT_SOM_TO_SOM = "CONVERT_SOM_TO_SOM"
    const val CONVERT_SOM_TO_ESOM = "CONVERT_SOM_TO_ESOM"
    const val CONVERT_SOM_TO_SALAM = "CONVERT_SOM_TO_SALAM"
    const val CONVERT_ESOM_TO_SOM = "CONVERT_ESOM_TO_SOM"
    const val CONVERT_SALAM_TO_SOM = "CONVERT_SALAM_TO_SOM"
    const val CONVERT_ESOM_TO_ESOM = "CONVERT_ESOM_TO_ESOM"
    const val CONVERT_SALAM_TO_SALAM = "CONVERT_SALAM_TO_SALAM"
    const val CONVERT_SOM_TO_USDT_TRC20 = "CONVERT_SOM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_SOM = "CONVERT_USDT_TRC20_TO_SOM"
    const val CONVERT_SALAM_TO_USDT_TRC20 = "CONVERT_SALAM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_SALAM = "CONVERT_USDT_TRC20_TO_SALAM"
    const val CONVERT_ESOM_TO_USDT_TRC20 = "CONVERT_ESOM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_ESOM = "CONVERT_USDT_TRC20_TO_ESOM"
    const val CONVERT_USDT_TRC20_TO_USDT_TRC20 = "CONVERT_USDT_TRC20_TO_USDT_TRC20"

    const val LEGACY_SOM_TO_ESOM = "SOM_TO_ESOM"
    const val LEGACY_ESOM_TO_SOM = "ESOM_TO_SOM"
    const val LEGACY_SOM_TO_SALAM = "SOM_TO_SALAM"
    const val LEGACY_SALAM_TO_SOM = "SALAM_TO_SOM"
    const val LEGACY_SOM_TO_SOM = "SOM_TO_SOM"
    const val LEGACY_ESOM_TO_ESOM = "ESOM_TO_ESOM"
    const val LEGACY_SALAM_TO_SALAM = "SALAM_TO_SALAM"
    const val LEGACY_SOM_TO_USDT_TRC20 = "SOM_TO_USDT_TRC20"
    const val LEGACY_USDT_TRC20_TO_SOM = "USDT_TRC20_TO_SOM"
    const val LEGACY_SALAM_TO_USDT_TRC20 = "SALAM_TO_USDT_TRC20"
    const val LEGACY_USDT_TRC20_TO_SALAM = "USDT_TRC20_TO_SALAM"
    const val LEGACY_ESOM_TO_USDT_TRC20 = "ESOM_TO_USDT_TRC20"
    const val LEGACY_USDT_TRC20_TO_ESOM = "USDT_TRC20_TO_ESOM"
    const val LEGACY_USDT_TRC20_TO_USDT_TRC20 = "USDT_TRC20_TO_USDT_TRC20"

    fun transferOperation(currency: CurrencyEnum): String? =
        transferOperations(currency).firstOrNull()

    fun transferOperations(currency: CurrencyEnum): List<String> = when (currency) {
        CurrencyEnum.SOM -> listOf(
            WALLET_TRANSFER_SOM,
            WALLET_TRANSFER_SALAM,
            "TRANSFER_SOM",
            "TRANSFER_SALAM",
            LEGACY_SOM_TO_SOM,
            LEGACY_SOM_TO_SALAM,
            LEGACY_SOM_TO_ESOM
        )

        CurrencyEnum.ESOM -> listOf(
            WALLET_TRANSFER_ESOM,
            WALLET_TRANSFER_SALAM,
            "TRANSFER_ESOM",
            "TRANSFER_SALAM",
            LEGACY_ESOM_TO_ESOM,
            LEGACY_SALAM_TO_SALAM,
            LEGACY_ESOM_TO_SOM,
            LEGACY_SALAM_TO_SOM
        )

        CurrencyEnum.USDT_TRC20 -> listOf(
            WALLET_TRANSFER_USDT_TRC20,
            "TRANSFER_USDT_TRC20",
            "TRANSFER_USDT",
            LEGACY_USDT_TRC20_TO_USDT_TRC20,
            LEGACY_USDT_TRC20_TO_SOM,
            LEGACY_USDT_TRC20_TO_ESOM
        )
    }

    fun convertOperations(from: CurrencyEnum, to: CurrencyEnum): List<String> {
        return when {
            from == to && from == CurrencyEnum.SOM -> listOf(
                CONVERT_SOM_TO_SOM,
                LEGACY_SOM_TO_SOM,
                "SOM_TO_SOM"
            )

            from == to && from == CurrencyEnum.ESOM -> listOf(
                CONVERT_ESOM_TO_ESOM,
                LEGACY_ESOM_TO_ESOM,
                CONVERT_SALAM_TO_SALAM,
                LEGACY_SALAM_TO_SALAM,
                "ESOM_TO_ESOM",
                "SALAM_TO_SALAM"
            )

            from == to && from == CurrencyEnum.USDT_TRC20 -> listOf(
                CONVERT_USDT_TRC20_TO_USDT_TRC20,
                LEGACY_USDT_TRC20_TO_USDT_TRC20,
                "USDT_TO_USDT"
            )

            from == CurrencyEnum.SOM && to == CurrencyEnum.ESOM -> listOf(
                CONVERT_SOM_TO_ESOM,
                CONVERT_SOM_TO_SALAM,
                LEGACY_SOM_TO_ESOM,
                LEGACY_SOM_TO_SALAM,
                "SOM_TO_ESOM",
                "SOM_TO_SALAM"
            )

            from == CurrencyEnum.ESOM && to == CurrencyEnum.SOM -> listOf(
                CONVERT_ESOM_TO_SOM,
                CONVERT_SALAM_TO_SOM,
                LEGACY_ESOM_TO_SOM,
                LEGACY_SALAM_TO_SOM,
                "ESOM_TO_SOM",
                "SALAM_TO_SOM"
            )

            from == CurrencyEnum.SOM && to == CurrencyEnum.USDT_TRC20 -> listOf(
                CONVERT_SOM_TO_USDT_TRC20,
                CONVERT_ESOM_TO_USDT_TRC20,
                CONVERT_SALAM_TO_USDT_TRC20,
                LEGACY_SOM_TO_USDT_TRC20,
                LEGACY_ESOM_TO_USDT_TRC20,
                LEGACY_SALAM_TO_USDT_TRC20,
                "SOM_TO_USDT",
                "ESOM_TO_USDT",
                "SALAM_TO_USDT",
                "SOM_TO_USDT_TRC20"
            )

            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.SOM -> listOf(
                CONVERT_USDT_TRC20_TO_SOM,
                CONVERT_USDT_TRC20_TO_ESOM,
                CONVERT_USDT_TRC20_TO_SALAM,
                LEGACY_USDT_TRC20_TO_SOM,
                LEGACY_USDT_TRC20_TO_ESOM,
                LEGACY_USDT_TRC20_TO_SALAM,
                "USDT_TO_SOM"
            )

            from == CurrencyEnum.ESOM && to == CurrencyEnum.USDT_TRC20 -> listOf(
                CONVERT_ESOM_TO_USDT_TRC20,
                CONVERT_SALAM_TO_USDT_TRC20,
                LEGACY_ESOM_TO_USDT_TRC20,
                LEGACY_SALAM_TO_USDT_TRC20,
                "ESOM_TO_USDT",
                "SALAM_TO_USDT",
                "SALAM_TO_USDT_TRC20"
            )

            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.ESOM -> listOf(
                CONVERT_USDT_TRC20_TO_ESOM,
                CONVERT_USDT_TRC20_TO_SALAM,
                LEGACY_USDT_TRC20_TO_ESOM,
                LEGACY_USDT_TRC20_TO_SALAM,
                "USDT_TO_ESOM",
                "USDT_TO_SALAM"
            )

            else -> emptyList()
        }
    }
}
