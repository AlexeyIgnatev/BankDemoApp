package com.esom.bank.screens.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.SingleLiveEvent
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
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
import com.esom.bank.screens.chat.model.SupportModel
import com.esom.bank.screens.history.pagingsource.TransactionsPagingSource
import com.esom.bank.screens.main.model.FeeModel
import com.esom.bank.screens.notification.model.NotificationModel
import kotlinx.coroutines.flow.Flow

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

    private val _history = SingleLiveEvent<UiState<List<TransactionModel?>>>()
    val history: LiveData<UiState<List<TransactionModel?>>> = _history

    private val _month = SingleLiveEvent<UiState<List<TransactionModel?>>>()
    val month: LiveData<UiState<List<TransactionModel?>>> = _month

    private val _receipt = SingleLiveEvent<UiState<ReceiptModel>>()
    val receipt: LiveData<UiState<ReceiptModel>> = _receipt

    private val _messages = MutableLiveData<UiState<List<SupportModel>>>()
    val messages: LiveData<UiState<List<SupportModel>>> = _messages

    private val _sendMessage = MutableLiveData<UiState<SupportModel>>()
    val sendMessage: LiveData<UiState<SupportModel>> =_sendMessage

    private val _notifications = MutableLiveData<UiState<List<NotificationModel>>>()
    val notifications: LiveData<UiState<List<NotificationModel>>> = _notifications

    private val _settings = MutableLiveData<UiState<FeeModel>>()
    val settings: LiveData<UiState<FeeModel>> = _settings

    private val _financialReport = SingleLiveEvent<UiState<Unit>>()
    val financialReport: LiveData<UiState<Unit>> = _financialReport

    fun clearAllDataAndNavigate() {
        _myData.value = UiState.Loading()
        _swapRes.value = UiState.Loading()
        _transferRes.value = UiState.Loading()
        _history.value = UiState.Loading()
        _month.value = UiState.Loading()
        _receipt.value = UiState.Loading()
        _messages.value = UiState.Loading()
        _sendMessage.value = UiState.Loading()
        _notifications.value = UiState.Loading()
        _settings.value = UiState.Loading()
        _financialReport.value = UiState.Loading()
        mainRepository.clearAllLocalData()
    }


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

    fun getSettings() {
        mainRepository.getSettings().onEach {
            _settings.value = it
        }.launchIn(viewModelScope)
    }

    fun convert(from: CurrencyEnum,
                to: CurrencyEnum,
                fromAmount: Double) {
        _swapRes.value = UiState.Loading()
        mainRepository.convert(from, to, fromAmount).onEach {
            _swapRes.value = it
        } .launchIn(viewModelScope)
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

    fun latestTransactions(currencyEnum: CurrencyEnum) {
        mainRepository.history(listOf(currencyEnum), getFromTime(), getToTime(), 5, 0).onEach { uiState ->
            _history.value = when (uiState) {
                is UiState.Success -> UiState.Success(uiState.data.filterNotNull())
                else -> uiState
            }
        }.launchIn(viewModelScope)
    }

    fun receipt(transactionId: Long, conversionSide: ConversionSide? = null) {
        _receipt.value = UiState.Loading()
        mainRepository.receipt(transactionId, conversionSide).onEach {
            _receipt.value = it
        }.launchIn(viewModelScope)
    }

    fun monthTransactions() {
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
        ).onEach { uiState ->
            _month.value = when (uiState) {
                is UiState.Success -> UiState.Success(uiState.data.filterNotNull())
                else -> uiState
            }
        }.launchIn(viewModelScope)
    }

    fun getWithoutTransactions(): Boolean = mainRepository.getWithoutTransactions()
    fun setWithoutTransactions(without: Boolean) = mainRepository.setWithoutTransactions(without)

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

    fun getMessages() {
        mainRepository.getMessages().onEach {
            _messages.value = it
        }.launchIn(viewModelScope)
    }
    fun sendMessage(text: String) {
        mainRepository.sendMessage(text).onEach {
            _sendMessage.value = it
        }.launchIn(viewModelScope)
    }
    fun loadNotifications() {
        mainRepository.getNotifications().onEach {
            _notifications.value = it
        }.launchIn(viewModelScope)
    }

    fun sendFinancialReport(
        email: String? = null,
        fromTime: Long? = null,
        toTime: Long? = null
    ) {
        _financialReport.value = UiState.Loading()
        mainRepository.sendFinancialReport(email, fromTime, toTime).onEach {
            _financialReport.value = it
        }.launchIn(viewModelScope)
    }
}
