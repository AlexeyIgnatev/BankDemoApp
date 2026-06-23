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
import com.esom.bank.screens.chat.enums.SupportRole
import com.esom.bank.screens.chat.model.SupportModel
import com.esom.bank.screens.history.pagingsource.TransactionsPagingSource
import com.esom.bank.screens.main.model.FeeModel
import com.esom.bank.screens.main.model.PaymentFeeModel
import com.esom.bank.screens.main.model.PaymentFeeOperationResolver
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.notification.model.NotificationModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _myData = MutableLiveData<UiState<UserModel>>()
    val myData: LiveData<UiState<UserModel>> = _myData

    private val _swapRes = SingleLiveEvent<UiState<StatusDto>>()
    val swapRes: LiveData<UiState<StatusDto>> = _swapRes

    private val _transferRes = SingleLiveEvent<UiState<StatusDto>>()
    val transferRes: LiveData<UiState<StatusDto>> = _transferRes

    private val _history = SingleLiveEvent<UiState<List<TransactionModel?>>>()
    val history: LiveData<UiState<List<TransactionModel?>>> = _history

    private val _month = SingleLiveEvent<UiState<List<TransactionModel?>>>()
    val month: LiveData<UiState<List<TransactionModel?>>> = _month

    private val _receipt = SingleLiveEvent<UiState<ReceiptModel>>()
    val receipt: LiveData<UiState<ReceiptModel>> = _receipt


    private val _messages = MutableLiveData<UiState<List<SupportModel>>>()
    val messages: LiveData<UiState<List<SupportModel>>> = _messages

    private val _sendMessage = SingleLiveEvent<UiState<SupportModel>>()
    val sendMessage: LiveData<UiState<SupportModel>> =_sendMessage

    private val _notifications = MutableLiveData<UiState<List<NotificationModel>>>()
    val notifications: LiveData<UiState<List<NotificationModel>>> = _notifications

    private val _settings = MutableLiveData<UiState<FeeModel>>()
    val settings: LiveData<UiState<FeeModel>> = _settings

    private val _fees = MutableLiveData<UiState<List<PaymentFeeModel>>>()
    val fees: LiveData<UiState<List<PaymentFeeModel>>> = _fees

    private val _financialReport = SingleLiveEvent<UiState<Unit>>()
    val financialReport: LiveData<UiState<Unit>> = _financialReport

    private val _pushNotificationsEnabled = MutableLiveData<Boolean>()
    val pushNotificationsEnabled: LiveData<Boolean> = _pushNotificationsEnabled

    private val _lastSuccessOperation = MutableLiveData<SuccessOperationModel?>()
    val lastSuccessOperation: LiveData<SuccessOperationModel?> = _lastSuccessOperation

    private val _lastSuccessReceipt = MutableLiveData<UiState<ReceiptModel>>()
    val lastSuccessReceipt: LiveData<UiState<ReceiptModel>> = _lastSuccessReceipt

    private val pendingMessages = mutableListOf<SupportModel>()
    private var cachedMessages: List<SupportModel> = emptyList()
    private var nextPendingMessageId = -1

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
        _fees.value = UiState.Loading()
        _financialReport.value = UiState.Loading()
        mainRepository.clearAllLocalData()
    }


    fun isAuthenticated() = mainRepository.isAuthenticated()

    fun authenticate(login: String, password: String) {
        _myData.value = UiState.Loading()
        mainRepository.authenticate(login, password).onEach {
            _myData.value = it
            if (it is UiState.Success) {
                getFees()
                refreshAndSendFcmToken()
            }
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

    fun getFees() {
        _fees.value = UiState.Loading()
        mainRepository.getFees().onEach {
            _fees.value = it
        }.launchIn(viewModelScope)
    }

    fun feeForOperation(operation: String?): PaymentFeeModel? {
        val state = _fees.value as? UiState.Success ?: return null
        return state.data.firstOrNull { it.operation.equals(operation, ignoreCase = true) }
    }

    fun calculateFee(amount: Double, operation: String?): Double {
        if (amount <= 0.0) return 0.0
        return feeForOperation(operation)?.calculateFee(amount) ?: 0.0
    }

    fun calculateTransferFee(amount: Double, currency: CurrencyEnum): Double {
        return calculateFeeForOperations(
            amount,
            PaymentFeeOperationResolver.transferOperations(currency)
        )
    }

    fun calculateConvertFee(amount: Double, from: CurrencyEnum, to: CurrencyEnum): Double {
        if (isSomEsomConversion(from, to)) {
            return calculateSettingsSomEsomFee(amount)
        }
        return calculateFeeForOperations(
            amount,
            PaymentFeeOperationResolver.convertOperations(from, to)
        )
    }

    fun feeForTransferOperation(currency: CurrencyEnum): PaymentFeeModel? {
        return feeForOperations(PaymentFeeOperationResolver.transferOperations(currency))
    }

    fun feeForConvertOperation(from: CurrencyEnum, to: CurrencyEnum): PaymentFeeModel? {
        if (isSomEsomConversion(from, to)) {
            return somEsomSettingsFeeModel()
        }
        return feeForOperationsWithPositiveFee(PaymentFeeOperationResolver.convertOperations(from, to))
    }

    private fun calculateFeeForOperations(amount: Double, operations: List<String>): Double {
        if (amount <= 0.0) return 0.0
        return operations.asSequence()
            .mapNotNull { feeForOperation(it) }
            .map { it.calculateFee(amount) }
            .firstOrNull { it > 0.0 }
            ?: 0.0
    }

    private fun feeForOperations(operations: List<String>): PaymentFeeModel? {
        if (operations.isEmpty()) return null
        return operations.asSequence()
            .mapNotNull { feeForOperation(it) }
            .firstOrNull()
    }

    private fun feeForOperationsWithPositiveFee(
        operations: List<String>,
        sampleAmount: Double = 1.0
    ): PaymentFeeModel? {
        if (operations.isEmpty()) return null
        return operations.asSequence()
            .mapNotNull { feeForOperation(it) }
            .firstOrNull { it.calculateFee(sampleAmount) > 0.0 }
    }

    private fun isSomEsomConversion(from: CurrencyEnum, to: CurrencyEnum): Boolean {
        return (from == CurrencyEnum.SOM && to == CurrencyEnum.ESOM) ||
            (from == CurrencyEnum.ESOM && to == CurrencyEnum.SOM)
    }

    private fun calculateSettingsSomEsomFee(amount: Double): Double {
        if (amount <= 0.0) return 0.0
        val settings = _settings.value as? UiState.Success<*> ?: return 0.0
        val feeModel = settings.data as? FeeModel ?: return 0.0
        val percent = feeModel.esomSomConversionFeePct?.coerceAtLeast(0.0) ?: 0.0
        val minFee = feeModel.esomSomConversionFeeMin?.coerceAtLeast(0.0) ?: 0.0
        val feeByPercent = amount * (percent / 100.0)
        return maxOf(feeByPercent, minFee)
    }

    private fun somEsomSettingsFeeModel(): PaymentFeeModel? {
        val settings = _settings.value as? UiState.Success<*> ?: return null
        val feeModel = settings.data as? FeeModel ?: return null
        val percent = feeModel.esomSomConversionFeePct?.coerceAtLeast(0.0) ?: 0.0
        val minFee = feeModel.esomSomConversionFeeMin?.coerceAtLeast(0.0) ?: 0.0
        return if (percent <= 0.0 && minFee <= 0.0) null else PaymentFeeModel(
            operation = "SETTINGS_SOM_ESOM_CONVERT",
            percentFee = percent,
            fixedFee = minFee
        )
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

    fun setLastSuccessOperation(
        operation: SuccessOperationModel,
        receipt: ReceiptModel? = null
    ) {
        _lastSuccessOperation.value = operation
        _lastSuccessReceipt.value = if (receipt != null) {
            UiState.Success(receipt)
        } else if (operation.loadReceiptAutomatically) {
            UiState.Loading()
        } else {
            UiState.Error(RECEIPT_OPERATION_NOT_FOUND)
        }
    }

    fun updateLastSuccessOperationReceipt(transactionId: Long?, receiptNumber: String?) {
        val current = _lastSuccessOperation.value ?: return
        _lastSuccessOperation.value = current.copy(
            transactionId = transactionId ?: current.transactionId,
            receiptNumber = receiptNumber?.takeIf { it.isNotBlank() } ?: current.receiptNumber
        )
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

    fun prepareReceiptForLastSuccessOperation() {
        val current = _lastSuccessOperation.value ?: return
        val transactionId = current.transactionId
        _lastSuccessReceipt.value = UiState.Loading()

        if (transactionId != null) {
            loadLastSuccessReceipt(transactionId, current.conversionSide)
        } else {
            _lastSuccessReceipt.value = UiState.Error(RECEIPT_OPERATION_NOT_FOUND)
        }
    }

    private fun loadLastSuccessReceipt(
        transactionId: Long,
        conversionSide: ConversionSide?
    ) {
        mainRepository.receipt(transactionId, conversionSide).onEach { state ->
            _lastSuccessReceipt.value = when (state) {
                is UiState.Success -> {
                    updateLastSuccessOperationReceipt(transactionId, state.data.receiptNumber)
                    UiState.Success<ReceiptModel>(state.data)
                }
                is UiState.Error -> UiState.Error<ReceiptModel>(state.message)
                is UiState.Loading -> UiState.Loading<ReceiptModel>()
            }
        }.launchIn(viewModelScope)
    }

    fun monthTransactions() {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        calendar.add(java.util.Calendar.MONTH, -1)
        val from = calendar.timeInMillis
        mainRepository.history(
            listOf(
                CurrencyEnum.SOM,
                CurrencyEnum.ESOM,
                CurrencyEnum.USDT_TRC20
            ),
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

    fun getMessages(showLoading: Boolean = true) {
        if (showLoading) {
            _messages.value = UiState.Loading()
        }
        mainRepository.getMessages().onEach { state ->
            when (state) {
                is UiState.Loading -> if (showLoading) _messages.value = state
                is UiState.Error -> if (showLoading) _messages.value = state
                is UiState.Success -> {
                    cachedMessages = mergeWithPendingMessages(state.data)
                    _messages.value = UiState.Success(cachedMessages)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        val pendingMessage = SupportModel(
            id = nextPendingMessageId--,
            ticketId = null,
            text = text,
            role = SupportRole.USER,
            createdAt = System.currentTimeMillis()
        )

        pendingMessages.add(pendingMessage)
        cachedMessages = mergeWithPendingMessages(cachedMessages)
        _messages.value = UiState.Success(cachedMessages)
        _sendMessage.value = UiState.Loading()

        mainRepository.sendMessage(text).onEach { state ->
            when (state) {
                is UiState.Loading -> _sendMessage.value = state
                is UiState.Error -> {
                    pendingMessages.removeAll { it.id == pendingMessage.id }
                    cachedMessages = cachedMessages.filterNot { it.id == pendingMessage.id }
                    cachedMessages = mergeWithPendingMessages(cachedMessages)
                    _messages.value = UiState.Success(cachedMessages)
                    _sendMessage.value = state
                }
                is UiState.Success -> {
                    pendingMessages.removeAll {
                        it.id == pendingMessage.id || it.isSameSentMessage(state.data)
                    }
                    cachedMessages = cachedMessages.filterNot {
                        it.id == pendingMessage.id || it.isSameSentMessage(state.data)
                    }
                    cachedMessages = mergeMessages(cachedMessages + state.data)
                    _messages.value = UiState.Success(cachedMessages)
                    _sendMessage.value = state
                    getMessages(showLoading = false)
                }
            }
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

    fun loadPushNotificationsEnabled() {
        _pushNotificationsEnabled.value = mainRepository.isPushNotificationsEnabled()
    }

    fun isPushNotificationsEnabled(): Boolean =
        mainRepository.isPushNotificationsEnabled()

    fun setPushNotificationsEnabled(enabled: Boolean) {
        mainRepository.setPushNotificationsEnabled(enabled)
        _pushNotificationsEnabled.value = enabled
        mainRepository.syncPushNotificationsEnabled(enabled).launchIn(viewModelScope)
    }

    fun getFcmToken(): String? =
        mainRepository.getFcmToken()

    fun setFcmToken(token: String?) {
        mainRepository.setFcmToken(token)
    }

    fun sendFcmToken(token: String?) {
        if (token.isNullOrBlank()) return
        mainRepository.sendFcmToken(token).launchIn(viewModelScope)
    }

    fun refreshAndSendFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                sendFcmToken(token)
            }
    }

    private fun mergeWithPendingMessages(remoteMessages: List<SupportModel>): List<SupportModel> {
        pendingMessages.removeAll { pendingMessage ->
            remoteMessages.any { remoteMessage -> pendingMessage.isSameSentMessage(remoteMessage) }
        }
        return mergeMessages(remoteMessages + pendingMessages)
    }

    private fun mergeMessages(messages: List<SupportModel>): List<SupportModel> {
        return messages
            .distinctBy { message ->
                if (message.id > 0) "server:${message.id}" else "pending:${message.id}"
            }
            .sortedBy { it.createdAt }
    }

    private fun SupportModel.isSameSentMessage(other: SupportModel): Boolean {
        return role == SupportRole.USER &&
            other.role == SupportRole.USER &&
            text == other.text &&
            abs(createdAt - other.createdAt) <= PENDING_MESSAGE_MATCH_WINDOW_MS
    }

    companion object {
        private const val PENDING_MESSAGE_MATCH_WINDOW_MS = 5 * 60 * 1000L
        const val RECEIPT_OPERATION_NOT_FOUND = "receipt_operation_not_found"
    }
}
