package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum

object PaymentFeeOperationResolver {
    const val WALLET_TRANSFER_SOM = "WALLET_TRANSFER_SOM"
    const val WALLET_TRANSFER_ESOM = "WALLET_TRANSFER_ESOM"
    const val WALLET_TRANSFER_USDT_TRC20 = "WALLET_TRANSFER_USDT_TRC20"

    const val CONVERT_SOM_TO_ESOM = "CONVERT_SOM_TO_ESOM"
    const val CONVERT_ESOM_TO_SOM = "CONVERT_ESOM_TO_SOM"
    const val CONVERT_SOM_TO_USDT_TRC20 = "CONVERT_SOM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_SOM = "CONVERT_USDT_TRC20_TO_SOM"
    const val CONVERT_ESOM_TO_USDT_TRC20 = "CONVERT_ESOM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_ESOM = "CONVERT_USDT_TRC20_TO_ESOM"

    const val LEGACY_SOM_TO_ESOM = "SOM_TO_ESOM"
    const val LEGACY_ESOM_TO_SOM = "ESOM_TO_SOM"
    const val LEGACY_SOM_TO_USDT_TRC20 = "SOM_TO_USDT_TRC20"
    const val LEGACY_USDT_TRC20_TO_SOM = "USDT_TRC20_TO_SOM"
    const val LEGACY_ESOM_TO_USDT_TRC20 = "ESOM_TO_USDT_TRC20"
    const val LEGACY_USDT_TRC20_TO_ESOM = "USDT_TRC20_TO_ESOM"

    fun transferOperation(currency: CurrencyEnum): String? =
        transferOperations(currency).firstOrNull()

    fun transferOperations(currency: CurrencyEnum): List<String> = when (currency) {
        CurrencyEnum.SOM -> listOf(WALLET_TRANSFER_SOM, WALLET_TRANSFER_ESOM)
        CurrencyEnum.ESOM -> listOf(WALLET_TRANSFER_ESOM, WALLET_TRANSFER_SOM)
        CurrencyEnum.USDT_TRC20 -> listOf(WALLET_TRANSFER_USDT_TRC20)
    }

    fun convertOperation(from: CurrencyEnum, to: CurrencyEnum): String? =
        convertOperations(from, to).firstOrNull()

    fun convertOperations(from: CurrencyEnum, to: CurrencyEnum): List<String> {
        if (from == to) return emptyList()

        return when {
            from == CurrencyEnum.SOM && to == CurrencyEnum.USDT_TRC20 -> listOf(
                CONVERT_SOM_TO_USDT_TRC20,
                LEGACY_SOM_TO_USDT_TRC20
            )

            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.SOM -> listOf(
                CONVERT_USDT_TRC20_TO_SOM,
                LEGACY_USDT_TRC20_TO_SOM
            )

            from == CurrencyEnum.ESOM && to == CurrencyEnum.USDT_TRC20 -> listOf(
                CONVERT_ESOM_TO_USDT_TRC20,
                LEGACY_ESOM_TO_USDT_TRC20
            )

            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.ESOM -> listOf(
                CONVERT_USDT_TRC20_TO_ESOM,
                LEGACY_USDT_TRC20_TO_ESOM
            )

            else -> emptyList()
        }
    }
}
