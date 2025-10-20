package com.esom.bank.screens.main.data

import android.content.Context
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import com.esom.bank.screens.chat.model.SupportModel
import com.esom.bank.screens.chat.model.toModel
import com.esom.bank.screens.history.data.HistoryLocalDataSource
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.toModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.FeeModel
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.main.model.toModel
import com.esom.bank.screens.notification.model.NotificationModel
import com.esom.bank.screens.notification.model.toModel
import com.esom.bank.screens.swap.dto.ConvertDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MainRepository {
    fun isAuthenticated(): Boolean
    fun authenticate(login: String, password: String): Flow<UiState<UserModel>>
    fun getUserInfo(): Flow<UiState<UserModel>>

    fun getSettings(): Flow<UiState<FeeModel>>

    fun convert(
        from: CurrencyEnum,
        to: CurrencyEnum,
        fromAmount: Double
    ): Flow<UiState<Unit>>

    fun transferFromFiat(
        amount: Double,
    ): Flow<UiState<Unit>>

    fun transferToFiat(
        amount: Double
    ): Flow<UiState<Unit>>

    fun transferToUser(
        amount: Double,
        phone: String,
        address: String? = null,
        currencyEnum: CurrencyEnum
    ): Flow<UiState<Unit>>

    fun history(
        currencyEnum: List<CurrencyEnum>? = null, fromTime: Long, toTime: Long,
        take: Int, skip: Int
    ): Flow<UiState<List<TransactionModel?>>>

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

    fun clearAllLocalData()
}

class MainRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainCloudDataSource: MainCloudDataSource,
    private val authLocalDataSource: AuthLocalDataSource,
    private val historyLocalDataSource: HistoryLocalDataSource
) : MainRepository {
    override fun isAuthenticated(): Boolean =
        authLocalDataSource.getLogin() != null && authLocalDataSource.getPassword() != null

    override fun authenticate(login: String, password: String): Flow<UiState<UserModel>> = flow {
        authLocalDataSource.setLogin(login)
        authLocalDataSource.setPassword(password)
        emitAll(
            mainCloudDataSource.getUserInfo().map { response ->
                when (response) {
                    is ApiResponse.Success -> return@map UiState.Success(response.data.toModel())
                    is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
                }
            }
        )
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

    override fun convert(
        from: CurrencyEnum,
        to: CurrencyEnum,
        fromAmount: Double
    ): Flow<UiState<Unit>> =
        mainCloudDataSource.convert(ConvertDto(from, to, fromAmount)).map {
            when (it) {
                is ApiResponse.Success -> return@map UiState.Success(Unit)
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
            }
        }


    override fun transferFromFiat(amount: Double): Flow<UiState<Unit>> =
        mainCloudDataSource.fiatToCrypto(amount).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(Unit)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun transferToFiat(amount: Double): Flow<UiState<Unit>> =
        mainCloudDataSource.cryptoToFiat(amount).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(Unit)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }

    override fun transferToUser(
        amount: Double,
        phone: String,
        address: String?,
        currencyEnum: CurrencyEnum
    ): Flow<UiState<Unit>> =
        mainCloudDataSource.transfer(amount, phone, address, currencyEnum).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(Unit)
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

    override fun clearAllLocalData() {
        authLocalDataSource.clearAuthData()
        historyLocalDataSource.clearAllHistoryData()
    }

}