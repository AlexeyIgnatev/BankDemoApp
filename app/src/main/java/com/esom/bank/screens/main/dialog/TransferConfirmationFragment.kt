package com.esom.bank.screens.main.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.esom.bank.R
import com.esom.bank.databinding.FragmentTransferConfirmationBinding
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class TransferConfirmationFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTransferConfirmationBinding
    private val uiModel: TransferConfirmationUiStateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            DATA_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            uiModel.setData(bundle)
            bindConfirmation(bundle)
        }

        binding.confirmBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_REQUEST_KEY,
                Bundle(uiModel.uiState.value.data).apply {
                    putBoolean(CONFIRMED_KEY, true)
                }
            )
            dismiss()
        }
        binding.cancelBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_REQUEST_KEY,
                bundleOf(CONFIRMED_KEY to false)
            )
            dismiss()
        }
    }

    private fun bindConfirmation(data: Bundle) {
        val operation = data.getString(OPERATION_KEY)
        val fromCurrency = CurrencyEnum.fromNameOrNull(data.getString(FROM_CURRENCY_KEY))
            ?: CurrencyEnum.SOM
        val toCurrency = CurrencyEnum.fromNameOrNull(data.getString(TO_CURRENCY_KEY))
            ?: fromCurrency
        val amount = data.getDouble(AMOUNT_KEY)
        val creditedAmount = data.getDouble(CREDITED_AMOUNT_KEY, amount)
        val fee = data.getDouble(FEE_KEY)
        val totalDebited = data.getDouble(TOTAL_DEBITED_KEY, amount)

        binding.title.text = if (operation == OPERATION_CONVERT) {
            "Подтверждение конвертации"
        } else {
            "Подтверждение перевода"
        }
        binding.recipientValue.text = data.getString(RECIPIENT_KEY)
            .orEmpty()
            .ifBlank { getString(R.string.empty_value) }
        binding.amounts.amountValue.text = formatAmount(amount, fromCurrency)
        binding.amounts.feeValue.text = formatAmount(fee, fromCurrency)
        binding.amounts.creditedValue.text = formatAmount(creditedAmount, toCurrency)
        binding.amounts.totalDebitedValue.text = formatAmount(totalDebited, fromCurrency)
    }

    private fun formatAmount(amount: Double, currency: CurrencyEnum): String {
        val value = BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        val currencyName = when (currency) {
            CurrencyEnum.SOM -> "Сом"
            CurrencyEnum.ESOM -> "САЛАМ"
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }
        return "$value $currencyName"
    }

    companion object {
        const val DATA_REQUEST_KEY = "transfer_confirmation_data"
        const val RESULT_REQUEST_KEY = "transfer_confirmation_result"
        const val CONFIRMED_KEY = "transfer_confirmation_confirmed"
        const val TITLE_KEY = "transfer_confirmation_title"
        const val OPERATION_KEY = "transfer_confirmation_operation"
        const val AMOUNT_KEY = "transfer_confirmation_amount"
        const val CREDITED_AMOUNT_KEY = "transfer_confirmation_credited_amount"
        const val FROM_CURRENCY_KEY = "transfer_confirmation_from_currency"
        const val TO_CURRENCY_KEY = "transfer_confirmation_to_currency"
        const val PHONE_KEY = "transfer_confirmation_phone"
        const val ADDRESS_KEY = "transfer_confirmation_address"
        const val OPERATION_TITLE_KEY = "transfer_confirmation_operation_title"
        const val PAID_FROM_KEY = "transfer_confirmation_paid_from"
        const val RECIPIENT_KEY = "transfer_confirmation_recipient"
        const val FEE_KEY = "transfer_confirmation_fee"
        const val TOTAL_DEBITED_KEY = "transfer_confirmation_total_debited"
        private const val OPERATION_CONVERT = "convert"
    }
}
