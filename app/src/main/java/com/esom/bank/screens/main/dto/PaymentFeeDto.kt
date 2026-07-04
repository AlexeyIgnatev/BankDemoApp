package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class PaymentFeeDto(
    @SerializedName(
        value = "operation",
        alternate = [
            "operation_type",
            "operationType",
            "fee_operation",
            "feeOperation",
            "name",
            "code",
            "key"
        ]
    )
    val operation: String? = null,
    @SerializedName(
        value = "percent_fee",
        alternate = [
            "percentFee",
            "percent_fee_pct",
            "fee_percent",
            "feePercent",
            "commission_percent",
            "percent"
        ]
    )
    val percentFee: String? = null,
    @SerializedName(
        value = "fixed_fee",
        alternate = [
            "fixedFee",
            "fee_fixed",
            "fixed_amount",
            "feeAmount",
            "commission_fixed",
            "fixed"
        ]
    )
    val fixedFee: String? = null
)
