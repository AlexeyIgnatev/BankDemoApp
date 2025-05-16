package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class TransferDto(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("phone_number")
    val phoneNumber: String
)
