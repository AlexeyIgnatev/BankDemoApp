package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class FeeDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("esom_per_usd")
    val esomPerUsd: Double,
    @SerializedName("usd_buy_rate")
    val usdBuyRate: Double = 0.0,
    @SerializedName("usd_sell_rate")
    val usdSellRate: Double = 0.0,
    @SerializedName(
        value = "esom_som_conversion_fee_pct",
        alternate = [
            "som_esom_percent_fee",
            "som_to_esom_percent_fee",
            "esom_to_som_percent_fee",
            "convert_som_esom_percent_fee",
            "commission_som_esom_percent"
        ]
    )
    val esomSomConversionFeePct: String? = null,
    @SerializedName(
        value = "esom_som_conversion_fee_min",
        alternate = [
            "som_esom_fixed_fee",
            "som_to_esom_fixed_fee",
            "esom_to_som_fixed_fee",
            "convert_som_esom_fixed_fee",
            "commission_som_esom_fixed_fee"
        ]
    )
    val esomSomConversionFeeMin: String? = null
)
