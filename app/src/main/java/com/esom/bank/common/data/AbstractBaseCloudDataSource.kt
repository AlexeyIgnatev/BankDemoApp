package com.esom.bank.common.data

import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.ErrorResponse
import com.google.gson.Gson
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
                emit(ApiResponse.Success(data = response.body()!!, code = response.code()))
            } else {
                val errorData = try {
                    Gson().fromJson(
                        response.errorBody()?.string(), ErrorResponse::class.java
                    )
                } catch (e: Exception) {
                    null
                }
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        data = errorData,
                        code = response.code()
                    )
                )
            }
        } catch (e: HttpException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.server_error))
        } catch (e: IOException) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.check_internet))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(ApiResponse.Error(R.string.something_went_wrong))
        }
    }
}