package com.esom.bank.screens.main.dto

import com.google.gson.annotations.SerializedName

data class FeeDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("esom_per_usd")
    val esomPerUsd: Int,
    @SerializedName("esom_som_conversion_fee_pct")
    val esomSomConversionFeePct: Double,
    @SerializedName("btc_trade_fee_pct")
    val btcTradeFeePct: Double,
    @SerializedName("eth_trade_fee_pct")
    val ethTradeFeePct: Double,
    @SerializedName("usdt_trade_fee_pct")
    val usdtTradeFeePct: Double,
    @SerializedName("btc_withdraw_fee_fixed")
    val btcWithdrawFeeFixed: Double,
    @SerializedName("eth_withdraw_fee_fixed")
    val ethWithdrawFeeFixed: Double,
    @SerializedName("usdt_withdraw_fee_fixed")
    val usdtWithdrawFeeFixed: Double,
    @SerializedName("min_withdraw_btc")
    val minWithdrawBtc: Double,
    @SerializedName("min_withdraw_eth")
    val minWithdrawEth: Double,
    @SerializedName("min_withdraw_usdt_trc20")
    val minWithdrawUsdtTrc20: Double
)
