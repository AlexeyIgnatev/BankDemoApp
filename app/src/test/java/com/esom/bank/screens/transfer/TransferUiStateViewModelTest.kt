package com.esom.bank.screens.transfer

import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferUiStateViewModelTest {

    @Test
    fun `salam wallet address from qr uses address mode`() {
        val viewModel = TransferUiStateViewModel()

        viewModel.initialize(CurrencyEnum.ESOM, "TAzegbepNRtfd3ZXZpbuLAUzT")

        assertEquals(CurrencyEnum.ESOM, viewModel.uiState.value.fromCurrency)
        assertFalse(viewModel.uiState.value.toPhoneNumber)
    }

    @Test
    fun `phone qr for salam uses phone mode`() {
        val viewModel = TransferUiStateViewModel()

        viewModel.initialize(CurrencyEnum.ESOM, "+996 555 123 456")

        assertEquals(CurrencyEnum.ESOM, viewModel.uiState.value.fromCurrency)
        assertTrue(viewModel.uiState.value.toPhoneNumber)
    }

    @Test
    fun `usdt qr keeps selected wallet and address mode`() {
        val viewModel = TransferUiStateViewModel()

        viewModel.initialize(CurrencyEnum.USDT_TRC20, "TXo5rWJgM4address")

        assertEquals(CurrencyEnum.USDT_TRC20, viewModel.uiState.value.fromCurrency)
        assertFalse(viewModel.uiState.value.toPhoneNumber)
    }
}
