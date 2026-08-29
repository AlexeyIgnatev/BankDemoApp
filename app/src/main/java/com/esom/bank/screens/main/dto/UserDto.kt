package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("customer_id")
    val id: Int = 0,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("middle_name")
    val middleName: String?,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("wallets")
    val wallets: List<WalletDto>
)
