package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName


data class UserDto(
    @SerializedName("customer_id")
    val id: Int = 0,
    @SerializedName("balance")
    val balance: BalanceDto,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("middle_name")
    val middleName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("phone")
    val phone: String,
    @SerializedName("platform_fee")
    val platformFee: Double
)
