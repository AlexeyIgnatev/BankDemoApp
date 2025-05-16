package com.esom.bank.screens.main.data

import com.esom.bank.common.data.AbstractBaseCloudDataSource
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.retrofit.api.ServerApi
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.SwapDto
import com.esom.bank.screens.main.dto.TransferDto
import com.esom.bank.screens.main.dto.UserDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MainCloudDataSource {
    fun getUserInfo(): Flow<ApiResponse<UserDto>>
    fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>>
    fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>>
    fun transfer(amount: Double, phone: String): Flow<ApiResponse<StatusDto>>
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

    override fun transfer(amount: Double, phone: String): Flow<ApiResponse<StatusDto>> =
        safeApiCall {
            serverApi.transfer(TransferDto(amount, phone))
        }
}
