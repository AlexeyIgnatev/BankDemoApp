package com.esom.bank.screens.wallet.model

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HomeTransactionItemTest {

    @Test
    fun `conversion sides are merged into one operation`() {
        val items = listOf(
            transaction(10, CurrencyEnum.SOM, ConversionSide.OUT, 1_000, 100.0),
            transaction(10, CurrencyEnum.ESOM, ConversionSide.IN, 999, 98.0),
            transaction(11, CurrencyEnum.USDT_TRC20, null, 998, 2.0, TransactionEnum.TRANSFER)
        ).toHomeTransactionItems()

        assertEquals(2, items.size)
        assertNotNull(items.first().conversionFrom)
        assertNotNull(items.first().conversionTo)
        assertEquals(CurrencyEnum.SOM, items.first().conversionFrom?.currencyEnum)
        assertEquals(CurrencyEnum.ESOM, items.first().conversionTo?.currencyEnum)
    }

    @Test
    fun `home operation count is limited`() {
        val transactions = (1L..8L).map {
            transaction(it, CurrencyEnum.SOM, null, it, it.toDouble(), TransactionEnum.TRANSFER)
        }

        assertEquals(4, transactions.toHomeTransactionItems(limit = 4).size)
    }

    @Test
    fun `transfer marked as conversion is not merged`() {
        val transfer = transaction(
            id = 20,
            currency = CurrencyEnum.ESOM,
            side = null,
            createdAt = 2_000,
            amount = 50.0
        ).copy(recipientFullName = "Recipient Name")

        val item = listOf(transfer).toHomeTransactionItems().single()

        assertEquals(transfer, item.transaction)
        assertEquals(null, item.conversionFrom)
        assertEquals(null, item.conversionTo)
    }

    @Test
    fun `conversion response with recipient account is treated as transfer`() {
        val transfer = transaction(
            id = 21,
            currency = CurrencyEnum.SOM,
            side = null,
            createdAt = 2_100,
            amount = 75.0
        ).copy(accountDetails = "+996 700 123 456")

        val item = listOf(transfer).toHomeTransactionItems().single()

        assertEquals(transfer, item.transaction)
        assertEquals(null, item.conversionFrom)
        assertEquals(null, item.conversionTo)
    }

    private fun transaction(
        id: Long,
        currency: CurrencyEnum,
        side: ConversionSide?,
        createdAt: Long,
        amount: Double,
        type: TransactionEnum = TransactionEnum.CONVERSION
    ) = TransactionModel(
        transactionId = id,
        currencyEnum = currency,
        type = type,
        conversionSide = side,
        amount = amount,
        successful = true,
        createdAt = createdAt
    )
}
