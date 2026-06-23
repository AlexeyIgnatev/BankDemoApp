package com.esom.bank.retrofit.api

import com.esom.bank.screens.chat.dto.SendMessageDto
import com.esom.bank.screens.chat.dto.SupportDto
import com.esom.bank.screens.history.dto.GetTransactionsDto
import com.esom.bank.screens.history.dto.ReceiptRequestDto
import com.esom.bank.screens.history.dto.ReceiptResponseDto
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.FcmTokenDto
import com.esom.bank.screens.main.dto.PaymentFeeDto
import com.esom.bank.screens.main.dto.PushSettingsDto
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.notification.dto.FinancialReportRequestDto
import com.esom.bank.screens.notification.dto.FinancialReportResponseDto
import com.esom.bank.screens.notification.dto.NotificationDto
import com.esom.bank.screens.swap.dto.ConvertDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApi {
    @GET("users/info")
    suspend fun getUserInfo(
        @Query("device") device: String
    ): Response<UserDto>

    @POST("payments/convert")
    suspend fun convert(
        @Body convertDto: ConvertDto
    ): Response<StatusDto>

    @POST("payments/fiat-to-crypto")
    suspend fun fiatToCrypto(
        @Body swapDto: SwapDto
    ): Response<StatusDto>

    @POST("payments/crypto-to-fiat")
    suspend fun cryptoToFiat(
        @Body swapDto: SwapDto
    ): Response<StatusDto>

    @POST("payments/transfer")
    suspend fun transfer(
        @Body transferDto: TransferDto
    ): Response<StatusDto>

    @POST("payments/history")
    suspend fun history(
        @Body transactionsDto: GetTransactionsDto
    ): Response<List<TransactionDto?>>

    @POST("payments/receipt")
    suspend fun receipt(
        @Body receiptRequestDto: ReceiptRequestDto
    ): Response<ReceiptResponseDto>

    @GET("blockchain-config/settings")
    suspend fun getSettings(): Response<FeeDto>

    @GET("payments/fees")
    suspend fun getFees(): Response<List<PaymentFeeDto>>

    @GET("/support/history")
    suspend fun getMessages(): Response<List<SupportDto>>

    @POST("/support/message")
    suspend fun sendMessage(
        @Body sendMessageDto: SendMessageDto
    ): Response<SupportDto>

    @POST("/users/fcm-token")
    suspend fun sendFcmToken(
        @Body requestDto: FcmTokenDto
    ): Response<Unit>

    @PATCH("/users/push-settings")
    suspend fun updatePushSettings(
        @Body requestDto: PushSettingsDto
    ): Response<Unit>

    @GET("/notifications")
    suspend fun getNotifications(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 40
    ): Response<List<NotificationDto>>

    @POST("notifications/financial-report")
    suspend fun sendFinancialReport(
        @Body requestDto: FinancialReportRequestDto
    ): Response<FinancialReportResponseDto>
}
