package com.esom.bank.screens.main.data

import android.content.Context
import com.esom.bank.BuildConfig
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.main.model.toModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

interface MainRepository {
    fun getMe(): Flow<UiState<UserModel>>
    fun getAllUsers(): Flow<UiState<List<UserModel>>>

    fun transferFromFiat(
        amount: Double,
    ): Flow<UiState<Unit>>

    fun transferToFiat(
        amount: Double
    ): Flow<UiState<Unit>>

    fun getTokenBalance(): Flow<UiState<Double>>

    fun transferToUser(
        amount: Double,
        address: String
    ): Flow<UiState<Unit>>
}

class MainRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainCloudDataSource: MainCloudDataSource,
    private val blockchainCloudDataSource: BlockchainCloudDataSource
) : MainRepository {
    override fun getMe(): Flow<UiState<UserModel>> {
        return mainCloudDataSource.getMe().map {
            when (it) {
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
                is ApiResponse.Success -> return@map UiState.Success(it.data.toModel())
            }
        }
    }

    override fun getAllUsers(): Flow<UiState<List<UserModel>>> {
        return mainCloudDataSource.getAllUsers().map {
            when (it) {
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
                is ApiResponse.Success -> return@map UiState.Success(it.data.map { u -> u.toModel() })
            }
        }
    }

    override fun transferFromFiat(amount: Double): Flow<UiState<Unit>> = flow {
        val meRes = mainCloudDataSource.getMe().last()
        if (meRes is ApiResponse.Error) {
            emit(UiState.Error(meRes.toString(context)))
            return@flow
        }

        val adminRes = mainCloudDataSource.getAdmin().last()
        if (adminRes is ApiResponse.Error) {
            emit(UiState.Error(adminRes.toString(context)))
            return@flow
        }

        val balance = (meRes as ApiResponse.Success).data.balance
        val newBalance = balance - amount

        val changeConfigRes =
            mainCloudDataSource.changeBalance(BuildConfig.USER_ID, newBalance).last()
        if (changeConfigRes is ApiResponse.Error) {
            emit(UiState.Error(changeConfigRes.toString(context)))
            return@flow
        }

        val transferFromFiatRes = blockchainCloudDataSource.transferFromFiat(
            meRes.data.address,
            BigInteger((amount * 0.999 * 10.0.pow(18)).toDecimalNotationString()),
            (adminRes as ApiResponse.Success).data.privateKey
        ).last()

        if (transferFromFiatRes is ApiResponse.Error) {
            emit(UiState.Error(transferFromFiatRes.toString(context)))
            return@flow
        }
        emit(UiState.Success(Unit))
    }

    override fun transferToFiat(amount: Double): Flow<UiState<Unit>> = flow {
        val meRes = mainCloudDataSource.getMe().last()
        if (meRes is ApiResponse.Error) {
            emit(UiState.Error(meRes.toString(context)))
            return@flow
        }

        val transferToFiatRes = blockchainCloudDataSource.transferToFiat(
            BigInteger((amount * 10.0.pow(18)).toDecimalNotationString()),
            (meRes as ApiResponse.Success).data.privateKey
        ).last()

        if (transferToFiatRes is ApiResponse.Error) {
            emit(UiState.Error(transferToFiatRes.toString(context)))
            return@flow
        }

        val balance = meRes.data.balance
        val newBalance = balance + amount * 0.999

        val changeConfigRes =
            mainCloudDataSource.changeBalance(BuildConfig.USER_ID, newBalance).last()
        if (changeConfigRes is ApiResponse.Error) {
            emit(UiState.Error(changeConfigRes.toString(context)))
            return@flow
        }
        emit(UiState.Success(Unit))
    }

    override fun getTokenBalance(): Flow<UiState<Double>> = flow {
        val meRes = mainCloudDataSource.getMe().last()
        if (meRes is ApiResponse.Error) {
            emit(UiState.Error(meRes.toString(context)))
            return@flow
        }

        emitAll(blockchainCloudDataSource.getBalance(
            (meRes as ApiResponse.Success).data.address
        ).map {
            when (it) {
                is ApiResponse.Error -> return@map UiState.Error(it.toString(context))
                is ApiResponse.Success -> return@map UiState.Success(
                    it.data.toDouble() / 10.0.pow(
                        18
                    )
                )
            }
        })
    }

    override fun transferToUser(amount: Double, address: String): Flow<UiState<Unit>> = flow {
        val meRes = mainCloudDataSource.getMe().last()
        if (meRes is ApiResponse.Error) {
            emit(UiState.Error(meRes.toString(context)))
            return@flow
        }

        val transferToUserRes = blockchainCloudDataSource.transferToUser(
            address,
            BigInteger((amount * 10.0.pow(18)).toDecimalNotationString()),
            (meRes as ApiResponse.Success).data.privateKey
        ).last()

        if (transferToUserRes is ApiResponse.Error) {
            emit(UiState.Error(transferToUserRes.toString(context)))
            return@flow
        }

        emit(UiState.Success(Unit))
    }

    private fun Double.toDecimalNotationString() =
        String.format(
            Locale.getDefault(),
            "%.99f",
            this
        )
            .trimEnd('0').replace(",", ".").trimEnd('.')
}