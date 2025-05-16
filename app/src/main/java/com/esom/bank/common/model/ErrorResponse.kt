package com.esom.bank.common.model

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("statusCode")
    val statusCode: Int? = null
)