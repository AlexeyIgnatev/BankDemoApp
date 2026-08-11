package com.esom.bank.screens.history

import androidx.lifecycle.ViewModel
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.history.model.HistoryUiState
import com.esom.bank.screens.history.model.HistoryTypeFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HistoryUiStateViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(HistoryUiState())
    val uiState = mutableUiState.asStateFlow()
    private val activeHistoryJob = MutableStateFlow<Job?>(null)

    fun setWallets(value: List<WalletModel>) = mutableUiState.update { it.copy(wallets = value) }
    fun setPeriodTransactions(value: List<TransactionModel>) =
        mutableUiState.update { it.copy(periodTransactions = value) }

    fun setWithoutTransfers(value: Boolean) = mutableUiState.update {
        it.copy(adapterState = it.adapterState.copy(withoutTransfers = value))
    }

    fun setTypeFilter(value: HistoryTypeFilter) = mutableUiState.update {
        it.copy(adapterState = it.adapterState.copy(typeFilter = value))
    }

    fun setAmountFilter(minimum: Double?, maximum: Double?) = mutableUiState.update {
        it.copy(adapterState = it.adapterState.copy(minimumAmount = minimum, maximumAmount = maximum))
    }

    fun replaceHistoryJob(job: Job) {
        activeHistoryJob.value?.cancel()
        activeHistoryJob.value = job
    }

    override fun onCleared() {
        activeHistoryJob.value?.cancel()
    }
}
