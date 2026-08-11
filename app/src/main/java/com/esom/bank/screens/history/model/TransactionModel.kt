package com.esom.bank.screens.history.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.history.dto.TransactionDto
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.enums.CurrencyEnum
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class TransactionModel(
    val transactionId: Long?,
    val currencyEnum: CurrencyEnum?,
    val type: TransactionEnum?,
    val conversionSide: ConversionSide?,
    val amount: Double?,
    val successful: Boolean?,
    val createdAt: Long?,
    val recipientFullName: String? = null,
    val senderFullName: String? = null,
    val accountDetails: String? = null
): Parcelable

fun TransactionDto?.toModel(): TransactionModel? =
    this?.let {
        TransactionModel(
            transactionId = it.transactionId ?: it.id,
            currencyEnum = it.currencyEnum,
            type = it.type,
            conversionSide = it.conversionSide,
            amount = it.amount,
            successful = it.successful,
            createdAt = it.createdAt,
            recipientFullName = it.recipientFullName,
            senderFullName = it.senderFullName,
            accountDetails = it.accountDetails
        )
    }

fun List<TransactionDto?>.toModel(): List<TransactionModel?> {
    return this.map { it.toModel() }
}

fun TransactionModel.isUserTransfer(): Boolean =
    type == TransactionEnum.TRANSFER ||
        (conversionSide == null &&
            (!recipientFullName.isNullOrBlank() ||
                !senderFullName.isNullOrBlank() ||
                !accountDetails.isNullOrBlank()))
