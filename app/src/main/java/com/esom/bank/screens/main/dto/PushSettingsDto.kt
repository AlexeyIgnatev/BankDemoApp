package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class PushSettingsDto(
    @SerializedName("pushEnabled")
    val pushEnabled: Boolean
)
