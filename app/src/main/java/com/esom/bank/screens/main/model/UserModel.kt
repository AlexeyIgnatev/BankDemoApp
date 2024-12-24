package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.UserDto
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class UserModel(
    val id: Int,
    val address: String,
    val balance: Double,
    val email: String,
    val mnemonic: String,
    val name: String,
    val phone: String,
    val privateKey: String
) : Parcelable

fun UserDto.toModel() = UserModel(
    id = id,
    address = address,
    balance = balance,
    email = email,
    mnemonic = mnemonic,
    name = name,
    phone = phone,
    privateKey = privateKey
)