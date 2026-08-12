package com.esom.bank.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatReceiptPersonName
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.dto.RecipientLookupRequestDto
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.transfer.model.TransferUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransferUiStateViewModel @Inject constructor(
    private val repository: MainRepository?
) : ViewModel() {
    constructor() : this(null)

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()
    private val recipientRequests = MutableSharedFlow<RecipientLookupRequestDto>(extraBufferCapacity = 1)

    init {
        repository?.let { recipientRepository -> viewModelScope.launch {
            recipientRequests
                .debounce(350)
                .distinctUntilChanged()
                .transformLatest { request ->
                    recipientRepository.recipientInfo(request)
                        .onStart {
                            _uiState.update {
                                it.copy(recipientLookupKey = request.lookupKey())
                            }
                        }
                        .map { request.lookupKey() to it }
                        .collect { emit(it) }
                }
                .collect { (lookupKey, state) ->
                    if (_uiState.value.recipientLookupKey != lookupKey) {
                        return@collect
                    }
                    if (state is UiState.Success) {
                        val data = state.data
                        val fullName = if (!data.lastName.isNullOrBlank()) {
                            listOf(data.lastName, data.firstName, data.middleName)
                                .map(String?::orEmpty)
                                .filter(String::isNotBlank)
                                .joinToString(" ")
                        } else {
                            data.fullName.orEmpty()
                        }
                        _uiState.update { it.copy(recipientName = formatReceiptPersonName(fullName)) }
                    }
                }
        } }
    }

    fun initialize(currency: CurrencyEnum, contact: String = "") {
        val toPhoneNumber = contact
            .takeIf { it.isNotBlank() }
            ?.let(::isPhoneContact)
            ?: (currency != CurrencyEnum.USDT_TRC20)
        updateCurrency(currency, toPhoneNumber)
    }

    fun setInitialRecipientName(value: String) {
        _uiState.update { it.copy(recipientName = formatReceiptPersonName(value)) }
    }

    fun lookupRecipient(phone: String?, address: String?, currency: CurrencyEnum) {
        recipientRequests.tryEmit(
            RecipientLookupRequestDto(
                phoneNumber = phone,
                address = address,
                currency = currency
            )
        )
    }

    fun clearRecipientName() {
        _uiState.update { it.copy(recipientName = "", recipientLookupKey = "") }
    }

    fun updateCurrency(currency: CurrencyEnum, toPhoneNumber: Boolean) {
        _uiState.update {
            it.copy(
                fromCurrency = currency,
                toPhoneNumber = toPhoneNumber,
                currencyPanelOptions = CurrencyEnum.supportedValues.filterNot { value -> value == currency }
            )
        }
    }

    fun setRecipientMode(toPhoneNumber: Boolean) =
        _uiState.update { it.copy(toPhoneNumber = toPhoneNumber) }

    fun toggleRecipientMode() =
        _uiState.update { it.copy(toPhoneNumber = !it.toPhoneNumber) }

    fun toggleCurrencyPanel() =
        _uiState.update { it.copy(currencyPanelShown = !it.currencyPanelShown) }

    fun setPendingTemplate(template: TransferTemplate?) =
        _uiState.update { it.copy(pendingTemplate = template) }

    fun consumePendingTemplate(): TransferTemplate? {
        val template = _uiState.value.pendingTemplate
        _uiState.update { it.copy(pendingTemplate = null) }
        return template
    }

    fun startAutomaticRepeat(): Boolean {
        if (_uiState.value.automaticRepeatStarted) return false
        _uiState.update { it.copy(automaticRepeatStarted = true) }
        return true
    }

    private fun isPhoneContact(contact: String): Boolean {
        val phoneCharactersOnly = contact.all {
            it.isDigit() || it == '+' || it == ' ' || it == '(' || it == ')' || it == '-'
        }
        return phoneCharactersOnly && contact.count(Char::isDigit) >= 7
    }

    private fun RecipientLookupRequestDto.lookupKey(): String =
        listOf(phoneNumber.orEmpty(), address.orEmpty(), currency.name).joinToString("|")
}
