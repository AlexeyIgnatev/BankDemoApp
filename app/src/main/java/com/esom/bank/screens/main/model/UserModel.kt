package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.main.dto.FeeDto
import com.esom.bank.screens.main.dto.UserDto
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Keep
@Parcelize
data class UserModel(
    val id: Int = 0,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val email: String,
    val phone: String,
    val wallets: List<WalletModel>
) : Parcelable

fun UserDto.toModel() = UserModel(
    id = id,
    firstName = firstName,
    middleName = middleName,
    lastName = lastName,
    email = email,
    phone = phone,
    wallets = wallets.toModel()
)

@Keep
@Parcelize
data class FeeModel(
    val id: Int,
    val usdBuyRate: BigDecimal,
    val usdSellRate: BigDecimal = BigDecimal.ZERO,
    val usdtWithdrawFeeFixed: BigDecimal = BigDecimal.ZERO,
    val minWithdrawUsdtTrc20: BigDecimal = BigDecimal.ZERO
): Parcelable

fun FeeDto.toModel() = FeeModel(
    id = id,
    usdBuyRate = usdBuyRate,
    usdSellRate = usdSellRate,
    usdtWithdrawFeeFixed = usdtWithdrawFeeFixed,
    minWithdrawUsdtTrc20 = minWithdrawUsdtTrc20
)
