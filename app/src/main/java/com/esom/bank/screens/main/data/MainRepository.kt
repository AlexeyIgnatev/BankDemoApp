package com.esom.bank.screens.main.data

import android.content.Context
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.UiState
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.main.model.toModel
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

    fun transferFromFiat(
        amount: Double,
    ): Flow<UiState<Unit>>

    fun transferToFiat(
        amount: Double
    ): Flow<UiState<Unit>>

    fun transferToUser(
        amount: Double,
        phone: String
    ): Flow<UiState<Unit>>
}

class MainRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainCloudDataSource: MainCloudDataSource,
    private val authLocalDataSource: AuthLocalDataSource
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

    override fun transferToUser(amount: Double, phone: String): Flow<UiState<Unit>> =
        mainCloudDataSource.transfer(amount, phone).map { response ->
            when (response) {
                is ApiResponse.Success -> return@map UiState.Success(Unit)
                is ApiResponse.Error -> return@map UiState.Error(response.toString(context))
            }
        }
}