package com.esom.bank.screens.main.dto

import androidx.annotation.Keep

@Keep
data class AdminDto(
    val address: String = "",
    val mnemonic: String = "",
    val privateKey: String = ""
)
