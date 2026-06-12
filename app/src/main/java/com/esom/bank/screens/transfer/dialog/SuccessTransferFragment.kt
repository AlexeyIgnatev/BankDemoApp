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
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.files.ReceiptFileUtils
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentSuccessTransferBinding
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
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
    private var shareAfterReceiptLoaded = false

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
        binding.shareBtn.setOnClickListener { requestReceiptForShare() }

        model.lastSuccessOperation.observe(viewLifecycleOwner) { state ->
            operation = state ?: operation
            bindOperation(operation)
        }

        model.lastSuccessReceipt.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.shareBtn.isEnabled = false
                }
                is UiState.Error -> {
                    binding.shareBtn.isEnabled = true
                    if (shareAfterReceiptLoaded) {
                        shareAfterReceiptLoaded = false
                        showReceiptError(state.message)
                    }
                }
                is UiState.Success -> {
                    binding.shareBtn.isEnabled = true
                    val enrichedReceipt = fillOnlyBlankReceiptFields(state.data)
                    operation = operation?.copy(
                        receiptNumber = enrichedReceipt.receiptNumber,
                        createdAt = enrichedReceipt.createdAt,
                        fee = enrichedReceipt.fee,
                        paidFromAccount = resolvePaidFromAccount(enrichedReceipt),
                        recipient = resolveRecipientAccount(enrichedReceipt),
                        amountIsNet = false
                    )
                    bindOperation(operation)
                    if (shareAfterReceiptLoaded) {
                        shareAfterReceiptLoaded = false
                        shareReceiptPdf(enrichedReceipt)
                    }
                }
            }
        }

        if (operation?.loadReceiptAutomatically == true &&
            model.lastSuccessReceipt.value !is UiState.Success
        ) {
            model.prepareReceiptForLastSuccessOperation()
        }
    }

    private fun bindOperation(operation: SuccessOperationModel?) {
        val data = operation ?: return
        val amountText = formatAmount(data.amount, data.currency)
        val totalAmount = data.creditedAmount ?: if (data.amountIsNet) {
            data.amount
        } else {
            (data.amount - data.fee).coerceAtLeast(0.0)
        }
        val totalText = formatAmount(totalAmount, resolveCreditedCurrency(data))
        val dateTimeText = formatDateTime(data.createdAt)

        binding.amount.text = "- $amountText"
        binding.operation.text = data.operationTitle
        binding.dateValue.text = dateTimeText
        binding.receiptValue.text = data.receiptNumber.ifBlank { getString(R.string.empty_value) }
        binding.paidFromValue.text =
            formatAccountForDisplay(data.paidFromAccount).ifBlank { getString(R.string.empty_value) }
        binding.recipientValue.text =
            formatAccountForDisplay(data.recipient).ifBlank { getString(R.string.empty_value) }
        binding.feeValue.text = formatAmount(data.fee, data.currency)
        binding.totalValue.text = totalText
    }

    private fun requestReceiptForShare() {
        val receipt = (model.lastSuccessReceipt.value as? UiState.Success)?.data
        if (receipt != null) {
            shareReceiptPdf(fillOnlyBlankReceiptFields(receipt))
        } else {
            if (operation?.transactionId != null) {
                shareAfterReceiptLoaded = true
                model.prepareReceiptForLastSuccessOperation()
            } else {
                showReceiptError(MainViewModel.RECEIPT_OPERATION_NOT_FOUND)
            }
        }
    }

    private fun shareReceiptPdf(receipt: ReceiptModel) {
        val receiptUri = runCatching {
            ReceiptFileUtils.createReceiptPdfForShare(requireContext(), receipt)
        }.getOrElse { error ->
            binding.root.showErrorSnackbar(error.message ?: getString(R.string.something_went_wrong))
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = ReceiptFileUtils.PDF_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, receiptUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.share_success_operation)
            )
        )
    }

    private fun fillOnlyBlankReceiptFields(receipt: ReceiptModel): ReceiptModel {
        val currentOperation = operation ?: return receipt
        return receipt.copy(
            amount = currentOperation.amount,
            paidFromAccount = bestAccountCandidate(
                receipt.paidFromAccount,
                receipt.absFromAccount,
                currentOperation.paidFromAccount
            ),
            accountDetails = bestAccountCandidate(
                receipt.accountDetails,
                receipt.absToAccount,
                currentOperation.recipient
            ),
            receiptNumber = receipt.receiptNumber.ifBlank {
                currentOperation.receiptNumber
            },
            creditedAmount = currentOperation.creditedAmount,
            targetCurrency = currentOperation.targetCurrency?.name.orEmpty()
        )
    }

    private fun resolvePaidFromAccount(receipt: ReceiptModel): String {
        val currentOperation = operation
        return bestAccountCandidate(
            receipt.absFromAccount,
            receipt.paidFromAccount,
            receipt.absAccount,
            currentOperation?.paidFromAccount.orEmpty()
        )
    }

    private fun resolveRecipientAccount(receipt: ReceiptModel): String {
        val currentOperation = operation
        return bestAccountCandidate(
            receipt.accountDetails,
            receipt.absToAccount,
            receipt.absAccount,
            currentOperation?.recipient.orEmpty()
        )
    }

    private fun showReceiptError(message: String) {
        val errorMessage = if (message == MainViewModel.RECEIPT_OPERATION_NOT_FOUND) {
            getString(R.string.receipt_operation_not_found)
        } else {
            message
        }
        binding.root.showErrorSnackbar(errorMessage)
    }

    private fun firstNotBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun resolveCreditedCurrency(operation: SuccessOperationModel): CurrencyEnum {
        return operation.targetCurrency ?: when (operation.conversionSide) {
            ConversionSide.IN -> CurrencyEnum.SOM
            ConversionSide.OUT -> CurrencyEnum.ESOM
            null -> operation.currency
        }
    }

    private fun bestAccountCandidate(vararg values: String): String =
        values
            .map { it.sanitizeAccountCandidate() }
            .filter { it.isNotBlank() }
            .maxByOrNull { accountCandidateScore(it) }
            .orEmpty()

    private fun accountCandidateScore(value: String): Int {
        val compact = value.replace(" ", "").replace("-", "")
        val visibleChars = compact.count { it != '*' }
        val starPenalty = compact.count { it == '*' } * 20
        return visibleChars * 10 + compact.length - starPenalty
    }

    private fun String.sanitizeAccountCandidate(): String =
        replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    private fun formatAccountForDisplay(value: String): String {
        val compact = value
            .sanitizeAccountCandidate()
            .replace(" ", "")
            .replace("-", "")
        if (compact.isBlank()) return ""
        if (compact.contains('*') || compact.length <= 8) return compact

        val visibleTail = compact.takeLast(8)
        return "${"*".repeat(compact.length - visibleTail.length)}$visibleTail"
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
