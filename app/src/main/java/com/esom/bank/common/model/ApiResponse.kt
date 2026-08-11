package com.esom.bank.common.model

import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Keep
sealed class ApiResponse<out T> : Parcelable {
    @Keep
    @Parcelize
    data class Success<T>(val data: @RawValue T, val code: Int) : ApiResponse<T>()

    @Keep
    @Parcelize
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
