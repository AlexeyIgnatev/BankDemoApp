package com.esom.bank.common.data

import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.ErrorResponse
import com.esom.bank.retrofit.exception.NotLoggedInException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

abstract class AbstractBaseCloudDataSource {
    fun <T> safeApiCall(apiToBeCalled: suspend () -> Response<T>): Flow<ApiResponse<T>> = flow {
        try {
            val response: Response<T> = apiToBeCalled()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    emit(ApiResponse.Success(data = body, code = response.code()))
                } else {
                    emit(
                        ApiResponse.Error(
                            R.string.something_went_wrong,
                            data = null,
                            code = response.code()
                        )
                    )
                }
            } else {
                val errorData = ErrorResponse.fromJson(
                    response.errorBody()?.string(),
                    response.code()
                )
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        data = errorData,
                        code = response.code()
                    )
                )
            }
        } catch (e: NotLoggedInException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.logged_out))
        } catch (e: HttpException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.server_error))
        } catch (e: IOException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.check_internet))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.something_went_wrong))
        }
    }

    fun safeUnitApiCall(apiToBeCalled: suspend () -> Response<*>): Flow<ApiResponse<Unit>> = flow {
        try {
            val response = apiToBeCalled()

            if (response.isSuccessful) {
                emit(ApiResponse.Success(data = Unit, code = response.code()))
            } else {
                val errorData = ErrorResponse.fromJson(
                    response.errorBody()?.string(),
                    response.code()
                )
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        data = errorData,
                        code = response.code()
                    )
                )
            }
        } catch (e: NotLoggedInException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.logged_out))
        } catch (e: HttpException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.server_error))
        } catch (e: IOException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.check_internet))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.something_went_wrong))
        }
    }
}
