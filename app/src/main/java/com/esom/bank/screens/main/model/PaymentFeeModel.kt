package com.esom.bank.screens.main.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.esom.bank.screens.main.dto.PaymentFeeDto
import java.math.BigDecimal
import java.util.Locale

@Keep
@Parcelize
data class PaymentFeeModel(
    val operation: String,
    val percentFee: BigDecimal,
    val fixedFee: BigDecimal
) : Parcelable {
    fun calculateFee(amount: BigDecimal): BigDecimal {
        if (amount <= BigDecimal.ZERO) return BigDecimal.ZERO
        val percentAmount = amount
            .multiply(percentFee.max(BigDecimal.ZERO))
            .divide(BigDecimal(100))
        return percentAmount.max(fixedFee.max(BigDecimal.ZERO))
    }
}

fun PaymentFeeDto.toModel() = PaymentFeeModel(
    operation = operation.orEmpty(),
    percentFee = percentFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
    fixedFee = fixedFee?.toBigDecimalOrNull() ?: BigDecimal.ZERO
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
