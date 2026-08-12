package com.esom.bank.screens.transfer

import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.applyKyrgyzPhoneMask
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.isCompleteKyrgyzPhone
import com.esom.bank.common.utils.views.kyrgyzPhoneDigits
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.slideInFromBottom
import com.esom.bank.common.utils.views.slideOut
import com.esom.bank.databinding.FragmentTransferBinding
import com.esom.bank.screens.main.dialog.TransferConfirmationFragment
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import java.math.BigDecimal
import java.math.RoundingMode

internal class TransferScreenController(
    private val fragment: Fragment,
    private val binding: FragmentTransferBinding,
    private val model: MainViewModel,
    private val uiModel: TransferUiStateViewModel,
    private val args: TransferFragmentArgs
) {

    fun bind() {
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if (imeBottom == 0) {
                    systemBarsBottom
                } else {
                    imeBottom
                }
            )
            insets
        }
        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().popBackStack()
        }

        uiModel.initialize(
            currency = CurrencyEnum.fromNameOrNull(args.currency) ?: CurrencyEnum.SOM,
            contact = args.contact
        )
        updateCurrencyIcon(uiModel.uiState.value.fromCurrency)
        updateCurrencyOptionsPanel()
        setContactHint()
        setupChangeButton()
        if (uiModel.uiState.value.toPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        if (args.contact.isNotBlank()) {
            binding.contact.setText(args.contact)
            binding.contact.setSelection(binding.contact.text?.length ?: 0)
        }
        if (args.amount > 0f) {
            binding.sumInput.setText(formatTemplateAmount(args.amount.toDouble()))
        }
        model.getFees()
        initInitialBalances()
        setupQuickAmounts()
        binding.sumInput.setOnUserTextChangeListener { text ->
            updateCommissionAndTotal(text.toString())
        }

        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.currentCurrencyLayout.setOnClickListener { toggleCurrencyPanel() }
        binding.qrScanBtn.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startQrFragment())
        }

        binding.firstUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.currencyPanelOptions) {
                selectCurrency(CurrencyEnum.USDT_TRC20)
            }
        }
        binding.firstDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in uiModel.uiState.value.currencyPanelOptions) {
                selectCurrency(CurrencyEnum.ESOM)
            }
        }
        binding.firstSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in uiModel.uiState.value.currencyPanelOptions) {
                selectCurrency(CurrencyEnum.SOM)
            }
        }

        binding.sendBtn.setOnClickListener { handleTransferButtonClick() }
        setupTransferConfirmationResultListener()

        model.myData.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateWalletBalances()
                initInitialBalances()
                updateCommissionAndTotal(binding.sumInput.text.toString())
            }
        }

        model.fees.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateCommissionAndTotal(binding.sumInput.text.toString())
                executeAutomaticRepeatIfReady()
            }
        }

        model.transferRes.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    binding.sendText.isVisible = false
                    binding.indicator.isVisible = true
                }

                is UiState.Error -> {
                    uiModel.setPendingTemplate(null)
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(
                        NavGraphDirections.startFailTransferFragment(it.message)
                    )
                }

                is UiState.Success -> {
                    uiModel.setPendingTemplate(null)
                    model.updateUserData()
                    model.updateLastSuccessOperationReceipt(
                        transactionId = it.data.transactionId,
                        receiptNumber = it.data.receiptNumber
                    )
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                }

                else -> {}
            }
        }
        executeAutomaticRepeatIfReady()
    }

    private fun executeAutomaticRepeatIfReady() {
        if (!args.autoExecute || args.amount <= 0f || args.contact.isBlank()) return
        if (model.fees.value !is UiState.Success) return
        if (!uiModel.startAutomaticRepeat()) return

        val amount = args.amount.toDouble()
        val contact = args.contact.trim()
        val phone = if (uiModel.uiState.value.toPhoneNumber) contact.kyrgyzPhoneDigits() else ""
        val address = contact.takeIf { !uiModel.uiState.value.toPhoneNumber }
        val currency = uiModel.uiState.value.fromCurrency
        val fee = calculateTransferCommission(amount, currency)

        model.setLastSuccessOperation(
            SuccessOperationModel(
                amount = amount,
                currency = currency,
                operationTitle = getString(R.string.transfer),
                paidFromAccount = getCurrentUserAccountForSuccess(),
                recipient = contact,
                recipientName = args.recipientName,
                receiptNumber = "",
                fee = fee,
                creditedAmount = amount,
                totalDebitedAmount = amount + fee,
                senderName = currentUserFullName()
            )
        )
        model.transferToUser(amount, phone, address, currency)
    }

    private fun requireContext() = fragment.requireContext()
    private fun requireActivity() = fragment.requireActivity()
    private fun findNavController() = fragment.findNavController()
    private fun getString(id: Int, vararg args: Any): String = fragment.getString(id, *args)
    private val resources get() = fragment.resources
    private val viewLifecycleOwner get() = fragment.viewLifecycleOwner
    private val parentFragmentManager get() = fragment.parentFragmentManager

    private fun updateCurrencyOptionsPanel() {
        updateWalletBalances()
    }

    private fun setupTransferConfirmationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            TransferConfirmationFragment.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (!bundle.getBoolean(TransferConfirmationFragment.CONFIRMED_KEY)) return@setFragmentResultListener
            if (bundle.getString(TransferConfirmationFragment.OPERATION_KEY) != OPERATION_TRANSFER) {
                return@setFragmentResultListener
            }

            val amount = bundle.getDouble(TransferConfirmationFragment.AMOUNT_KEY)
            val currencyName = bundle.getString(TransferConfirmationFragment.FROM_CURRENCY_KEY).orEmpty()
            val currency = CurrencyEnum.fromNameOrNull(currencyName)
                ?: return@setFragmentResultListener
            val phone = bundle.getString(TransferConfirmationFragment.PHONE_KEY).orEmpty()
            val address = bundle.getString(TransferConfirmationFragment.ADDRESS_KEY)
            val recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty()
            val fee = bundle.getDouble(TransferConfirmationFragment.FEE_KEY)
            val totalDebited = bundle.getDouble(TransferConfirmationFragment.TOTAL_DEBITED_KEY)

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = currency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.transfer),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = recipient,
                    receiptNumber = "",
                    fee = fee,
                    creditedAmount = amount,
                    totalDebitedAmount = totalDebited,
                    senderName = currentUserFullName()
                )
            )
            model.transferToUser(amount, phone, address, currency)
        }
    }

    private fun renderTemplates() {
        val templates = model.getTransferTemplates()
        binding.templatesSection.isVisible = templates.isNotEmpty()
        binding.templatesContainer.removeAllViews()

        templates.forEach { template ->
            val currency = CurrencyEnum.fromNameOrNull(template.currency) ?: return@forEach
            val label = buildString {
                append(template.recipient)
                append('\n')
                append(formatTemplateAmount(template.amount))
                append(' ')
                append(getCurrencyName(currency))
            }
            binding.templatesContainer.addView(
                createTemplateView(label) { applyTemplate(template) }
            )
        }
    }

    private fun createTemplateView(label: String, onClick: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            minWidth = dp(132)
            maxWidth = dp(190)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setBackgroundResource(R.drawable.recent_template_background)
            text = label
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1D1D1B"))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setOnClickListener { onClick() }
        }
    }

    private fun applyTemplate(template: TransferTemplate) {
        val currency = CurrencyEnum.fromNameOrNull(template.currency) ?: return
        uiModel.updateCurrency(currency, template.isPhone)

        updateCurrencyIcon(currency)
        updateWalletBalances()
        setupChangeButton()
        setContactHint()
        if (uiModel.uiState.value.toPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }

        binding.contact.setText(template.recipient)
        binding.contact.setSelection(binding.contact.text?.length ?: 0)
        binding.sumInput.setText(formatTemplateAmount(template.amount))
        binding.sumInput.setSelection(binding.sumInput.text?.length ?: 0)
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun formatTemplateAmount(amount: Double): String =
        amount.formatBalanceNew()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun setupChangeButton() {
        val canSwitchRecipientMode = uiModel.uiState.value.fromCurrency in listOf(
            CurrencyEnum.USDT_TRC20,
            CurrencyEnum.ESOM
        )
        binding.changeLayout.isVisible = canSwitchRecipientMode
        binding.changeLayout.setOnClickListener {
            if (uiModel.uiState.value.fromCurrency in listOf(CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM)) {
                uiModel.toggleRecipientMode()
                updateContactType()
            }
        }
    }

    private fun updateContactType() {
        setContactHint()
        binding.contact.setText("")
        if (uiModel.uiState.value.toPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun updateWalletBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone

        val currentWallet = wallets.find { it.currency == uiModel.uiState.value.fromCurrency }
        binding.sum.text = currentWallet?.balance?.formatBalanceNew() ?: "0"
        binding.currencyTitle.text = getCurrencyName(uiModel.uiState.value.fromCurrency)
        binding.currency.text = when (uiModel.uiState.value.fromCurrency) {
            CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
            else -> currentWallet?.address?.takeLast(3)?.let { "*$it" } ?: ""
        }

        binding.peopleTitle.text = args.recipientName.ifBlank { getString(R.string.recipient) }
        binding.peopleIcon.setImageResource(
            when (uiModel.uiState.value.fromCurrency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )

        binding.firstUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.currencyPanelOptions
        binding.firstDigitalBtn.isVisible = CurrencyEnum.ESOM in uiModel.uiState.value.currencyPanelOptions
        binding.firstSomBtn.isVisible = CurrencyEnum.SOM in uiModel.uiState.value.currencyPanelOptions

        val usdtWallet = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val esomWallet = wallets.find { it.currency == CurrencyEnum.ESOM }
        val somWallet = wallets.find { it.currency == CurrencyEnum.SOM }

        bindCurrencyRow(
            icon = binding.firstUsdtIcon,
            title = binding.firstUsdtTitle,
            suffix = binding.firstUsdt,
            balance = binding.firstSum1,
            divider = binding.firstUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in uiModel.uiState.value.currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindCurrencyRow(
            icon = binding.firstDigitalIcon,
            title = binding.firstDigitalTitle,
            suffix = binding.firstDigital,
            balance = binding.firstSum2,
            divider = binding.firstDigitalView,
            currency = CurrencyEnum.ESOM.takeIf { it in uiModel.uiState.value.currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindCurrencyRow(
            icon = binding.firstSomIcon,
            title = binding.firstSomTitle,
            suffix = binding.firstSom,
            balance = binding.firstSum3,
            divider = binding.firstSomView,
            currency = CurrencyEnum.SOM.takeIf { it in uiModel.uiState.value.currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
    }

    private fun bindCurrencyRow(
        icon: android.widget.ImageView,
        title: android.widget.TextView,
        suffix: android.widget.TextView,
        balance: android.widget.TextView,
        divider: android.view.View,
        currency: CurrencyEnum?,
        walletUSDT: WalletModel?,
        walletESOM: WalletModel?,
        walletSOM: WalletModel?,
        phone: String?
    ) {
        if (currency == null) {
            icon.isVisible = false
            title.isVisible = false
            suffix.isVisible = false
            balance.isVisible = false
            divider.isVisible = false
            return
        }

        val wallet = when (currency) {
            CurrencyEnum.USDT_TRC20 -> walletUSDT
            CurrencyEnum.ESOM -> walletESOM
            CurrencyEnum.SOM -> walletSOM
        }

        icon.setImageResource(
            when (currency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )
        icon.isVisible = true
        title.text = getCurrencyName(currency)
        title.isVisible = true
        suffix.text = when (currency) {
            CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
            else -> wallet?.address?.takeLast(3)?.let { "*$it" } ?: ""
        }
        suffix.isVisible = true
        balance.text = wallet?.balance?.formatBalanceNew() ?: "0"
        balance.isVisible = true
        divider.isVisible = true
    }

    private fun setContactHint() {
        binding.contact.hint = if (uiModel.uiState.value.toPhoneNumber) {
            "Введите номер телефона"
        } else {
            "Введите адрес кошелька"
        }
    }

    private fun setupQuickAmounts() {
        listOf(
            binding.sum50Layout to BigDecimal("50"),
            binding.sum100Layout to BigDecimal("100"),
            binding.sum1000Layout to BigDecimal("1000"),
            binding.sum10000Layout to BigDecimal("10000")
        ).forEach { (layout, value) ->
            layout.setOnClickListener {
                val currentAmount = binding.sumInput.text
                    ?.toString()
                    ?.trim()
                    ?.replace(',', '.')
                    ?.toBigDecimalOrNull()
                    ?: BigDecimal.ZERO
                val updatedAmount = currentAmount.add(value).stripTrailingZeros().toPlainString()
                binding.sumInput.setText(updatedAmount)
                binding.sumInput.setSelection(updatedAmount.length)
                updateCommissionAndTotal(updatedAmount)
            }
        }
    }

    private fun initInitialBalances() {
        updateWalletBalances()
    }

    private fun toggleCurrencyPanel() {
        if (uiModel.uiState.value.currencyPanelShown) {
            slideOut(binding.typeCurrencyLayout)
        } else {
            binding.typeCurrencyLayout.visibility = View.VISIBLE
            slideIn(binding.typeCurrencyLayout)
        }
        uiModel.toggleCurrencyPanel()
    }

    private fun selectCurrency(currency: CurrencyEnum) {
        toggleCurrency(currency)
        if (uiModel.uiState.value.currencyPanelShown) toggleCurrencyPanel()
    }

    private fun toggleCurrency(currency: CurrencyEnum) {
        val wasToPhoneNumber = uiModel.uiState.value.toPhoneNumber
        uiModel.updateCurrency(
            currency,
            currency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
        )
        if (wasToPhoneNumber != uiModel.uiState.value.toPhoneNumber) {
            binding.contact.setText("")
        }
        updateCurrencyIcon(currency)
        updateWalletBalances()
        updateCommissionAndTotal(binding.sumInput.text.toString())
        setupChangeButton()
        setContactHint()
        if (uiModel.uiState.value.toPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
    }

    private fun updateCurrencyIcon(currency: CurrencyEnum) {
        when (currency) {
            CurrencyEnum.SOM -> setCurrencyUI(R.drawable.som_icon, getString(R.string.som), true)
            CurrencyEnum.ESOM -> setCurrencyUI(
                R.drawable.salam_icon,
                getString(R.string.digital),
                false
            )

            CurrencyEnum.USDT_TRC20 -> setCurrencyUI(
                R.drawable.usdt_icon,
                getString(R.string.usdt),
                false
            )
        }
    }

    private fun setCurrencyUI(iconRes: Int, title: String, showSomIcons: Boolean) {
        binding.icon.setImageResource(iconRes)
        binding.currencyTitle.text = title
        binding.comissionSomIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.totalSomIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon50.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon100.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon1000.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon10000.visibility = if (showSomIcons) View.VISIBLE else View.GONE
    }

    private fun updateCommissionAndTotal(amountText: String) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val commission = calculateTransferCommission(amount, uiModel.uiState.value.fromCurrency)

        val totalAmount = amount + commission

        binding.comissionValue.text = formatTransferAmount(commission)
        binding.total.text = formatTransferAmount(totalAmount.coerceAtLeast(0.0))
    }

    private fun calculateTransferCommission(amount: Double, currency: CurrencyEnum): Double {
        if (amount <= 0.0) return 0.0
        return model.calculateTransferFee(amount, currency)
    }

    private fun formatTransferAmount(amount: Double): String {
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun applyPhoneMask() {
        val editText = binding.contact
        (editText.tag as? TextWatcher)?.let(editText::removeTextChangedListener)
        editText.tag = null

        editText.setText("")
        editText.setHorizontallyScrolling(false)
        editText.isSingleLine = false
        editText.maxLines = 3

        editText.tag = editText.applyKyrgyzPhoneMask()
    }

    private fun removePhoneMask() {
        (binding.contact.tag as? TextWatcher)?.let(binding.contact::removeTextChangedListener)
        binding.contact.tag = null
        binding.contact.setText("")
        binding.contact.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        binding.contact.filters = arrayOf()

        binding.contact.setHorizontallyScrolling(false)
        binding.contact.isSingleLine = false
        binding.contact.maxLines = 3
    }

    private fun handleTransferButtonClick() {
        if (model.transferRes.value is UiState.Loading) return
        val sum = binding.sumInput.text.toString().toDoubleOrNull()
        val contactInfo = binding.contact.text.toString().trim()
        if (sum == null) {
            binding.root.showErrorSnackbar("Введите сумму для перевода")
            return
        }

        when {
            uiModel.uiState.value.toPhoneNumber -> {
                if (!contactInfo.isCompleteKyrgyzPhone()) {
                    binding.root.showErrorSnackbar("Введите корректный номер телефона")
                    return
                }
            }

            else -> {
                if (contactInfo.isEmpty()) {
                    binding.root.showErrorSnackbar("Введите адрес получателя")
                    return
                }
                if (contactInfo.length < 20) {
                    binding.root.showErrorSnackbar("Адрес слишком короткий")
                    return
                }
            }
        }

        val walletBalance =
            (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == uiModel.uiState.value.fromCurrency }?.balance
                ?: 0.0
        val fee = calculateTransferCommission(sum, uiModel.uiState.value.fromCurrency)
        if (sum + fee > walletBalance) {
            val currencyName = getCurrencyName(uiModel.uiState.value.fromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        val phone = if (uiModel.uiState.value.toPhoneNumber) contactInfo.kyrgyzPhoneDigits() else null
        val address = if (!uiModel.uiState.value.toPhoneNumber) contactInfo else null

        if (model.transferRes.value !is UiState.Loading) {
            val recipient = if (uiModel.uiState.value.toPhoneNumber) {
                contactInfo
            } else {
                address.orEmpty()
            }
            showTransferConfirmation(
                amount = sum,
                phone = phone.orEmpty(),
                address = address,
                recipient = recipient
            )
        }
    }

    private fun showTransferConfirmation(
        amount: Double,
        phone: String,
        address: String?,
        recipient: String
    ) {
        val amountText = "${amount.formatBalanceNew()} ${getCurrencyName(uiModel.uiState.value.fromCurrency)}"
        parentFragmentManager.setFragmentResult(
            TransferConfirmationFragment.DATA_REQUEST_KEY,
            bundleOf(
                TransferConfirmationFragment.TITLE_KEY to getString(
                    R.string.transfer_confirmation_message,
                    amountText,
                    recipient
                ),
                TransferConfirmationFragment.OPERATION_KEY to OPERATION_TRANSFER,
                TransferConfirmationFragment.AMOUNT_KEY to amount,
                TransferConfirmationFragment.CREDITED_AMOUNT_KEY to amount,
                TransferConfirmationFragment.FEE_KEY to calculateTransferCommission(
                    amount,
                    uiModel.uiState.value.fromCurrency
                ),
                TransferConfirmationFragment.TOTAL_DEBITED_KEY to (
                    amount + calculateTransferCommission(amount, uiModel.uiState.value.fromCurrency)
                ),
                TransferConfirmationFragment.FROM_CURRENCY_KEY to uiModel.uiState.value.fromCurrency.name,
                TransferConfirmationFragment.PHONE_KEY to phone,
                TransferConfirmationFragment.ADDRESS_KEY to address,
                TransferConfirmationFragment.OPERATION_TITLE_KEY to getString(R.string.transfer),
                TransferConfirmationFragment.PAID_FROM_KEY to getCurrentUserAccountForSuccess(),
                TransferConfirmationFragment.RECIPIENT_KEY to recipient
            )
        )
        findNavController().navigate(NavGraphDirections.startTransferConfirmationFragment())
    }

    private fun getCurrentUserAccountForSuccess(): String {
        val user = (model.myData.value as? UiState.Success)?.data
        val walletAddress = user?.wallets?.firstOrNull { it.currency == uiModel.uiState.value.fromCurrency }?.address.orEmpty()
        return when {
            uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM -> user?.phone.orEmpty()
            walletAddress.isNotBlank() -> walletAddress
            else -> user?.phone.orEmpty()
        }
    }

    companion object {
        private const val OPERATION_TRANSFER = "transfer"
    }

    private fun getCurrencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun currentUserFullName(): String {
        val user = (model.myData.value as? UiState.Success)?.data ?: return ""
        return listOf(user.firstName, user.middleName.orEmpty(), user.lastName)
            .filter(String::isNotBlank)
            .joinToString(" ")
    }

    private fun slideIn(view: View) {
        view.slideInFromBottom()
    }

    private fun slideOut(view: View, onEnd: (() -> Unit)? = null) {
        view.slideOut(view.height.toFloat(), 450L, onEnd)
    }
}
