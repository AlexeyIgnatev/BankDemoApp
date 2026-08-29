package com.esom.bank.screens.transfer.dto

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.gson.annotations.SerializedName

data class RecipientLookupRequestDto(
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("address")
    val address: String? = null,
    @SerializedName("currency")
    val currency: CurrencyEnum
)

data class RecipientLookupResponseDto(
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("middle_name")
    val middleName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
    @SerializedName("full_name")
    val fullName: String? = null
)
