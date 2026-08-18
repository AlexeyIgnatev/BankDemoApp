package com.esom.bank.screens.transfer.dialog.success

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatReceiptAccountTail
import com.esom.bank.common.utils.formatReceiptPersonName
import com.esom.bank.common.utils.resolveReusableRecipient
import com.esom.bank.common.utils.files.ReceiptFileUtils
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentSuccessTransferBinding
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.swap.model.SwapTemplate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val uiModel: SuccessTransferUiStateViewModel by viewModels()

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
        uiModel.setOperation(model.lastSuccessOperation.value ?: buildFallbackOperation())
        bindOperation(uiModel.uiState.value.operation)

        binding.backBtn.setOnClickListener { closeSuccessScreen() }
        binding.createTemplateBtn.setOnClickListener { showCreateTemplateDialog() }
        binding.repeatOperationBtn.setOnClickListener { repeatOperation() }
        binding.shareBtn.setOnClickListener { requestReceiptForShare() }

        model.lastSuccessOperation.observe(viewLifecycleOwner) { state ->
            uiModel.setOperation(state ?: uiModel.uiState.value.operation)
            bindOperation(uiModel.uiState.value.operation)
        }

        model.lastSuccessReceipt.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.shareBtn.isEnabled = false
                }
                is UiState.Error -> {
                    binding.shareBtn.isEnabled = true
                    if (uiModel.uiState.value.shareAfterReceiptLoaded) {
                        uiModel.consumeShareAfterLoad()
                        showReceiptError(state.message)
                    }
                }
                is UiState.Success -> {
                    binding.shareBtn.isEnabled = true
                    val creditedAmount = resolveCreditedAmount(
                        currentOperation = uiModel.uiState.value.operation,
                        receipt = state.data
                    )
                    uiModel.setOperation(uiModel.uiState.value.operation?.copy(
                        receiptNumber = state.data.receiptNumber,
                        createdAt = state.data.createdAt,
                        fee = state.data.fee,
                        operationTitle = resolveOperationTitle(state.data, uiModel.uiState.value.operation),
                        paidFromAccount = resolvePaidFromAccount(state.data),
                        recipient = resolveRecipientAccount(state.data),
                        recipientName = state.data.recipientFullName.ifBlank {
                            uiModel.uiState.value.operation?.recipientName.orEmpty()
                        },
                        creditedAmount = creditedAmount,
                        amountIsNet = false,
                        totalDebitedAmount = state.data.totalDebitedAmount
                            ?: uiModel.uiState.value.operation?.totalDebitedAmount
                            ?: if (uiModel.uiState.value.operation?.conversionSide == null && uiModel.uiState.value.operation?.targetCurrency == null) {
                                state.data.amount + state.data.fee
                            } else {
                                state.data.amount
                            }
                    ))
                    val enrichedReceipt = fillOnlyBlankReceiptFields(state.data)
                    bindOperation(uiModel.uiState.value.operation)
                    if (uiModel.uiState.value.shareAfterReceiptLoaded) {
                        uiModel.consumeShareAfterLoad()
                        shareReceiptPdf(enrichedReceipt)
                    }
                }
                null -> Unit
            }
        }

        if (uiModel.uiState.value.operation?.loadReceiptAutomatically == true &&
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
        } else if (data.conversionSide == null && data.targetCurrency == null) {
            data.amount
        } else {
            (data.amount - data.fee).coerceAtLeast(0.0)
        }
        val totalText = formatAmount(totalAmount, resolveCreditedCurrency(data))
        val dateTimeText = formatDateTime(data.createdAt)

        binding.amount.text = "${if (data.amountIsIncoming) "+" else "-"} $amountText"
        binding.operation.text = data.operationTitle
        binding.dateValue.text = dateTimeText
        binding.receiptValue.text = data.receiptNumber.ifBlank { getString(R.string.empty_value) }
        val senderAccount = formatReceiptAccountTail(resolveSenderAccountForDisplay(data))
        val senderName = formatReceiptPersonName(data.senderName)
        binding.paidFromValue.text = when {
            senderName.isNotBlank() && senderAccount.isNotBlank() -> "$senderName\n$senderAccount"
            senderName.isNotBlank() -> senderName
            else -> senderAccount.ifBlank { getString(R.string.empty_value) }
        }
        binding.recipientValue.text = formatRecipientForDisplay(data)
        binding.feeValue.text = formatAmount(data.fee, data.currency)
        binding.totalValue.text = totalText
        binding.totalWithdrawnValue.text = formatAmount(
            data.totalDebitedAmount ?: data.amount,
            data.currency
        )
        binding.createTemplateBtn.visibility = if (data.openedFromHistory) View.GONE else View.VISIBLE
    }

    private fun closeSuccessScreen() {
        if (uiModel.uiState.value.operation?.openedFromHistory == true) {
            findNavController().popBackStack()
        } else {
            findNavController().navigate(NavGraphDirections.startMainFragment())
        }
    }

    private fun showCreateTemplateDialog() {
        val operation = uiModel.uiState.value.operation ?: return
        if (operation.openedFromHistory) return
        val input = EditText(requireContext()).apply { hint = "Например, Детский сад" }
        val inputContainer = FrameLayout(requireContext()).apply {
            val horizontalPadding = dp(24)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Создать шаблон")
            .setMessage(templateDescription(operation))
            .setView(inputContainer, 0, 0, 0, 0)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Создать шаблон", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    input.error = "Введите название"
                    return@setOnClickListener
                }
                saveTemplate(operation, name)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun saveTemplate(operation: SuccessOperationModel, name: String) {
        val target = operation.targetCurrency
        if (target != null) {
            model.addSwapTemplate(SwapTemplate(
                amount = operation.amount,
                fromCurrency = operation.currency.name,
                toCurrency = target.name,
                name = name
            ))
        } else {
            model.addTransferTemplate(TransferTemplate(
                amount = operation.amount,
                currency = operation.currency.name,
                recipient = operation.recipient,
                isPhone = operation.recipient.count(Char::isDigit) >= 7 &&
                    operation.recipient.all { it.isDigit() || it in "+ ()-" },
                name = name
            ))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun repeatOperation() {
        val operation = uiModel.uiState.value.operation ?: return
        val target = operation.targetCurrency
        if (target != null) {
            findNavController().navigate(
                NavGraphDirections.startSwapFragment(
                    operation.currency.name,
                    target.name,
                    operation.amount.toFloat(),
                    true
                )
            )
        } else {
            findNavController().navigate(
                NavGraphDirections.startTransferFragment(
                    operation.currency.name,
                    operation.recipient,
                    operation.recipientName,
                    operation.amount.toFloat(),
                    true
                )
            )
        }
    }

    private fun templateDescription(operation: SuccessOperationModel): String =
        if (operation.targetCurrency != null) {
            "Конвертация ${formatAmount(operation.amount, operation.currency)} в ${formatCurrency(operation.targetCurrency)}"
        } else {
            "Перевод ${formatAmount(operation.amount, operation.currency)} получателю ${operation.recipientName.ifBlank { operation.recipient }}"
        }

    private fun formatRecipientForDisplay(operation: SuccessOperationModel): String {
        val account = formatReceiptAccountTail(operation.recipient)
        val recipientName = formatReceiptPersonName(operation.recipientName)
        return when {
            recipientName.isNotBlank() && account.isNotBlank() ->
                "$recipientName\n$account"
            recipientName.isNotBlank() -> recipientName
            account.isNotBlank() -> account
            else -> getString(R.string.empty_value)
        }
    }

    private fun resolveSenderAccountForDisplay(operation: SuccessOperationModel): String {
        val operationAccount = operation.paidFromAccount
        if (operationAccount.count(Char::isLetterOrDigit) >= RECEIPT_VISIBLE_ACCOUNT_LENGTH) {
            return operationAccount
        }

        val user = (model.myData.value as? UiState.Success)?.data ?: return operationAccount
        if (operation.currency == CurrencyEnum.SOM && user.phone.isNotBlank()) {
            return user.phone
        }
        return user.wallets
            .firstOrNull { it.currency == operation.currency }
            ?.address
            ?.takeIf(String::isNotBlank)
            ?: user.phone.ifBlank { operationAccount }
    }

    private fun requestReceiptForShare() {
        val receipt = (model.lastSuccessReceipt.value as? UiState.Success)?.data
        if (receipt != null) {
            shareReceiptPdf(fillOnlyBlankReceiptFields(receipt))
        } else {
            if (uiModel.uiState.value.operation?.transactionId != null) {
                uiModel.requestShareAfterLoad()
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
        val currentOperation = uiModel.uiState.value.operation ?: return receipt
        return receipt.copy(
            amount = currentOperation.amount,
            paidFromAccount = firstNotBlank(
                receipt.paidFromAccount,
                receipt.absFromAccount,
                currentOperation.paidFromAccount
            ),
            accountDetails = firstNotBlank(
                receipt.accountDetails,
                receipt.absToAccount,
                currentOperation.recipient
            ),
            recipientFullName = receipt.recipientFullName.ifBlank {
                currentOperation.recipientName
            },
            senderFullName = receipt.senderFullName.ifBlank {
                currentOperation.senderName
            },
            receiptNumber = receipt.receiptNumber.ifBlank {
                currentOperation.receiptNumber
            },
            creditedAmount = currentOperation.creditedAmount,
            targetCurrency = currentOperation.targetCurrency?.name.orEmpty(),
            totalDebitedAmount = currentOperation.totalDebitedAmount
        )
    }

    private fun resolvePaidFromAccount(receipt: ReceiptModel): String {
        val currentOperation = uiModel.uiState.value.operation
        return firstNotBlank(
            receipt.paidFromAccount,
            receipt.absFromAccount,
            receipt.absAccount,
            currentOperation?.paidFromAccount.orEmpty()
        )
    }

    private fun resolveRecipientAccount(receipt: ReceiptModel): String {
        val currentOperation = uiModel.uiState.value.operation
        return resolveReusableRecipient(
            currentOperation?.recipient.orEmpty(),
            receipt.accountDetails,
            receipt.absToAccount,
            receipt.absAccount
        )
    }

    private fun resolveOperationTitle(
        receipt: ReceiptModel,
        currentOperation: SuccessOperationModel?
    ): String {
        val type = receipt.type.lowercase(Locale.ROOT)
        return when {
            receipt.conversionSide != null || type.contains("convert") ->
                "Конвертация собственных средств"
            type.contains("income") || type.contains("inflow") ->
                "Пополнение"
            type.contains("transfer") || type.contains("expense") ->
                getString(R.string.transfer)
            else -> currentOperation?.operationTitle.orEmpty()
        }
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

    private fun resolveCreditedAmount(
        currentOperation: SuccessOperationModel?,
        receipt: ReceiptModel
    ): Double? {
        val operation = currentOperation ?: return receipt.creditedAmount
        val isSomEsomConversion = (
            operation.currency == CurrencyEnum.SOM && operation.targetCurrency == CurrencyEnum.ESOM
                ) || (
            operation.currency == CurrencyEnum.ESOM && operation.targetCurrency == CurrencyEnum.SOM
                )

        return if (isSomEsomConversion) {
            (operation.amount - receipt.fee).coerceAtLeast(0.0)
        } else {
            receipt.creditedAmount ?: operation.creditedAmount
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
            CurrencyEnum.ESOM -> "САЛАМ"
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }

    private fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", Locale("ru", "RU"))
        return formatter.format(Date(timestamp))
    }

    private companion object {
        const val RECEIPT_VISIBLE_ACCOUNT_LENGTH = 8
    }
}
