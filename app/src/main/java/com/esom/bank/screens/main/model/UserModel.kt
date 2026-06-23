package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.UserDto
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class UserModel(
    val id: Int = 0,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val email: String,
    val phone: String,
    val privateKey: String? = null,
    val wallets: List<WalletModel>
) : Parcelable

fun UserDto.toModel() = UserModel(
    id = id,
    firstName = firstName,
    middleName = middleName,
    lastName = lastName,
    email = email,
    phone = phone,
    wallets = wallets.toModel(),
    privateKey = privateKey
)

@Keep
@Parcelize
data class FeeModel(
    val id: Int,
    val esomPerUsd: Double,
    val somEsomPercentFee: Double? = null,
    val somEsomFixedFee: Double? = null
): Parcelable

fun FeeDto.toModel() = FeeModel(
    id = id,
    esomPerUsd = esomPerUsd,
    somEsomPercentFee = somEsomPercentFee?.toDoubleOrNull(),
    somEsomFixedFee = somEsomFixedFee?.toDoubleOrNull()
)
