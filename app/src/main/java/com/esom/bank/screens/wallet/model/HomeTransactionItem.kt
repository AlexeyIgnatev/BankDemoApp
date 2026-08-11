package com.esom.bank.screens.wallet.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.isUserTransfer

@Keep
@Parcelize
data class HomeTransactionItem(
    val transaction: TransactionModel,
    val conversionFrom: TransactionModel? = null,
    val conversionTo: TransactionModel? = null
) : Parcelable

fun List<TransactionModel>.toHomeTransactionItems(limit: Int = 4): List<HomeTransactionItem> {
    val consumed = mutableSetOf<TransactionModel>()
    val result = mutableListOf<HomeTransactionItem>()

    sortedByDescending { it.createdAt ?: 0L }.forEach { transaction ->
        if (transaction in consumed || result.size >= limit) return@forEach

        if (transaction.type == TransactionEnum.CONVERSION &&
            !transaction.isUserTransfer() &&
            transaction.conversionSide != null &&
            transaction.transactionId != null
        ) {
            val pair = filter {
                it.transactionId == transaction.transactionId &&
                    it.type == TransactionEnum.CONVERSION &&
                    !it.isUserTransfer() &&
                    it.conversionSide != null
            }
            val from = pair.firstOrNull { it.conversionSide == ConversionSide.OUT }
            val to = pair.firstOrNull { it.conversionSide == ConversionSide.IN }
            consumed.addAll(pair)
            result += HomeTransactionItem(
                transaction = to ?: from ?: transaction,
                conversionFrom = from,
                conversionTo = to
            )
        } else {
            consumed += transaction
            result += HomeTransactionItem(transaction)
        }
    }

    return result
}
