package com.esom.bank.retrofit.dto

import com.google.gson.annotations.SerializedName

data class UserAuthRequestDto(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
)

data class UserAuthResponseDto(
    @SerializedName("accessToken")
    val accessToken: String,
)
