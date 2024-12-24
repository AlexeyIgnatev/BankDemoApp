package com.esom.bank.screens.main.dto

import androidx.annotation.Keep

@Keep
data class UserDto(
    val id: Int = 0,
    val address: String = "",
    val balance: Double = 0.0,
    val email: String = "",
    val mnemonic: String = "",
    val name: String = "",
    val phone: String = "",
    val privateKey: String = ""
)
