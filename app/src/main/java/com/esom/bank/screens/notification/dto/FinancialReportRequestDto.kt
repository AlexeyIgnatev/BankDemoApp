package com.esom.bank.screens.notification.dto

import com.google.gson.annotations.SerializedName

data class FinancialReportRequestDto(
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("from_time")
    val fromTime: Long? = null,
    @SerializedName("to_time")
    val toTime: Long? = null
)
