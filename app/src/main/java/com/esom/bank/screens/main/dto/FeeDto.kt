package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class FeeDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("usd_buy_rate")
    val usdBuyRate: BigDecimal,
    @SerializedName("usd_sell_rate")
    val usdSellRate: BigDecimal = BigDecimal.ZERO,
    @SerializedName("usdt_withdraw_fee_fixed")
    val usdtWithdrawFeeFixed: BigDecimal = BigDecimal.ZERO,
    @SerializedName("min_withdraw_usdt_trc20")
    val minWithdrawUsdtTrc20: BigDecimal = BigDecimal.ZERO
)
