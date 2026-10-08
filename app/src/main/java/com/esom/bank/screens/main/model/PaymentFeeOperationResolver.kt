package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum
import java.math.BigDecimal

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
            from == CurrencyEnum.SOM && to == CurrencyEnum.USDT_TRC20 ->
                listOf(CONVERT_SOM_TO_ESOM, CONVERT_ESOM_TO_USDT_TRC20)
            from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.SOM ->
                listOf(CONVERT_USDT_TRC20_TO_ESOM, CONVERT_ESOM_TO_SOM)
            else -> emptyList()
        }

    /**
     * Returns the source-currency equivalent of all fees in a conversion route.
     * SOM -> USDT is executed as SOM -> ESOM -> USDT; both fee stages must be
     * included, with the second tariff based on the ESOM remaining after stage 1.
     * SOM and ESOM are 1:1 pegged, so their fee amounts share the same value scale.
     */
    fun calculateConvertFee(
        amount: BigDecimal,
        from: CurrencyEnum,
        to: CurrencyEnum,
        feeForOperation: (String) -> PaymentFeeModel?,
        intermediateRate: BigDecimal = BigDecimal.ONE,
    ): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO

        if (from == CurrencyEnum.SOM && to == CurrencyEnum.USDT_TRC20) {
            val somFee = feeForOperation(CONVERT_SOM_TO_ESOM)
                ?.calculateFee(amount) ?: BigDecimal.ZERO
            val esomAfterFirstFee = (amount - somFee).max(BigDecimal.ZERO)
            val esomFee = feeForOperation(CONVERT_ESOM_TO_USDT_TRC20)
                ?.calculateFee(esomAfterFirstFee) ?: BigDecimal.ZERO
            return somFee + esomFee
        }

        if (from == CurrencyEnum.USDT_TRC20 && to == CurrencyEnum.SOM) {
            val rate = intermediateRate.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
            val usdtFee = feeForOperation(CONVERT_USDT_TRC20_TO_ESOM)
                ?.calculateFee(amount) ?: BigDecimal.ZERO
            val esomAfterFirstFee = (amount - usdtFee).max(BigDecimal.ZERO) * rate
            val esomFee = feeForOperation(CONVERT_ESOM_TO_SOM)
                ?.calculateFee(esomAfterFirstFee) ?: BigDecimal.ZERO
            val usdtEquivalentEsomFee = esomFee.divide(rate, 18, java.math.RoundingMode.HALF_UP)
            return usdtFee + usdtEquivalentEsomFee
        }

        return convertOperations(from, to).asSequence()
            .mapNotNull(feeForOperation)
            .map { it.calculateFee(amount) }
            .maxOrNull()
            ?: BigDecimal.ZERO
    }
}
