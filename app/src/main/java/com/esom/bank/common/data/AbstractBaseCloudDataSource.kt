package com.esom.bank.common.data

import android.util.Log
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
    fun <T> safeApiCall(
        exposeUnexpectedException: Boolean = false,
        apiToBeCalled: suspend () -> Response<T>,
    ): Flow<ApiResponse<T>> = flow {
        try {
            val response: Response<T> = apiToBeCalled()
            Log.d("ApiCall", "response=${response.code()} url=${response.raw().request.url.encodedPath}")

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
                val request = response.raw().request
                val rawErrorBody = response.errorBody()?.string()
                val errorData = ErrorResponse.fromJson(
                    rawErrorBody,
                    response.code()
                )
                Log.e(
                    "ApiCall",
                    "error response=${response.code()} method=${request.method} " +
                        "url=${request.url.encodedPath} " +
                        "message=${errorData?.message ?: "<empty>"} " +
                        "bodyLength=${rawErrorBody?.length ?: 0}"
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
            emit(ApiResponse.Error(R.string.logged_out))
        } catch (e: HttpException) {
            emit(ApiResponse.Error(R.string.server_error))
        } catch (e: IOException) {
            Log.e("ApiCall", "network request failed: ${e::class.java.simpleName}: ${e.message}", e)
            if (exposeUnexpectedException) {
                emit(
                    ApiResponse.Error(
                        R.string.something_went_wrong,
                        data = ErrorResponse(message = safeDiagnostic(e)),
                    ),
                )
            } else {
                emit(ApiResponse.Error(R.string.check_internet))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "ApiCall",
                "request failed: ${e::class.java.simpleName}: ${e.message}",
                e,
            )
            emit(
                ApiResponse.Error(
                    R.string.something_went_wrong,
                    data = if (exposeUnexpectedException) {
                        ErrorResponse(message = safeDiagnostic(e))
                    } else {
                        null
                    },
                ),
            )
        }
    }

    private fun safeDiagnostic(error: Exception): String {
        val detail = (error.message ?: "no details")
            .replace(
                Regex("(?i)(password|token|authorization|secret)(\\s*[:=]\\s*)[^,\\s}]+"),
            ) { match ->
                "${match.groupValues[1]}${match.groupValues[2]}[redacted]"
            }
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .take(180)
        return "${error::class.java.simpleName}: $detail"
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
            emit(ApiResponse.Error(R.string.logged_out))
        } catch (e: HttpException) {
            emit(ApiResponse.Error(R.string.server_error))
        } catch (e: IOException) {
            emit(ApiResponse.Error(R.string.check_internet))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "ApiCall",
                "unit request failed: ${e::class.java.simpleName}: ${e.message}",
                e,
            )
            emit(ApiResponse.Error(R.string.something_went_wrong))
        }
    }
}
