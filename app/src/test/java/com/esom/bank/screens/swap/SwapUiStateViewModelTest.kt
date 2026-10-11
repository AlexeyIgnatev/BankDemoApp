package com.esom.bank.screens.swap

import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SwapUiStateViewModelTest {
    @Test
    fun `choosing current destination as source swaps the pair`() {
        val viewModel = SwapUiStateViewModel()
        viewModel.initialize(CurrencyEnum.SOM, CurrencyEnum.ESOM)

        viewModel.selectFrom(CurrencyEnum.ESOM)

        assertEquals(CurrencyEnum.ESOM, viewModel.uiState.value.fromCurrency)
        assertEquals(CurrencyEnum.SOM, viewModel.uiState.value.toCurrency)
    }

    @Test
    fun `choosing current source as destination swaps the pair`() {
        val viewModel = SwapUiStateViewModel()
        viewModel.initialize(CurrencyEnum.SOM, CurrencyEnum.ESOM)

        viewModel.selectTo(CurrencyEnum.SOM)

        assertEquals(CurrencyEnum.SOM, viewModel.uiState.value.toCurrency)
        assertEquals(CurrencyEnum.ESOM, viewModel.uiState.value.fromCurrency)
    }

    @Test
    fun `initializing same currency selects a different destination`() {
        val viewModel = SwapUiStateViewModel()

        viewModel.initialize(CurrencyEnum.SOM, CurrencyEnum.SOM)

        assertNotEquals(
            viewModel.uiState.value.fromCurrency,
            viewModel.uiState.value.toCurrency,
        )
    }
}
