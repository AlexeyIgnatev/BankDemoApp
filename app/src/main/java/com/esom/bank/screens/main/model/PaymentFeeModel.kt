package com.esom.bank.screens.main.model

import com.esom.bank.screens.main.dto.PaymentFeeDto
import java.util.Locale

data class PaymentFeeModel(
    val operation: String,
    val percentFee: Double,
    val fixedFee: Double
) {
    fun calculateFee(amount: Double): Double {
        if (amount <= 0.0) return 0.0
        return amount * (percentFee.coerceAtLeast(0.0) / 100.0) + fixedFee.coerceAtLeast(0.0)
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
