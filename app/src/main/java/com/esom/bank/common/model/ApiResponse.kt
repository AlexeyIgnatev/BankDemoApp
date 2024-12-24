package com.esom.bank.common.model

import android.content.Context
import androidx.annotation.StringRes

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val code: Int) : ApiResponse<T>()

    data class Error<T>(
        @StringRes val message: Int,
        val data: ErrorResponse? = null,
        val code: Int? = null
    ) :
        ApiResponse<T>() {
        fun toString(context: Context): String {
            return data?.message ?: context.getString(message)
        }
    }

    fun <T> ApiResponse<T>.toUiState(context: Context): UiState<T> {
        return when (this) {
            is Error -> UiState.Error(context.getString(this.message))
            is Success -> UiState.Success(this.data)
        }
    }
}