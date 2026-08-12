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
    @SerializedName(value = "first_name", alternate = ["firstName"])
    val firstName: String? = null,
    @SerializedName(value = "middle_name", alternate = ["middleName", "patronymic"])
    val middleName: String? = null,
    @SerializedName(value = "last_name", alternate = ["lastName", "surname"])
    val lastName: String? = null,
    @SerializedName(value = "full_name", alternate = ["fullName", "recipient_full_name"])
    val fullName: String? = null
)
