package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum

object PaymentFeeOperationResolver {
    const val WALLET_TRANSFER_ESOM = "WALLET_TRANSFER_ESOM"
    const val WALLET_TRANSFER_USDT_TRC20 = "WALLET_TRANSFER_USDT_TRC20"
    const val ESOM_TO_USDT_TRC20 = "ESOM_TO_USDT_TRC20"

    fun transferOperation(currency: CurrencyEnum): String? = when (currency) {
        CurrencyEnum.ESOM -> WALLET_TRANSFER_ESOM
        CurrencyEnum.USDT_TRC20 -> WALLET_TRANSFER_USDT_TRC20
        CurrencyEnum.SOM -> null
    }

    fun convertOperation(from: CurrencyEnum, to: CurrencyEnum): String? {
        if (from == to) return null

        return when {
            from == CurrencyEnum.ESOM && to == CurrencyEnum.USDT_TRC20 -> ESOM_TO_USDT_TRC20
            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.ESOM -> ESOM_TO_USDT_TRC20
            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.SOM -> ESOM_TO_USDT_TRC20
            from == CurrencyEnum.SOM && to == CurrencyEnum.USDT_TRC20 -> ESOM_TO_USDT_TRC20
            else -> null
        }
    }
}
