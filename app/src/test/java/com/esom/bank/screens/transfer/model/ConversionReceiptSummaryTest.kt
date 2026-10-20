package com.esom.bank.screens.transfer.model

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ConversionReceiptSummaryTest {
    @Test
    fun `compound conversion keeps source debit and actual destination credit`() {
        val operation = SuccessOperationModel(
            amount = BigDecimal("50"),
            currency = CurrencyEnum.SOM,
            operationTitle = "Конвертация",
            paidFromAccount = "сомовый счет",
            recipient = "USDT wallet",
            receiptNumber = "",
            fee = BigDecimal("0.5"),
            feeCurrency = CurrencyEnum.SOM,
            creditedAmount = BigDecimal("0.57"),
            creditedCurrency = CurrencyEnum.USDT_TRC20,
            debitedCurrency = CurrencyEnum.SOM,
            conversionSide = ConversionSide.OUT,
            targetCurrency = CurrencyEnum.USDT_TRC20,
            totalDebitedAmount = BigDecimal("50")
        )
        val finalLegReceipt = ReceiptModel(
            successful = true,
            amount = BigDecimal("49.5"),
            type = "CONVERSION",
            currency = "ESOM",
            createdAt = 1L,
            fee = BigDecimal.ZERO,
            feeCurrency = "ESOM",
            accountDetails = "USDT wallet",
            recipientFullName = "User",
            paidFromAccount = "ESOM wallet",
            conversionSide = ConversionSide.OUT,
            creditedAmount = BigDecimal("0.5625"),
            creditedCurrency = "USDT_TRC20",
            debitedCurrency = "ESOM",
            absAccount = "",
            absFromAccount = "",
            absToAccount = "",
            receiptNumber = "TX-126"
        )

        val summary = finalLegReceipt.withConversionSummary(operation)

        assertEquals(BigDecimal("50"), summary.amount)
        assertEquals("SOM", summary.currency)
        assertEquals(BigDecimal("0.5"), summary.fee)
        assertEquals("SOM", summary.feeCurrency)
        assertEquals("SOM", summary.debitedCurrency)
        assertEquals("сомовый счет", summary.paidFromAccount)
        assertEquals(BigDecimal("0.5625"), summary.creditedAmount)
        assertEquals("USDT_TRC20", summary.creditedCurrency)
        assertEquals(BigDecimal("50"), summary.totalDebitedAmount)
    }
}
