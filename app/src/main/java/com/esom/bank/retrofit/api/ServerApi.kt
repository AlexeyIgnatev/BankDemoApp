package com.esom.bank.retrofit.api

import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServerApi {
    @GET("users/info")
    suspend fun getUserInfo(): Response<UserDto>

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
}