package com.esom.bank.screens.main.data

import com.esom.bank.common.data.AbstractBaseCloudDataSource
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.utils.PhoneInfo
import com.esom.bank.retrofit.api.ServerApi
import com.esom.bank.screens.chat.dto.SendMessageDto
import com.esom.bank.screens.chat.dto.SupportDto
import com.esom.bank.screens.history.dto.GetTransactionsDto
import com.esom.bank.screens.history.dto.ReceiptRequestDto
import com.esom.bank.screens.history.dto.ReceiptResponseDto
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.FcmTokenDto
import com.esom.bank.screens.main.dto.PaymentFeeDto
import com.esom.bank.screens.main.dto.PushSettingsDto
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.notification.dto.FinancialReportRequestDto
import com.esom.bank.screens.notification.dto.FinancialReportResponseDto
import com.esom.bank.screens.notification.dto.NotificationDto
import com.esom.bank.screens.swap.dto.ConvertDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MainCloudDataSource {
    fun getUserInfo(): Flow<ApiResponse<UserDto>>
    fun getSettings(): Flow<ApiResponse<FeeDto>>
    fun getFees(): Flow<ApiResponse<List<PaymentFeeDto>>>
    fun convert(convert: ConvertDto): Flow<ApiResponse<StatusDto>>
    fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>>
    fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>>
    fun transfer(amount: Double, phone: String, address: String? = null, currencyEnum: CurrencyEnum): Flow<ApiResponse<StatusDto>>
    fun history(currencyEnum: List<CurrencyEnum>? = null, fromTime: Long, toTime: Long,
                take: Int, skip: Int): Flow<ApiResponse<List<TransactionDto?>>>
    fun receipt(transactionId: Long, conversionSide: ConversionSide? = null): Flow<ApiResponse<ReceiptResponseDto>>
    fun getMessages(): Flow<ApiResponse<List<SupportDto>>>
    fun sendMessage(text: String): Flow<ApiResponse<SupportDto>>
    fun getNotifications(): Flow<ApiResponse<List<NotificationDto>>>
    fun sendFinancialReport(
        email: String? = null,
        fromTime: Long? = null,
        toTime: Long? = null
    ): Flow<ApiResponse<FinancialReportResponseDto>>
    fun sendFcmToken(token: String): Flow<ApiResponse<Unit>>
    fun updatePushSettings(pushEnabled: Boolean): Flow<ApiResponse<Unit>>
}

class MainCloudDataSourceImpl @Inject constructor(
    private val serverApi: ServerApi
) : MainCloudDataSource,
    AbstractBaseCloudDataSource() {
    override fun getUserInfo(): Flow<ApiResponse<UserDto>> = safeApiCall {
        serverApi.getUserInfo(PhoneInfo.getFormattedPhoneInfo())
    }

    override fun getSettings(): Flow<ApiResponse<FeeDto>> = safeApiCall {
        serverApi.getSettings()
    }

    override fun getFees(): Flow<ApiResponse<List<PaymentFeeDto>>> = safeApiCall {
        serverApi.getFees()
    }

    override fun convert(convert: ConvertDto): Flow<ApiResponse<StatusDto>>  = safeApiCall {
        serverApi.convert(convert)
    }

    override fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>> = safeApiCall {
        serverApi.fiatToCrypto(SwapDto(amount))
    }

    override fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>> = safeApiCall {
        serverApi.cryptoToFiat(SwapDto(amount))
    }

    override fun transfer(amount: Double, phone: String, address: String?, currencyEnum: CurrencyEnum): Flow<ApiResponse<StatusDto>> =
        safeApiCall {
            serverApi.transfer(TransferDto(amount, phone, address, currencyEnum))
        }

    override fun history(
        currencyEnum: List<CurrencyEnum>?,
        fromTime: Long,
        toTime: Long,
        take: Int,
        skip: Int
    ): Flow<ApiResponse<List<TransactionDto?>>> = safeApiCall {
        serverApi.history(GetTransactionsDto(
            currencyEnum, fromTime, toTime, take, skip
        ))
    }

    override fun receipt(
        transactionId: Long,
        conversionSide: ConversionSide?
    ): Flow<ApiResponse<ReceiptResponseDto>> = safeApiCall {
        serverApi.receipt(
            ReceiptRequestDto(
                transactionId = transactionId,
                conversionSide = conversionSide
            )
        )
    }

    override fun getMessages(): Flow<ApiResponse<List<SupportDto>>> = safeApiCall {
        serverApi.getMessages()
    }

    override fun sendMessage(text: String): Flow<ApiResponse<SupportDto>> = safeApiCall {
        serverApi.sendMessage(SendMessageDto(text))
    }

    override fun getNotifications(): Flow<ApiResponse<List<NotificationDto>>> = safeApiCall {
        serverApi.getNotifications()
    }

    override fun sendFinancialReport(
        email: String?,
        fromTime: Long?,
        toTime: Long?
    ): Flow<ApiResponse<FinancialReportResponseDto>> = safeApiCall {
        serverApi.sendFinancialReport(
            FinancialReportRequestDto(
                email = email,
                fromTime = fromTime,
                toTime = toTime
            )
        )
    }

    override fun sendFcmToken(token: String): Flow<ApiResponse<Unit>> = safeUnitApiCall {
        serverApi.sendFcmToken(FcmTokenDto(token = token))
    }

    override fun updatePushSettings(pushEnabled: Boolean): Flow<ApiResponse<Unit>> = safeUnitApiCall {
        serverApi.updatePushSettings(PushSettingsDto(pushEnabled = pushEnabled))
    }
}
