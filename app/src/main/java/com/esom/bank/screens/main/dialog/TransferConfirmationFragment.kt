package com.esom.bank.screens.main.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.esom.bank.R
import com.esom.bank.common.utils.toMoneyDecimalOrZero
import com.esom.bank.databinding.FragmentTransferConfirmationBinding
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class TransferConfirmationFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentTransferConfirmationBinding? = null
    private val binding: FragmentTransferConfirmationBinding
        get() = _binding ?: error("Binding accessed outside of the view lifecycle")
    private val uiModel: TransferConfirmationUiStateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferConfirmationBinding.inflate(inflater, container, false)
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
            publishResult(confirmed = true)
        }
        binding.cancelBtn.setOnClickListener {
            publishResult(confirmed = false)
        }
    }

    private fun publishResult(confirmed: Boolean) {
        requireActivity().supportFragmentManager.setFragmentResult(
            RESULT_REQUEST_KEY,
            Bundle(uiModel.uiState.value.data).apply {
                putBoolean(CONFIRMED_KEY, confirmed)
            }
        )
        findNavController().navigateUp()
    }

    private fun bindConfirmation(data: Bundle) {
        val operation = data.getString(OPERATION_KEY)
        val fromCurrency = CurrencyEnum.fromNameOrNull(data.getString(FROM_CURRENCY_KEY))
            ?: CurrencyEnum.SOM
        val toCurrency = CurrencyEnum.fromNameOrNull(data.getString(TO_CURRENCY_KEY))
            ?: fromCurrency
        val amount = data.getString(AMOUNT_KEY).orEmpty().toMoneyDecimalOrZero()
        val creditedAmount = data.getString(CREDITED_AMOUNT_KEY).orEmpty().toMoneyDecimalOrZero()
        val fee = data.getString(FEE_KEY).orEmpty().toMoneyDecimalOrZero()
        val totalDebited = data.getString(TOTAL_DEBITED_KEY).orEmpty().toMoneyDecimalOrZero()

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

    private fun formatAmount(amount: BigDecimal, currency: CurrencyEnum): String {
        val value = amount
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        const val RECIPIENT_NAME_KEY = "transfer_confirmation_recipient_name"
        const val FEE_KEY = "transfer_confirmation_fee"
        const val DISPLAY_FEE_KEY = "transfer_confirmation_display_fee"
        const val TOTAL_DEBITED_KEY = "transfer_confirmation_total_debited"
        private const val OPERATION_CONVERT = "convert"
    }
}
