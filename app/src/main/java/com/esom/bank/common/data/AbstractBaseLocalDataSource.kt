package com.esom.bank.common.data

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

abstract class AbstractBaseLocalDataSource {
    fun <T> safeDbCall(
        call: suspend () -> T
    ): Flow<ApiResponse<T>> = flow {
        val result = try {
            val response: T = call()
            if (response != null) ApiResponse.Success(data = response, 0) else ApiResponse.Error(
                R.string.something_went_wrong
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Firebase.crashlytics.recordException(e)
            ApiResponse.Error(R.string.something_went_wrong)
        }
        emit(result)
    }

    fun <T> safeDbFlow(
        call: () -> Flow<T>
    ): Flow<ApiResponse<T>> = call().map { response ->
        if (response != null) {
            ApiResponse.Success(data = response, 0)
        } else {
            ApiResponse.Error(
                R.string.something_went_wrong
            )
        }
    }.catch {
        it.printStackTrace()
        Firebase.crashlytics.recordException(it)
        emit(ApiResponse.Error(R.string.something_went_wrong))
    }

    fun <T> Flow<ApiResponse<List<T>>>.mapFirstOrNull() = map {
        when (it) {
            is ApiResponse.Error -> return@map ApiResponse.Error(it.message)
            is ApiResponse.Success -> return@map ApiResponse.Success(it.data.firstOrNull(), 0)
        }
    }
}