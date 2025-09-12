package com.esom.bank.screens.main.data

import com.esom.bank.common.model.ApiResponse
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.dto.StatusDto
import com.esom.bank.screens.main.dto.UserDto
import com.esom.bank.screens.main.dto.WalletDto
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MainCloudDataSourceMock @Inject constructor(): MainCloudDataSource {

    override fun getUserInfo(): Flow<ApiResponse<UserDto>> = flow {
        emit(
            ApiResponse.Success(
                UserDto(
                    id = 21028,
                    firstName = "Мадина",
                    middleName = "Салморбековна",
                    lastName = "Байбосунова",
                    phone = "+996 555 687878",
                    email = "madina.b@fkb.kg",
                    privateKey = null,
                    wallets = listOf(
                        WalletDto(
                            currency = CurrencyEnum.SOM,
                            address = "+996 555 687878",
                            balance = 1010.62,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                        WalletDto(
                            currency = CurrencyEnum.USDT_TRC20,
                            address = "TJkTgPifKq1Q9crT9zNCy5dbcXrd71vvof",
                            balance = 0.0,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                        WalletDto(
                            currency = CurrencyEnum.BTC,
                            address = "bc1qycral9w687hqzzh2jpq67e3rt5udj3khrzwqnq",
                            balance = 0.0,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                        WalletDto(
                            currency = CurrencyEnum.ETH,
                            address = "0x604fFa2e0a04f0595206A03AcA898ddAaed900A0",
                            balance = 0.0,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                        WalletDto(
                            currency = CurrencyEnum.ESOM,
                            address = "0x604fFa2e0a04f0595206A03AcA898ddAaed900A0",
                            balance = 99.0,
                            buyRate = 1.0,
                            sellRate = 1.0
                        ),
                    ),
                    platformFee = 0.01
                ),
                code = 200
            )
        )
    }

    override fun fiatToCrypto(amount: Double): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success"), code = 200))
    }

    override fun cryptoToFiat(amount: Double): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success"), code = 200))
    }

    override fun transfer(
        amount: Double,
        phone: String,
        address: String?,
        currencyEnum: CurrencyEnum
    ): Flow<ApiResponse<StatusDto>> = flow {
        emit(ApiResponse.Success(StatusDto("success"), code = 200))
    }

    override fun history(
        currencyEnum: List<CurrencyEnum>?,
        fromTime: Long,
        toTime: Long,
        take: Int,
        skip: Int
    ): Flow<ApiResponse<List<TransactionDto>>> = flow {

        val allTransactions = List(40) { index ->
            val currencies = CurrencyEnum.values()
            val randomCurrency = currencies[index % currencies.size]
            val randomType = TransactionEnum.values()[index % TransactionEnum.values().size]
            val createdAt = System.currentTimeMillis() - index * 60_000L
            TransactionDto(
                currencyEnum = randomCurrency,
                type = randomType,
                amount = (10..1000).random().toDouble(),
                successful = true,
                createdAt = createdAt
            )
        }

        val filtered = allTransactions.filter { tx ->
            val matchCurrency = currencyEnum?.let { it.contains(tx.currencyEnum) } ?: true
            val matchTime = if (take == 5) true else tx.createdAt in fromTime..toTime
            matchCurrency && matchTime
        }

        val result = filtered.drop(skip).take(take)

        emit(ApiResponse.Success(result, code = 200))
    }



}
