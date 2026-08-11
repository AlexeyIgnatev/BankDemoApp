package com.esom.bank.screens.history.model

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTransactionFiltersTest {

    @Test
    fun `transfer filter keeps transfer transactions`() {
        val transfer = transaction(
            id = 1,
            currency = CurrencyEnum.SOM,
            type = TransactionEnum.TRANSFER,
            createdAt = 1_000,
            amount = 10.0
        )

        val filtered = listOf(transfer).filterHistoryTransactions(
            HistoryAdapterUiState(typeFilter = HistoryTypeFilter.TRANSFERS)
        )

        assertEquals(1, filtered.size)
    }

    @Test
    fun `conversion filter keeps conversion transactions`() {
        val conversion = transaction(
            id = 2,
            currency = CurrencyEnum.SOM,
            type = TransactionEnum.CONVERSION,
            side = ConversionSide.OUT,
            createdAt = 2_000,
            amount = 99.0
        )

        val filtered = listOf(conversion).filterHistoryTransactions(
            HistoryAdapterUiState(typeFilter = HistoryTypeFilter.CONVERSIONS)
        )

        assertEquals(1, filtered.size)
        assertTrue(filtered.first().isHistoryConversionTransaction())
    }

    @Test
    fun `income filter keeps incoming user transfers`() {
        val income = transaction(
            id = 3,
            currency = CurrencyEnum.SOM,
            type = TransactionEnum.EXPENSE,
            createdAt = 3_000,
            amount = 50.0,
            senderName = "Alice"
        )

        val filtered = listOf(income).filterHistoryTransactions(
            HistoryAdapterUiState(typeFilter = HistoryTypeFilter.INCOME)
        )

        assertEquals(1, filtered.size)
    }

    private fun transaction(
        id: Long,
        currency: CurrencyEnum,
        type: TransactionEnum,
        side: ConversionSide? = null,
        createdAt: Long,
        amount: Double,
        senderName: String? = null
    ) = TransactionModel(
        transactionId = id,
        currencyEnum = currency,
        type = type,
        conversionSide = side,
        amount = amount,
        successful = true,
        createdAt = createdAt,
        senderFullName = senderName
    )
}
