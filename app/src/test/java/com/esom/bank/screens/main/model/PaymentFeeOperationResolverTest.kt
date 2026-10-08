package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PaymentFeeOperationResolverTest {
    @Test
    fun `som to usdt includes sequential som and esom fees`() {
        val fees = mapOf(
            PaymentFeeOperationResolver.CONVERT_SOM_TO_ESOM to fee("1", "0"),
            PaymentFeeOperationResolver.CONVERT_ESOM_TO_USDT_TRC20 to fee("1", "0"),
        )

        val total = PaymentFeeOperationResolver.calculateConvertFee(
            amount = BigDecimal("50"),
            from = CurrencyEnum.SOM,
            to = CurrencyEnum.USDT_TRC20,
            feeForOperation = { fees[it] },
        )

        // First fee: 0.50 SOM; second fee: 1% of 49.50 ESOM = 0.495.
        assertEquals(0, BigDecimal("0.995").compareTo(total))
    }

    @Test
    fun `som to usdt uses fixed fee when it exceeds the second stage percentage`() {
        val fees = mapOf(
            PaymentFeeOperationResolver.CONVERT_SOM_TO_ESOM to fee("1", "0"),
            PaymentFeeOperationResolver.CONVERT_ESOM_TO_USDT_TRC20 to fee("1", "0.6"),
        )

        val total = PaymentFeeOperationResolver.calculateConvertFee(
            amount = BigDecimal("50"),
            from = CurrencyEnum.SOM,
            to = CurrencyEnum.USDT_TRC20,
            feeForOperation = { fees[it] },
        )

        assertEquals(0, BigDecimal("1.1").compareTo(total))
    }

    @Test
    fun `usdt to som converts the second stage fee back to usdt for the displayed total`() {
        val fees = mapOf(
            PaymentFeeOperationResolver.CONVERT_USDT_TRC20_TO_ESOM to fee("1", "0"),
            PaymentFeeOperationResolver.CONVERT_ESOM_TO_SOM to fee("1", "0"),
        )

        val total = PaymentFeeOperationResolver.calculateConvertFee(
            amount = BigDecimal("10"),
            from = CurrencyEnum.USDT_TRC20,
            to = CurrencyEnum.SOM,
            feeForOperation = { fees[it] },
            intermediateRate = BigDecimal("90"),
        )

        // 0.10 USDT first; then 1% of 891 ESOM = 8.91 ESOM = 0.099 USDT.
        assertEquals(0, BigDecimal("0.199").compareTo(total))
    }

    private fun fee(percent: String, fixed: String) = PaymentFeeModel(
        operation = "test",
        percentFee = BigDecimal(percent),
        fixedFee = BigDecimal(fixed),
    )
}
