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
import com.esom.bank.screens.chat.model.SupportMessagesUiState
import com.esom.bank.screens.history.pagingsource.TransactionsPagingSource
import com.esom.bank.screens.main.model.FeeModel
import com.esom.bank.screens.main.model.PaymentFeeModel
import com.esom.bank.screens.main.model.PaymentFeeOperationResolver
import com.esom.bank.screens.main.model.findByOperation
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.notification.model.NotificationModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.esom.bank.screens.pinCreate.data.LockType
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _balancesVisible = MutableLiveData(mainRepository.areBalancesVisible())
    val balancesVisible: LiveData<Boolean> = _balancesVisible
    private val _myData = MutableLiveData<UiState<UserModel>?>()
    val myData: LiveData<UiState<UserModel>?> = _myData

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


    private val _messages = MutableLiveData<UiState<List<SupportModel>>?>()
    val messages: LiveData<UiState<List<SupportModel>>?> = _messages

    private val _sendMessage = SingleLiveEvent<UiState<SupportModel>>()
    val sendMessage: LiveData<UiState<SupportModel>> =_sendMessage

    private val _notifications = MutableLiveData<UiState<List<NotificationModel>>?>()
    val notifications: LiveData<UiState<List<NotificationModel>>?> = _notifications

    private val _hasUnreadNotifications = MutableLiveData(false)
    val hasUnreadNotifications: LiveData<Boolean> = _hasUnreadNotifications

    private val _settings = MutableLiveData<UiState<FeeModel>?>()
    val settings: LiveData<UiState<FeeModel>?> = _settings

    private val _fees = MutableLiveData<UiState<List<PaymentFeeModel>>?>()
    val fees: LiveData<UiState<List<PaymentFeeModel>>?> = _fees

    private val _financialReport = SingleLiveEvent<UiState<Unit>>()
    val financialReport: LiveData<UiState<Unit>> = _financialReport

    private val _pushNotificationsEnabled = MutableLiveData<Boolean>()
    val pushNotificationsEnabled: LiveData<Boolean> = _pushNotificationsEnabled

    private val _lastSuccessOperation = MutableLiveData<SuccessOperationModel?>()
    val lastSuccessOperation: LiveData<SuccessOperationModel?> = _lastSuccessOperation

    private val _lastSuccessReceipt = MutableLiveData<UiState<ReceiptModel>?>()
    val lastSuccessReceipt: LiveData<UiState<ReceiptModel>?> = _lastSuccessReceipt

    private val supportMessagesUiState = MutableStateFlow(SupportMessagesUiState())

    fun clearAllDataAndNavigate() {
        mainRepository.clearAllLocalData()
        _myData.value = null
        _swapRes.clear()
        _transferRes.clear()
        _history.clear()
        _month.clear()
        _receipt.clear()
        _messages.value = null
        _sendMessage.clear()
        _notifications.value = null
        _settings.value = null
        _fees.value = null
        _financialReport.clear()
        _hasUnreadNotifications.value = false
        _lastSuccessOperation.value = null
        _lastSuccessReceipt.value = null
    }


    fun isAuthenticated() = mainRepository.isAuthenticated()
    fun getLogin(): String = mainRepository.getLogin()
    fun hasLock(): Boolean = mainRepository.hasLock()
    fun hasPin(): Boolean = mainRepository.hasPin()
    fun hasPattern(): Boolean = mainRepository.hasPattern()
    fun getLockType(): LockType? = mainRepository.getLockType()
    fun savePin(pin: String) = mainRepository.savePin(pin)
    fun verifyPin(pin: String): Boolean = mainRepository.verifyPin(pin)
    fun savePattern(pattern: List<Int>) = mainRepository.savePattern(pattern)
    fun verifyPattern(pattern: List<Int>): Boolean = mainRepository.verifyPattern(pattern)
    fun isBiometricEnabled(): Boolean = mainRepository.isBiometricEnabled()
    fun setBiometricEnabled(enabled: Boolean) = mainRepository.setBiometricEnabled(enabled)
    fun getPrimaryCurrency(): CurrencyEnum = mainRepository.getPrimaryCurrency()
    fun setPrimaryCurrency(currency: CurrencyEnum) = mainRepository.setPrimaryCurrency(currency)
    fun getTransferTemplates(): List<TransferTemplate> = mainRepository.getTransferTemplates()
    fun getSwapTemplates(): List<SwapTemplate> = mainRepository.getSwapTemplates()
    fun addTransferTemplate(template: TransferTemplate) = mainRepository.addTransferTemplate(template)
    fun addSwapTemplate(template: SwapTemplate) = mainRepository.addSwapTemplate(template)
    fun renameTransferTemplate(template: TransferTemplate, name: String) =
        mainRepository.renameTransferTemplate(template, name)
    fun renameSwapTemplate(template: SwapTemplate, name: String) =
        mainRepository.renameSwapTemplate(template, name)

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
        return state.data.findByOperation(operation)
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
        return calculateFeeForOperations(
            amount,
            PaymentFeeOperationResolver.convertOperations(from, to)
        )
    }

    fun feeForTransferOperation(currency: CurrencyEnum): PaymentFeeModel? {
        return feeForOperations(PaymentFeeOperationResolver.transferOperations(currency))
    }

    fun feeForConvertOperation(from: CurrencyEnum, to: CurrencyEnum): PaymentFeeModel? {
        return feeForOperations(
            PaymentFeeOperationResolver.convertOperations(from, to)
        )
    }

    private fun calculateFeeForOperations(amount: Double, operations: List<String>): Double {
        if (amount <= 0.0) return 0.0
        return operations.asSequence()
            .mapNotNull { feeForOperation(it) }
            .map { it.calculateFee(amount) }
            .maxOrNull()
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

    fun latestTransactions(currencyEnum: CurrencyEnum? = null) {
        val currencies = currencyEnum?.let(::listOf) ?: CurrencyEnum.supportedValues.toList()
        mainRepository.history(currencies, getFromTime(), getToTime(), 8, 0).onEach { uiState ->
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
        mainRepository.history(
            listOf(
                CurrencyEnum.SOM,
                CurrencyEnum.ESOM,
                CurrencyEnum.USDT_TRC20
            ),
            getFromTime(),
            getToTime(),
            100,
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
                    supportMessagesUiState.update { current ->
                        val pending = current.pendingMessages.filterNot { pendingMessage ->
                            state.data.any { remoteMessage -> pendingMessage.isSameSentMessage(remoteMessage) }
                        }
                        current.copy(
                            pendingMessages = pending,
                            cachedMessages = mergeMessages(state.data + pending)
                        )
                    }
                    _messages.value = UiState.Success(supportMessagesUiState.value.cachedMessages)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        val currentState = supportMessagesUiState.value
        val pendingMessage = SupportModel(
            id = currentState.nextPendingMessageId,
            ticketId = null,
            text = text,
            role = SupportRole.USER,
            createdAt = System.currentTimeMillis()
        )

        supportMessagesUiState.update {
            val pending = it.pendingMessages + pendingMessage
            it.copy(
                pendingMessages = pending,
                cachedMessages = mergeMessages(it.cachedMessages + pendingMessage),
                nextPendingMessageId = it.nextPendingMessageId - 1
            )
        }
        _messages.value = UiState.Success(supportMessagesUiState.value.cachedMessages)
        _sendMessage.value = UiState.Loading()

        mainRepository.sendMessage(text).onEach { state ->
            when (state) {
                is UiState.Loading -> _sendMessage.value = state
                is UiState.Error -> {
                    supportMessagesUiState.update {
                        it.copy(
                            pendingMessages = it.pendingMessages.filterNot { message -> message.id == pendingMessage.id },
                            cachedMessages = it.cachedMessages.filterNot { message -> message.id == pendingMessage.id }
                        )
                    }
                    _messages.value = UiState.Success(supportMessagesUiState.value.cachedMessages)
                    _sendMessage.value = state
                }
                is UiState.Success -> {
                    supportMessagesUiState.update {
                        val pending = it.pendingMessages.filterNot { message ->
                            message.id == pendingMessage.id || message.isSameSentMessage(state.data)
                        }
                        val cached = it.cachedMessages.filterNot { message ->
                            message.id == pendingMessage.id || message.isSameSentMessage(state.data)
                        }
                        it.copy(
                            pendingMessages = pending,
                            cachedMessages = mergeMessages(cached + state.data)
                        )
                    }
                    _messages.value = UiState.Success(supportMessagesUiState.value.cachedMessages)
                    _sendMessage.value = state
                    getMessages(showLoading = false)
                }
            }
        }.launchIn(viewModelScope)
    }
    fun loadNotifications() {
        mainRepository.getNotifications().onEach {
            _notifications.value = it
            if (it is UiState.Success) {
                val seenIds = mainRepository.getSeenNotificationIds()
                _hasUnreadNotifications.value = it.data.any { notification -> notification.id.toString() !in seenIds }
            }
        }.launchIn(viewModelScope)
    }

    fun markNotificationsSeen(notifications: List<NotificationModel>) {
        if (notifications.isEmpty()) return
        val seenIds = mainRepository.getSeenNotificationIds() + notifications.map { it.id.toString() }
        mainRepository.setSeenNotificationIds(seenIds)
        _hasUnreadNotifications.value = false
    }

    fun toggleBalancesVisibility() {
        val visible = !(_balancesVisible.value ?: true)
        mainRepository.setBalancesVisible(visible)
        _balancesVisible.value = visible
    }

    fun getThemeMode(): Int = mainRepository.getThemeMode()

    fun setThemeMode(mode: Int) = mainRepository.setThemeMode(mode)

    fun isWalletHistoryExpanded(): Boolean = mainRepository.isWalletHistoryExpanded()

    fun setWalletHistoryExpanded(expanded: Boolean) =
        mainRepository.setWalletHistoryExpanded(expanded)

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
