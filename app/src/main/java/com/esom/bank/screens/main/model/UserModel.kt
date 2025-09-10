package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.UserDto
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class UserModel(
    val id: Int = 0,
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val privateKey: String? = null,
    val wallets: List<WalletModel>,
    val platformFee: Double
) : Parcelable

fun UserDto.toModel() = UserModel(
    id = id,
    firstName = firstName,
    middleName = middleName,
    lastName = lastName,
    email = email,
    phone = phone,
    wallets = wallets.toModel(),
    privateKey = privateKey,
    platformFee = platformFee
)