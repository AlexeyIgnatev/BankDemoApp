package com.esom.bank.retrofit.api

import com.esom.bank.screens.chat.dto.SendMessageDto
import com.esom.bank.screens.chat.dto.SupportDto
import com.esom.bank.screens.history.dto.GetTransactionsDto
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.notification.dto.NotificationDto
import com.esom.bank.screens.swap.dto.ConvertDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApi {
    @GET("users/info")
    suspend fun getUserInfo(): Response<UserDto>

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

    @GET("blockchain-config/settings")
    suspend fun getSettings(): Response<FeeDto>

    @GET("/api/support/message")
    suspend fun getMessages(
        @Query("take") take: Int = 40
    ): Response<List<SupportDto>>

    @POST("/api/support/message")
    suspend fun sendMessage(
        @Body sendMessageDto: SendMessageDto
    ): Response<SupportDto>

    @GET("/notifications")
    suspend fun getNotifications(
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 40
    ): Response<List<NotificationDto>>
}