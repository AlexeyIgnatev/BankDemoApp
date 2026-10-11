package com.esom.bank.common.utils

import com.esom.bank.common.utils.views.normalizeDecimalAmountInput
import org.junit.Assert.assertEquals
import org.junit.Test

class DecimalAmountInputTest {
    @Test
    fun `removes redundant leading integer zeros`() {
        assertEquals("1.25", normalizeDecimalAmountInput("01.25"))
        assertEquals("0.50", normalizeDecimalAmountInput("000.50"))
        assertEquals("0", normalizeDecimalAmountInput("000"))
    }

    @Test
    fun `preserves an unfinished decimal input`() {
        assertEquals("0,", normalizeDecimalAmountInput("00,"))
        assertEquals(".5", normalizeDecimalAmountInput(".5"))
        assertEquals("", normalizeDecimalAmountInput(""))
    }
}
