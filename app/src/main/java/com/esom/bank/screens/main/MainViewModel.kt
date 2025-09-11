package com.esom.bank.screens.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.SingleLiveEvent
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.esom.bank.screens.history.pagingsource.TransactionsPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _myData = MutableLiveData<UiState<UserModel>>()
    val myData: LiveData<UiState<UserModel>> = _myData

    private val _swapRes = SingleLiveEvent<UiState<Unit>>()
    val swapRes: LiveData<UiState<Unit>> = _swapRes

    private val _transferRes = SingleLiveEvent<UiState<Unit>>()
    val transferRes: LiveData<UiState<Unit>> = _transferRes

    private val _history = SingleLiveEvent<UiState<List<TransactionModel>>>()
    val history: LiveData<UiState<List<TransactionModel>>> = _history

    private val _month = SingleLiveEvent<UiState<List<TransactionModel>>>()
    val month: LiveData<UiState<List<TransactionModel>>> = _month

    fun isAuthenticated() = mainRepository.isAuthenticated()

    fun authenticate(login: String, password: String) {
        _myData.value = UiState.Loading()
        mainRepository.authenticate(login, password).onEach {
            _myData.value = it
        }.launchIn(viewModelScope)
    }

    fun updateUserData() {
        mainRepository.getUserInfo().onEach {
            _myData.value = it
        }.launchIn(viewModelScope)
    }

    fun transferFromFiat(amount: Double) {
        _swapRes.value = UiState.Loading()
        mainRepository.transferFromFiat(amount).onEach {
            _swapRes.value = it
        }.launchIn(viewModelScope)
    }

    fun transferToFiat(amount: Double) {
        _swapRes.value = UiState.Loading()
        mainRepository.transferToFiat(amount).onEach {
            _swapRes.value = it
        }.launchIn(viewModelScope)
    }

    fun transferToUser(amount: Double, phone: String, address: String?, currencyEnum: CurrencyEnum) {
        _transferRes.value = UiState.Loading()
        mainRepository.transferToUser(amount, phone, address, currencyEnum).onEach {
            _transferRes.value = it
        }.launchIn(viewModelScope)
    }
    fun historyPaging(
        currencyEnum: List<CurrencyEnum>?,
        fromTime: Long,
        toTime: Long,
        pageSize: Int = 20
    ): Flow<PagingData<TransactionModel>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                TransactionsPagingSource(
                    repository = mainRepository,
                    currencyEnum = currencyEnum,
                    fromTime = fromTime,
                    toTime = toTime
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    suspend fun latestTransactions(currencyEnum: CurrencyEnum) {
        mainRepository.history(listOf(currencyEnum), getFromTime(), getToTime(), 5, 0).onEach {
            _history.value = it
        }.launchIn(viewModelScope)
    }

    suspend fun monthTransactions() {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(java.util.Calendar.MONTH, -1)
        val from = calendar.timeInMillis
        mainRepository.history(
            listOf(CurrencyEnum.BTC, CurrencyEnum.ETH, CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM, CurrencyEnum.SOM),
            from,
            System.currentTimeMillis(),
            50,
            0
        ).onEach {
            _month.value = it
        }.launchIn(viewModelScope)
    }


    fun getCurrency(): List<CurrencyEnum> = mainRepository.getCurrency()
    fun setCurrency(currency: List<CurrencyEnum>) {
        mainRepository.setCurrency(currency)
    }

    fun getFromTime(): Long = mainRepository.getFromTime()
    fun setFromTime(time: Long) {
        mainRepository.setFromTime(time)
    }

    fun getToTime(): Long = mainRepository.getToTime()
    fun setToTime(time: Long) {
        mainRepository.setToTime(time)
    }
}