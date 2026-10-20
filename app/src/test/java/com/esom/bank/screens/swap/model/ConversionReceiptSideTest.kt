package com.esom.bank.screens.swap.model

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionReceiptSideTest {
    @Test
    fun `conversion receipt always requests destination side`() {
        val currencies = CurrencyEnum.values()

        currencies.forEach { from ->
            currencies.filter { it != from }.forEach { to ->
                assertEquals(ConversionSide.OUT, conversionReceiptSide(from, to))
            }
        }
    }

    @Test
    fun `same currency is not a conversion receipt`() {
        CurrencyEnum.values().forEach { currency ->
            assertNull(conversionReceiptSide(currency, currency))
        }
    }
}
