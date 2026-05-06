package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class FcmTokenDto(
    @SerializedName("token")
    val token: String,
    @SerializedName("platform")
    val platform: String = "android"
)
