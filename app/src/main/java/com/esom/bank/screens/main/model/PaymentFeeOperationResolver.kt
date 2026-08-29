package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum

object PaymentFeeOperationResolver {
    const val WALLET_TRANSFER_SOM = "WALLET_TRANSFER_SOM"
    const val WALLET_TRANSFER_ESOM = "WALLET_TRANSFER_ESOM"
    const val WALLET_TRANSFER_USDT_TRC20 = "WALLET_TRANSFER_USDT_TRC20"

    const val CONVERT_SOM_TO_ESOM = "SOM_TO_ESOM"
    const val CONVERT_ESOM_TO_SOM = "ESOM_TO_SOM"
    const val CONVERT_ESOM_TO_USDT_TRC20 = "ESOM_TO_USDT_TRC20"
    const val CONVERT_USDT_TRC20_TO_ESOM = "USDT_TRC20_TO_ESOM"

    fun transferOperation(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> WALLET_TRANSFER_SOM
        CurrencyEnum.ESOM -> WALLET_TRANSFER_ESOM
        CurrencyEnum.USDT_TRC20 -> WALLET_TRANSFER_USDT_TRC20
    }

    fun transferOperations(currency: CurrencyEnum): List<String> =
        listOf(transferOperation(currency))

    fun convertOperations(from: CurrencyEnum, to: CurrencyEnum): List<String> =
        when {
            from == CurrencyEnum.SOM && to == CurrencyEnum.ESOM ->
                listOf(CONVERT_SOM_TO_ESOM)
            from == CurrencyEnum.ESOM && to == CurrencyEnum.SOM ->
                listOf(CONVERT_ESOM_TO_SOM)
            from == CurrencyEnum.ESOM && to == CurrencyEnum.USDT_TRC20 ->
                listOf(CONVERT_ESOM_TO_USDT_TRC20)
            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.ESOM ->
                listOf(CONVERT_USDT_TRC20_TO_ESOM)
            else -> emptyList()
        }
}
