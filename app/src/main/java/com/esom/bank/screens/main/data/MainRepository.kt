package com.esom.bank.screens.main.data

import android.content.Context
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import com.esom.bank.screens.chat.model.SupportModel
import com.esom.bank.screens.chat.model.toModel
import com.esom.bank.screens.history.data.HistoryLocalDataSource
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.toModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.FeeModel
import com.esom.bank.screens.main.model.PaymentFeeModel
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.main.model.toPaymentFeeModels
import com.esom.bank.screens.main.model.toModel
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.messaging.data.MessagingLocalDataSource
import com.esom.bank.screens.notification.model.NotificationModel
import com.esom.bank.screens.notification.model.toModel
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import com.esom.bank.screens.pinCreate.data.LockType
import com.esom.bank.screens.qr.data.PrimaryCurrencyLocalDataSource
import com.esom.bank.screens.swap.dto.ConvertDto
import com.esom.bank.screens.main.data.RecentTemplateLocalDataSource
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.transfer.model.TransferTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MainRepository {
    fun isAuthenticated(): Boolean
    fun getLogin(): String
    fun hasLock(): Boolean
    fun hasPin(): Boolean
    fun hasPattern(): Boolean
    fun getLockType(): LockType?
    fun savePin(pin: String)
    fun verifyPin(pin: String): Boolean
    fun savePattern(pattern: List<Int>)
    fun verifyPattern(pattern: List<Int>): Boolean
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)
    fun getPrimaryCurrency(): CurrencyEnum
    fun setPrimaryCurrency(currency: CurrencyEnum)
    fun getTransferTemplates(): List<TransferTemplate>
    fun getSwapTemplates(): List<SwapTemplate>
    fun addTransferTemplate(template: TransferTemplate)
    fun addSwapTemplate(template: SwapTemplate)
    fun renameTransferTemplate(template: TransferTemplate, name: String)
    fun renameSwapTemplate(template: SwapTemplate, name: String)
    fun getSeenNotificationIds(): Set<String>
    fun setSeenNotificationIds(ids: Set<String>)
    fun areBalancesVisible(): Boolean
    fun setBalancesVisible(visible: Boolean)
    fun getThemeMode(): Int
    fun setThemeMode(mode: Int)
    fun isWalletHistoryExpanded(): Boolean
    fun setWalletHistoryExpanded(expanded: Boolean)
    fun authenticate(login: String, password: String): Flow<UiState<UserModel>>
    fun getUserInfo(): Flow<UiState<UserModel>>

    fun getSettings(): Flow<UiState<FeeModel>>
    fun getFees(): Flow<UiState<List<PaymentFeeModel>>>

    fun convert(
        from: CurrencyEnum,
        to: CurrencyEnum,
        fromAmount: Double
    ): Flow<UiState<StatusDto>>

    fun transferFromFiat(
        amount: Double,
    ): Flow<UiState<StatusDto>>

    fun transferToFiat(
        amount: Double
    ): Flow<UiState<StatusDto>>

    fun transferToUser(
        amount: Double,
        phone: String,
        address: String? = null,
        currencyEnum: CurrencyEnum
    ): Flow<UiState<StatusDto>>

    fun history(
        currencyEnum: List<CurrencyEnum>? = null, fromTime: Long, toTime: Long,
        take: Int, skip: Int
    ): Flow<UiState<List<TransactionModel?>>>
    fun receipt(transactionId: Long, conversionSide: ConversionSide? = null): Flow<UiState<ReceiptModel>>

    fun getWithoutTransactions(): Boolean
    fun setWithoutTransactions(without: Boolean)

    fun getCurrency(): List<CurrencyEnum>
    fun setCurrency(currency: List<CurrencyEnum>)

    fun getFromTime(): Long
    fun setFromTime(time: Long)

    fun getToTime(): Long
    fun setToTime(time: Long)

    fun getNotifications(): Flow<UiState<List<NotificationModel>>>
    fun getMessages(): Flow<UiState<List<SupportModel>>>
    fun sendMessage(text: String): Flow<UiState<SupportModel>>
    fun sendFinancialReport(
        email: String? = null,
        fromTime: Long? = null,
        toTime: Long? = null
    ): Flow<UiState<Unit>>

    fun isPushNotificationsEnabled(): Boolean
    fun setPushNotificationsEnabled(enabled: Boolean)
    fun syncPushNotificationsEnabled(enabled: Boolean): Flow<UiState<Unit>>
    fun getFcmToken(): String?
    fun setFcmToken(token: String?)
    fun sendFcmToken(token: String): Flow<UiState<Unit>>
    fun getNextNotificationId(): Int

    fun clearAllLocalData()
}

class MainRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainCloudDataSource: MainCloudDataSource,
    private val authLocalDataSource: AuthLocalDataSource,
    private val historyLocalDataSource: HistoryLocalDataSource,
    private val pinLocalDataSource: PinLocalDataSource,
    private val messagingLocalDataSource: MessagingLocalDataSource,
    private val primaryCurrencyLocalDataSource: PrimaryCurrencyLocalDataSource,
    private val recentTemplateLocalDataSource: RecentTemplateLocalDataSource,
    private val appPreferencesLocalDataSource: AppPreferencesLocalDataSource
) : MainRepository {
    override fun getLogin(): String = authLocalDataSource.getLogin().orEmpty()

    override fun isAuthenticated(): Boolean =
        authLocalDataSource.getLogin() != null && authLocalDataSource.getPassword() != null

    override fun hasLock(): Boolean = pinLocalDataSource.hasLock()

    override fun hasPin(): Boolean = pinLocalDataSource.hasPin()

    override fun hasPattern(): Boolean = pinLocalDataSource.hasPattern()

    override fun getLockType(): LockType? = pinLocalDataSource.getLockType()

    override fun savePin(pin: String) = pinLocalDataSource.savePin(pin)

    override fun verifyPin(pin: String): Boolean = pinLocalDataSource.verifyPin(pin)

    override fun savePattern(pattern: List<Int>) = pinLocalDataSource.savePattern(pattern)

    override fun verifyPattern(pattern: List<Int>): Boolean = pinLocalDataSource.verifyPattern(pattern)

    override fun isBiometricEnabled(): Boolean = pinLocalDataSource.isBio()

    override fun setBiometricEnabled(enabled: Boolean) = pinLocalDataSource.setBio(enabled)

    override fun getPrimaryCurrency(): CurrencyEnum = primaryCurrencyLocalDataSource.get()

    override fun setPrimaryCurrency(currency: CurrencyEnum) = primaryCurrencyLocalDataSource.set(currency)

    override fun getTransferTemplates(): List<TransferTemplate> =
        recentTemplateLocalDataSource.getTransferTemplates()

    override fun getSwapTemplates(): List<SwapTemplate> = recentTemplateLocalDataSource.getSwapTemplates()

    override fun addTransferTemplate(template: TransferTemplate) =
        recentTemplateLocalDataSource.addTransferTemplate(template)

    override fun addSwapTemplate(template: SwapTemplate) = recentTemplateLocalDataSource.addSwapTemplate(template)

    override fun renameTransferTemplate(template: TransferTemplate, name: String) =
        recentTemplateLocalDataSource.renameTransferTemplate(template, name)

    override fun renameSwapTemplate(template: SwapTemplate, name: String) =
        recentTemplateLocalDataSource.renameSwapTemplate(template, name)

    override fun getSeenNotificationIds(): Set<String> =
        appPreferencesLocalDataSource.getSeenNotificationIds()

    override fun setSeenNotificationIds(ids: Set<String>) =
        appPreferencesLocalDataSource.setSeenNotificationIds(ids)

    override fun areBalancesVisible(): Boolean = appPreferencesLocalDataSource.areBalancesVisible()

    override fun setBalancesVisible(visible: Boolean) =
        appPreferencesLocalDataSource.setBalancesVisible(visible)

    override fun getThemeMode(): Int = appPreferencesLocalDataSource.getThemeMode()

    override fun setThemeMode(mode: Int) = appPreferencesLocalDataSource.setThemeMode(mode)

    override fun isWalletHistoryExpanded(): Boolean =
        appPreferencesLocalDataSource.isWalletHistoryExpanded()

    override fun setWalletHistoryExpanded(expanded: Boolean) =
        appPreferencesLocalDataSource.setWalletHistoryExpanded(expanded)

    override fun authenticate(login: String, password: String): Flow<UiState<UserModel>> = flow {
        authLocalDataSource.setLogin(login)
        authLocalDataSource.setPassword(password)
        var authenticated = false
        try {
            emitAll(
                mainCloudDataSource.getUserInfo().map { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            authenticated = true
                            UiState.Success(response.data.toModel())
                        }
                        is ApiResponse.Error -> UiState.Error(response.toString(context))
                    }
                }
            )
        } finally {
            if (!authenticated) authLocalDataSource.clearAuthData()
        }
    }

    override fun getUserInfo(): Flow<UiState<UserModel>> =
        mainCloudDataSource.getUserInfo().map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data.toModel())
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun getSettings(): Flow<UiState<FeeModel>> =
        mainCloudDataSource.getSettings().map { response ->
            when(response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data.toModel())
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun getFees(): Flow<UiState<List<PaymentFeeModel>>> =
        mainCloudDataSource.getFees().map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data.toPaymentFeeModels())
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun convert(
        from: CurrencyEnum,
        to: CurrencyEnum,
        fromAmount: Double
    ): Flow<UiState<StatusDto>> =
        mainCloudDataSource.convert(ConvertDto(from, to, fromAmount)).map {
            when (it) {
                is ApiResponse.Success -> return@map UiState.Success(it.data)
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
            }
        }


    override fun transferFromFiat(amount: Double): Flow<UiState<StatusDto>> =
        mainCloudDataSource.fiatToCrypto(amount).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun transferToFiat(amount: Double): Flow<UiState<StatusDto>> =
        mainCloudDataSource.cryptoToFiat(amount).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun transferToUser(
        amount: Double,
        phone: String,
        address: String?,
        currencyEnum: CurrencyEnum
    ): Flow<UiState<StatusDto>> =
        mainCloudDataSource.transfer(amount, phone, address, currencyEnum).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun history(
        currencyEnum: List<CurrencyEnum>?,
        fromTime: Long,
        toTime: Long,
        take: Int,
        skip: Int
    ): Flow<UiState<List<TransactionModel?>>> =
        mainCloudDataSource.history(currencyEnum, fromTime, toTime, take, skip).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(response.data.toModel())
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun receipt(
        transactionId: Long,
        conversionSide: ConversionSide?
    ): Flow<UiState<ReceiptModel>> =
        mainCloudDataSource.receipt(transactionId, conversionSide).map { response ->
            when (response) {
                is ApiResponse.Success -> {
                    return@map UiState.Success(response.data.toModel(conversionSide))
                }
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun getWithoutTransactions(): Boolean {
        return historyLocalDataSource.getWithoutTransactions()
    }

    override fun setWithoutTransactions(without: Boolean) {
        historyLocalDataSource.setWithoutTransactions(without)
    }

    override fun getCurrency(): List<CurrencyEnum> {
        return historyLocalDataSource.getCurrency()
    }

    override fun setCurrency(currency: List<CurrencyEnum>) {
        historyLocalDataSource.setCurrency(currency)
    }

    override fun getFromTime(): Long {
        return historyLocalDataSource.getFromTime()
    }

    override fun setFromTime(time: Long) {
        historyLocalDataSource.setFromTime(time)
    }

    override fun getToTime(): Long {
        return historyLocalDataSource.getToTime()
    }

    override fun setToTime(time: Long) {
        historyLocalDataSource.setToTime(time)
    }

    override fun getNotifications(): Flow<UiState<List<NotificationModel>>> =
        mainCloudDataSource.getNotifications().map { state ->
            when (state) {
                is ApiResponse.Success -> return@map UiState.Success(state.data.toModel())
                is ApiResponse.Error -> return@map UiState.Error(state.toString(context))
            }
        }

    override fun getMessages(): Flow<UiState<List<SupportModel>>> =
        mainCloudDataSource.getMessages().map {
            when(it) {
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
                is ApiResponse.Success -> {
                    return@map UiState.Success(it.data.toModel())
                }
            }
        }

    override fun sendMessage(text: String): Flow<UiState<SupportModel>> =
        mainCloudDataSource.sendMessage(text).map {
            when(it) {
                is ApiResponse.Error -> return@map  UiState.Error(it.toString(context))
                is ApiResponse.Success -> return@map UiState.Success(it.data.toModel())
            }
        }

    override fun sendFinancialReport(
        email: String?,
        fromTime: Long?,
        toTime: Long?
    ): Flow<UiState<Unit>> =
        mainCloudDataSource.sendFinancialReport(email, fromTime, toTime).map { response ->
            when (response) {
                is ApiResponse.Success -> {
                    if (response.data.successful == true) {
                        UiState.Success(Unit)
                    } else {
                        UiState.Error("Не удалось выгрузить отчет")
                    }
                }
                is ApiResponse.Error -> UiState.Error(response.toString(context))
            }
        }

    override fun isPushNotificationsEnabled(): Boolean =
        messagingLocalDataSource.isPushNotificationsEnabled()

    override fun setPushNotificationsEnabled(enabled: Boolean) {
        messagingLocalDataSource.setPushNotificationsEnabled(enabled)
    }

    override fun syncPushNotificationsEnabled(enabled: Boolean): Flow<UiState<Unit>> = flow {
        messagingLocalDataSource.setPushNotificationsEnabled(enabled)
        emitAll(
            mainCloudDataSource.updatePushSettings(enabled).map { response ->
                when (response) {
                    is ApiResponse.Success -> UiState.Success(Unit)
                    is ApiResponse.Error -> UiState.Error(response.toString(context))
                }
            }
        )
    }

    override fun getFcmToken(): String? =
        messagingLocalDataSource.getFcmToken()

    override fun setFcmToken(token: String?) {
        messagingLocalDataSource.setFcmToken(token)
    }

    override fun sendFcmToken(token: String): Flow<UiState<Unit>> = flow {
        messagingLocalDataSource.setFcmToken(token)
        emitAll(
            mainCloudDataSource.sendFcmToken(token).map { response ->
                when (response) {
                    is ApiResponse.Success -> UiState.Success(Unit)
                    is ApiResponse.Error -> UiState.Error(response.toString(context))
                }
            }
        )
    }

    override fun getNextNotificationId(): Int =
        messagingLocalDataSource.getNextNotificationId()

    override fun clearAllLocalData() {
        authLocalDataSource.clearAuthData()
        historyLocalDataSource.clearAllHistoryData()
        pinLocalDataSource.clearLock()
        messagingLocalDataSource.clearMessagingData()
    }

}
