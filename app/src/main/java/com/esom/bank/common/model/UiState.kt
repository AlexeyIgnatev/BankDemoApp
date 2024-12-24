package com.esom.bank.common.model

sealed class UiState<out T> {
    class Success<T>(val data: T) : UiState<T>()

    class Error<T>(val message: String) : UiState<T>()

    class Loading<T> : UiState<T>()
}