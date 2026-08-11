package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.dto.PaymentFeeDto
import java.util.Locale

@Keep
@Parcelize
data class PaymentFeeModel(
    val operation: String,
    val percentFee: Double,
    val fixedFee: Double
) : Parcelable {
    fun calculateFee(amount: Double): Double {
        if (amount <= 0.0) return 0.0
        val percentAmount = amount * (percentFee.coerceAtLeast(0.0) / 100.0)
        return maxOf(percentAmount, fixedFee.coerceAtLeast(0.0))
    }
}

fun PaymentFeeDto.toModel() = PaymentFeeModel(
    operation = operation.orEmpty(),
    percentFee = percentFee?.toDoubleOrNull() ?: 0.0,
    fixedFee = fixedFee?.toDoubleOrNull() ?: 0.0
)

fun List<PaymentFeeDto>.toPaymentFeeModels(): List<PaymentFeeModel> =
    map { it.toModel() }

fun List<PaymentFeeModel>.findByOperation(operation: String?): PaymentFeeModel? =
    operation?.takeIf { it.isNotBlank() }?.let { op ->
        val normalizedOperation = op.normalizeFeeOperationKey()
        firstOrNull { fee ->
            fee.operation.normalizeFeeOperationKey() == normalizedOperation
        }
    }

private fun String.normalizeFeeOperationKey(): String {
    return trim()
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
}
