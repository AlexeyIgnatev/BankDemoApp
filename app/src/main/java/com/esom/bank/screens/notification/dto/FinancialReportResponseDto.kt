package com.esom.bank.screens.notification.dto

import com.google.gson.annotations.SerializedName

data class FinancialReportResponseDto(
    @SerializedName("successful")
    val successful: Boolean?
)
