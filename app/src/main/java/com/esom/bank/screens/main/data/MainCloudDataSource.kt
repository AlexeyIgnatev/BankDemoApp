package com.esom.bank.screens.main.data

import com.esom.bank.common.data.AbstractBaseCloudDataSource
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.retrofit.api.ServerApi
import com.esom.bank.screens.history.dto.GetTransactionsDto
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MainCloudDataSource {
    fun getUserInfo(): Flow<ApiResponse<UserDto>>
    fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>>
    fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>>
    fun transfer(amount: Double, phone: String, address: String? = null, currencyEnum: CurrencyEnum): Flow<ApiResponse<StatusDto>>
    fun history(currencyEnum: List<CurrencyEnum>? = null, fromTime: Long, toTime: Long,
                take: Int, skip: Int): Flow<ApiResponse<List<TransactionDto>>>
}

class MainCloudDataSourceImpl @Inject constructor(
    private val serverApi: ServerApi
) : MainCloudDataSource,
    AbstractBaseCloudDataSource() {
    override fun getUserInfo(): Flow<ApiResponse<UserDto>> = safeApiCall {
        serverApi.getUserInfo()
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
    ): Flow<ApiResponse<List<TransactionDto>>> = safeApiCall {
        serverApi.history(GetTransactionsDto(
            currencyEnum, fromTime, toTime, take, skip
        ))
    }
}
