package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class FeeDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("esom_per_usd")
    val esomPerUsd: Double,
    @SerializedName(
        value = "som_esom_percent_fee",
        alternate = [
            "som_to_esom_percent_fee",
            "esom_to_som_percent_fee",
            "convert_som_esom_percent_fee",
            "commission_som_esom_percent"
        ]
    )
    val somEsomPercentFee: String? = null,
    @SerializedName(
        value = "som_esom_fixed_fee",
        alternate = [
            "som_to_esom_fixed_fee",
            "esom_to_som_fixed_fee",
            "convert_som_esom_fixed_fee",
            "commission_som_esom_fixed_fee"
        ]
    )
    val somEsomFixedFee: String? = null
)
