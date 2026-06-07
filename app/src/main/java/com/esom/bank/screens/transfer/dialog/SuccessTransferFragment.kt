package com.esom.bank.screens.transfer.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentSuccessTransferBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SuccessTransferFragment : Fragment() {
    private lateinit var binding: FragmentSuccessTransferBinding
    private val model: MainViewModel by activityViewModels()
    private var operation: SuccessOperationModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSuccessTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, compat, rect ->
            view.updatePadding(
                top = rect.top + compat.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + compat.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            compat
        }
        operation = model.lastSuccessOperation.value ?: buildFallbackOperation()
        bindOperation(operation)

        binding.backBtn.setOnClickListener { findNavController().navigate(NavGraphDirections.startMainFragment()) }
        binding.cancelBtn.setOnClickListener { findNavController().navigate(NavGraphDirections.startMainFragment()) }
        binding.shareBtn.setOnClickListener { shareOperation() }
    }

    private fun bindOperation(operation: SuccessOperationModel?) {
        val data = operation ?: return
        val amountText = formatAmount(data.amount, data.currency)
        val dateTimeText = formatDateTime(data.createdAt)

        binding.amount.text = "- $amountText"
        binding.operation.text = data.operationTitle
        binding.dateValue.text = dateTimeText
        binding.receiptValue.text = data.receiptNumber.ifBlank { getString(R.string.empty_value) }
        binding.paidFromValue.text =
            data.paidFromAccount.ifBlank { getString(R.string.empty_value) }
        binding.recipientValue.text = data.recipient.ifBlank { getString(R.string.empty_value) }
        binding.totalValue.text = amountText
    }

    private fun shareOperation() {
        val data = operation ?: return
        val shareText = getString(
            R.string.success_share_text,
            data.operationTitle,
            formatAmount(data.amount, data.currency),
            formatDateTime(data.createdAt),
            data.receiptNumber.ifBlank { getString(R.string.empty_value) },
            data.paidFromAccount.ifBlank { getString(R.string.empty_value) },
            data.recipient.ifBlank { getString(R.string.empty_value) }
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.share_success_operation)
            )
        )
    }

    private fun buildFallbackOperation(): SuccessOperationModel =
        SuccessOperationModel(
            amount = 0.0,
            currency = CurrencyEnum.SOM,
            operationTitle = getString(R.string.transfer),
            paidFromAccount = "",
            recipient = "",
            receiptNumber = ""
        )

    private fun formatAmount(amount: Double, currency: CurrencyEnum): String {
        val scaled = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        return "${scaled.toPlainString().replace('.', ',')} ${formatCurrency(currency)}"
    }

    private fun formatCurrency(currency: CurrencyEnum): String =
        when (currency) {
            CurrencyEnum.SOM -> "С"
            CurrencyEnum.ESOM -> getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> "USDT"
            CurrencyEnum.BTC -> "BTC"
            CurrencyEnum.ETH -> "ETH"
        }

    private fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("ru", "RU"))
        return formatter.format(Date(timestamp))
    }
}
