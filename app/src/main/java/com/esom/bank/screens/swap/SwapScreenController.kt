package com.esom.bank.screens.swap

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.displayName
import com.esom.bank.common.utils.iconRes
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.setupDecimalAmountInput
import com.esom.bank.common.utils.views.toDecimalAmountOrNull
import com.esom.bank.common.utils.views.setTextProgrammatically
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.slideInFromTop
import com.esom.bank.common.utils.views.slideOut
import com.esom.bank.databinding.FragmentSwapBinding
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.dialog.TransferConfirmationFragment
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import java.math.BigDecimal
import java.math.RoundingMode

internal class SwapScreenController(
    private val fragment: Fragment,
    private val binding: FragmentSwapBinding,
    private val args: SwapFragmentArgs,
    private val model: MainViewModel,
    private val uiModel: SwapUiStateViewModel
) {

    companion object {
        private const val TAG = "SwapFragment"
        private const val OPERATION_CONVERT = "convert"
    }

    fun bind() {
        binding.root.doOnApplyWindowInsets { rootView, insets, rect ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            rootView.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if (imeBottom == 0) systemBarsBottom else imeBottom
            )
            insets
        }

        binding.sum.setupDecimalAmountInput()
        binding.peopleSum.setupDecimalAmountInput()
        initInitialIcons()
        setupQuickAmounts()
        setupClickListeners()
        setupTransferConfirmationResultListener()
        if (args.amount > 0f) {
            binding.sum.setTextProgrammatically(formatTemplateAmount(args.amount.toDouble()))
            updateAmountsFromSend(args.amount.toDouble())
        }

        if (binding.sum.text.isNullOrBlank()) {
            binding.sum.setText("0")
        }
        if (binding.peopleSum.text.isNullOrBlank()) {
            binding.peopleSum.setText("0")
        }

        updateBalanceDisplay()
        updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
        model.getFees()
        model.getSettings()
        executeAutomaticRepeatIfReady()
    }

    private fun requireContext() = fragment.requireContext()
    private fun requireActivity() = fragment.requireActivity()
    private fun findNavController() = fragment.findNavController()
    private fun getString(id: Int, vararg args: Any): String = fragment.getString(id, *args)
    private val resources get() = fragment.resources
    private val viewLifecycleOwner get() = fragment.viewLifecycleOwner
    private val parentFragmentManager get() = fragment.parentFragmentManager

    private fun setupQuickAmounts() {
        listOf(
            binding.sum50Layout to "50",
            binding.sum100Layout to "100",
            binding.sum1000Layout to "1000",
            binding.sum10000Layout to "10000",
        ).forEach { (layout, value) ->
            layout.setOnClickListener {
                binding.sum.setText(value)
                binding.sum.setSelection(binding.sum.text?.length ?: 0)
                updateAmountsFromSend(parseAmount(value))
            }
        }

        listOf(
            binding.peopleSum50Layout to "50",
            binding.peopleSum100Layout to "100",
            binding.peopleSum1000Layout to "1000",
            binding.peopleSum10000Layout to "10000",
        ).forEach { (layout, value) ->
            layout.setOnClickListener {
                binding.peopleSum.setText(value)
                binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
                updateAmountsFromReceive(parseAmount(value))
            }
        }

        binding.sumAllLayout.setOnClickListener {
            val allAvailable = getAvailableFromBalance()
            binding.sum.setText(formatInputAmount(allAvailable))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            updateAmountsFromSend(allAvailable)
        }

        binding.peopleSumAllLayout.setOnClickListener {
            val allAvailable = getAvailableFromBalance()
            val grossConvertedAmount = convertWithoutFee(allAvailable)

            uiModel.setUpdatingAmounts(true)
            binding.sum.setText(formatInputAmount(allAvailable))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            binding.peopleSum.setText(formatInputAmount(grossConvertedAmount))
            binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
            uiModel.setUpdatingAmounts(false)

            updateCommissionAndTotal(allAvailable, grossConvertedAmount)
        }
    }

    private fun setupTransferConfirmationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            TransferConfirmationFragment.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (!bundle.getBoolean(TransferConfirmationFragment.CONFIRMED_KEY)) return@setFragmentResultListener
            if (bundle.getString(TransferConfirmationFragment.OPERATION_KEY) != OPERATION_CONVERT) {
                return@setFragmentResultListener
            }

            val fromCurrency = CurrencyEnum.fromNameOrNull(
                bundle.getString(TransferConfirmationFragment.FROM_CURRENCY_KEY)
            ) ?: return@setFragmentResultListener
            val toCurrency = CurrencyEnum.fromNameOrNull(
                bundle.getString(TransferConfirmationFragment.TO_CURRENCY_KEY)
            ) ?: return@setFragmentResultListener
            val amount = bundle.getDouble(TransferConfirmationFragment.AMOUNT_KEY)
            val creditedAmount = bundle.getDouble(TransferConfirmationFragment.CREDITED_AMOUNT_KEY)
            val fee = bundle.getDouble(TransferConfirmationFragment.FEE_KEY)
            val totalDebited = bundle.getDouble(TransferConfirmationFragment.TOTAL_DEBITED_KEY)

            uiModel.setPendingTemplate(SwapTemplate(
                amount = amount,
                fromCurrency = fromCurrency.name,
                toCurrency = toCurrency.name
            ))

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = fromCurrency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.convertation),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty(),
                    receiptNumber = "",
                    fee = fee,
                    feeCurrency = fromCurrency,
                    creditedAmount = creditedAmount,
                    creditedCurrency = toCurrency,
                    debitedCurrency = fromCurrency,
                    conversionSide = getConversionSide(fromCurrency, toCurrency),
                    targetCurrency = toCurrency,
                    totalDebitedAmount = totalDebited,
                    senderName = currentUserFullName()
                )
            )
            model.convert(fromCurrency, toCurrency, amount)
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().popBackStack()
        }

        binding.currenciesBtn.setOnClickListener {
            if (uiModel.uiState.value.toPanelShown) return@setOnClickListener

            if (uiModel.uiState.value.fromPanelShown) {
                binding.peopleCurrenciesBtn.isClickable = true
                slideOut(binding.typeCurrencyLayout)
            } else {
                binding.peopleCurrenciesBtn.isClickable = false
                binding.typeCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.typeCurrencyLayout)
            }
            uiModel.toggleFromPanel()
        }

        binding.peopleCurrenciesBtn.setOnClickListener {
            if (uiModel.uiState.value.fromPanelShown) return@setOnClickListener

            if (uiModel.uiState.value.toPanelShown) {
                binding.currenciesBtn.isClickable = true
                binding.peopleLayout.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            } else {
                binding.currenciesBtn.isClickable = false
                binding.peopleLayout.elevation = 20f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            uiModel.toggleToPanel()
        }

        fun closeAllPanels() {
            if (uiModel.uiState.value.fromPanelShown) {
                binding.peopleCurrenciesBtn.isClickable = true
                slideOut(binding.typeCurrencyLayout)
            }
            if (uiModel.uiState.value.toPanelShown) {
                binding.currenciesBtn.isClickable = true
                binding.peopleLayout.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            }
            uiModel.closePanels()
        }

        binding.firstUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.USDT_TRC20)
            }
            closeAllPanels()
        }

        binding.firstDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in uiModel.uiState.value.fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.ESOM)
            }
            closeAllPanels()
        }

        binding.firstSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in uiModel.uiState.value.fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.SOM)
            }
            closeAllPanels()
        }

        binding.peopleUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.USDT_TRC20)
            }
            closeAllPanels()
        }

        binding.peopleDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in uiModel.uiState.value.toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.ESOM)
            }
            closeAllPanels()
        }

        binding.peopleSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in uiModel.uiState.value.toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.SOM)
            }
            closeAllPanels()
        }

        binding.sum.setOnUserTextChangeListener {
            if (uiModel.uiState.value.updatingAmounts) return@setOnUserTextChangeListener
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
        }

        binding.peopleSum.setOnUserTextChangeListener {
            if (uiModel.uiState.value.updatingAmounts) return@setOnUserTextChangeListener
            updateAmountsFromReceive(
                parseAmount(binding.peopleSum.text?.toString()),
                preserveReceiveInput = true
            )
        }

        binding.sendBtn.setOnClickListener {
            handleConvertButtonClick()
        }

        model.myData.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is UiState.Success -> {
                    updateBalanceDisplay()
                    updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
                }

                else -> Unit
            }
        }

        model.swapRes.observe(viewLifecycleOwner) {
            binding.sendText.isVisible = it !is UiState.Loading
            binding.indicator.isVisible = it is UiState.Loading

            when (it) {
                is UiState.Error -> {
                    uiModel.setPendingTemplate(null)
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
                    findNavController().navigate(
                        NavGraphDirections.startSuccessTransferFragment()
                    )
                }

                else -> Unit
            }
        }

        model.settings.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    Log.d(
                        TAG,
                        "Settings loaded: esom_per_usd=${state.data.esomPerUsd}"
                    )
                    updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
                    executeAutomaticRepeatIfReady()
                }
                is UiState.Error -> {
                    Log.e(TAG, "Settings load error: ${state.message}")
                }
                is UiState.Loading -> {
                    Log.d(TAG, "Settings loading...")
                }
                null -> Unit
            }
        }

        model.fees.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
                executeAutomaticRepeatIfReady()
            }
        }
    }

    private fun executeAutomaticRepeatIfReady() {
        if (!args.autoExecute || args.amount <= 0f) return
        if (model.fees.value !is UiState.Success || model.settings.value !is UiState.Success) return
        if (!uiModel.startAutomaticRepeat()) return

        val amount = args.amount.toDouble()
        val from = uiModel.uiState.value.fromCurrency
        val to = uiModel.uiState.value.toCurrency
        model.setLastSuccessOperation(
            SuccessOperationModel(
                amount = amount,
                currency = from,
                operationTitle = getString(R.string.convertation),
                paidFromAccount = getAccountForSuccess(from),
                recipient = getAccountForSuccess(to),
                receiptNumber = "",
                fee = calculateFeePreview(amount),
                creditedAmount = calculateReceivedFromSend(amount),
                conversionSide = getConversionSide(from, to),
                targetCurrency = to,
                totalDebitedAmount = amount,
                senderName = currentUserFullName()
            )
        )
        model.convert(from, to, amount)
    }

    private fun renderTemplates() {
        val templates = model.getSwapTemplates()
        binding.templatesSection.isVisible = templates.isNotEmpty()
        binding.templatesContainer.removeAllViews()

        templates.forEach { template ->
            val from = CurrencyEnum.fromNameOrNull(template.fromCurrency) ?: return@forEach
            val to = CurrencyEnum.fromNameOrNull(template.toCurrency) ?: return@forEach
            val label = buildString {
                append(getCurrencyName(from))
                append(" -> ")
                append(getCurrencyName(to))
                append('\n')
                append(formatTemplateAmount(template.amount))
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

    private fun applyTemplate(template: SwapTemplate) {
        val from = CurrencyEnum.fromNameOrNull(template.fromCurrency) ?: return
        val to = CurrencyEnum.fromNameOrNull(template.toCurrency) ?: return
        if (from == to) return

        uiModel.initialize(from, to)
        updateCurrencyViews()
        updateFromPanelViews()
        updateToPanelViews()
        onCurrencySelectionChanged()

        binding.sum.setTextProgrammatically(formatTemplateAmount(template.amount))
        binding.sum.setSelection(binding.sum.text?.length ?: 0)
        updateAmountsFromSend(template.amount)
    }

    private fun formatTemplateAmount(amount: Double): String =
        amount.formatBalanceNew()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun selectFromCurrency(currency: CurrencyEnum) {
        uiModel.selectFrom(currency)
        updateCurrencyViews()
        updateFromPanelViews()
        onCurrencySelectionChanged()
    }

    private fun selectToCurrency(currency: CurrencyEnum) {
        uiModel.selectTo(currency)
        updateCurrencyViews()
        updateToPanelViews()
        onCurrencySelectionChanged()
    }

    private fun onCurrencySelectionChanged() {
        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()
        updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
    }

    private fun updateCommissionTitles() {
        binding.comissionTitle.text = getCurrencyName(uiModel.uiState.value.fromCurrency)
        binding.secondTitle.text = getCurrencyName(uiModel.uiState.value.toCurrency)
        binding.thirdTitle.text =
            "Комиссия (${getCurrencyName(uiModel.uiState.value.fromCurrency)})"
    }

    private fun initInitialIcons() {
        uiModel.initialize(
            CurrencyEnum.fromNameOrNull(args.from) ?: CurrencyEnum.SOM,
            CurrencyEnum.fromNameOrNull(args.to) ?: CurrencyEnum.ESOM
        )

        Log.d(TAG, "=== initInitialIcons() ===")
        Log.d(TAG, "uiModel.uiState.value.fromCurrency: $uiModel.uiState.value.fromCurrency")
        Log.d(TAG, "uiModel.uiState.value.toCurrency: $uiModel.uiState.value.toCurrency")

        updateCurrencyViews()
        updateFromPanelViews()
        updateToPanelViews()

        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()

        Log.d(TAG, "=== END initInitialIcons() ===")
    }

    private fun updateCurrencyViews() {
        binding.icon.setImageResource(getCurrencyIcon(uiModel.uiState.value.fromCurrency))
        binding.currencyTitle.text = getCurrencyString(uiModel.uiState.value.fromCurrency)

        binding.peopleIcon.setImageResource(getCurrencyIcon(uiModel.uiState.value.toCurrency))
        binding.peopleTitle.text = getCurrencyString(uiModel.uiState.value.toCurrency)
    }

    private fun updateFromPanelViews() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone
        val usdtWallet = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val esomWallet = wallets.find { it.currency == CurrencyEnum.ESOM }
        val somWallet = wallets.find { it.currency == CurrencyEnum.SOM }

        binding.firstUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.fromPanelCurrencies
        binding.firstDigitalBtn.isVisible = CurrencyEnum.ESOM in uiModel.uiState.value.fromPanelCurrencies
        binding.firstSomBtn.isVisible = CurrencyEnum.SOM in uiModel.uiState.value.fromPanelCurrencies

        bindPanelRow(
            icon = binding.firstUsdtIcon,
            title = binding.firstUsdtTitle,
            suffix = binding.firstUsdt,
            balance = binding.firstSum1,
            divider = binding.firstUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in uiModel.uiState.value.fromPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindPanelRow(
            icon = binding.firstDigitalIcon,
            title = binding.firstDigitalTitle,
            suffix = binding.firstDigital,
            balance = binding.firstSum2,
            divider = binding.firstDigitalView,
            currency = CurrencyEnum.ESOM.takeIf { it in uiModel.uiState.value.fromPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindPanelRow(
            icon = binding.firstSomIcon,
            title = binding.firstSomTitle,
            suffix = binding.firstSom,
            balance = binding.firstSum3,
            divider = binding.firstSomView,
            currency = CurrencyEnum.SOM.takeIf { it in uiModel.uiState.value.fromPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
    }

    private fun updateToPanelViews() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone
        val usdtWallet = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val esomWallet = wallets.find { it.currency == CurrencyEnum.ESOM }
        val somWallet = wallets.find { it.currency == CurrencyEnum.SOM }

        binding.peopleUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in uiModel.uiState.value.toPanelCurrencies
        binding.peopleDigitalBtn.isVisible = CurrencyEnum.ESOM in uiModel.uiState.value.toPanelCurrencies
        binding.peopleSomBtn.isVisible = CurrencyEnum.SOM in uiModel.uiState.value.toPanelCurrencies

        bindPanelRow(
            icon = binding.peopleUsdtIcon,
            title = binding.peopleUsdtTitle,
            suffix = binding.peopleUsdt,
            balance = binding.peopleSum1,
            divider = binding.peopleUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in uiModel.uiState.value.toPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindPanelRow(
            icon = binding.peopleDigitalIcon,
            title = binding.peopleDigitalTitle,
            suffix = binding.peopleDigital,
            balance = binding.peopleSum2,
            divider = binding.peopleDigitalView,
            currency = CurrencyEnum.ESOM.takeIf { it in uiModel.uiState.value.toPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindPanelRow(
            icon = binding.peopleSomIcon,
            title = binding.peopleSomTitle,
            suffix = binding.peopleSom,
            balance = binding.peopleSum3,
            divider = binding.peopleSomView,
            currency = CurrencyEnum.SOM.takeIf { it in uiModel.uiState.value.toPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
    }

    private fun getCurrencyIcon(currency: CurrencyEnum): Int = currency.iconRes()

    private fun getCurrencyString(currency: CurrencyEnum): String =
        currency.displayName(requireContext())

    private fun updateSomIconsVisibility() {
        binding.thirdIconSwap.isVisible = uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM
        binding.somIconSwap.isVisible = uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM
        binding.salamIconSwap.isVisible = uiModel.uiState.value.toCurrency == CurrencyEnum.SOM
        binding.totalSomIcon.isVisible = uiModel.uiState.value.toCurrency == CurrencyEnum.SOM
    }

    private fun updateBalanceDisplay() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return

        val fromWallet = wallets.find { it.currency == uiModel.uiState.value.fromCurrency }
        val fromBalance = fromWallet?.balance ?: 0.0

        binding.sendAvailableTitle.text =
            getString(R.string.available_title, fromBalance.formatBalanceNew())

        updateFromPanelViews()
        updateToPanelViews()
    }

    private fun bindPanelRow(
        icon: android.widget.ImageView,
        title: TextView,
        suffix: TextView,
        balance: TextView,
        divider: View,
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

    private fun getAvailableFromBalance(): Double {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return 0.0
        return wallets.find { it.currency == uiModel.uiState.value.fromCurrency }?.balance ?: 0.0
    }

    private fun updateAmountsFromSend(fromAmount: Double) {
        val grossConvertedAmount = convertWithoutFee(fromAmount)

        uiModel.setUpdatingAmounts(true)
        binding.peopleSum.setTextProgrammatically(
            formatInputAmount(grossConvertedAmount)
        )
        binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
        uiModel.setUpdatingAmounts(false)

        updateCommissionAndTotal(fromAmount, grossConvertedAmount)
    }

    private fun updateAmountsFromReceive(
        receivedAmount: Double,
        preserveReceiveInput: Boolean = false
    ) {
        val fromAmount = calculateSendFromReceived(receivedAmount)

        uiModel.setUpdatingAmounts(true)
        binding.sum.setTextProgrammatically(formatInputAmount(fromAmount))
        binding.sum.setSelection(binding.sum.text?.length ?: 0)
        if (!preserveReceiveInput) {
            binding.peopleSum.setTextProgrammatically(
                formatInputAmount(receivedAmount)
            )
            binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
        }
        uiModel.setUpdatingAmounts(false)

        updateCommissionAndTotal(fromAmount, receivedAmount)
    }

    private fun updateCommissionAndTotal(
        fromAmount: Double? = null,
        convertedAmount: Double? = null
    ) {
        val grossAmount = fromAmount ?: parseAmount(binding.sum.text?.toString())
        val actualConvertedAmount = convertedAmount ?: convertWithoutFee(grossAmount)
        val fee = calculateFeePreview(grossAmount)
        val convertedFee = convertWithoutFee(fee)
        val netConvertedAmount = (actualConvertedAmount - convertedFee).coerceAtLeast(0.0)

        binding.thirdValue.text = formatCurrencyAmount(fee)
        binding.comissionValue.text = formatCurrencyAmount(grossAmount)
        binding.secondValue.text = formatCurrencyAmount(actualConvertedAmount)
        binding.total.text = formatCurrencyAmount(netConvertedAmount)

        Log.d(
            TAG,
            "Conversion: $grossAmount ${getCurrencyName(uiModel.uiState.value.fromCurrency)} -> " +
                    "$actualConvertedAmount ${getCurrencyName(uiModel.uiState.value.toCurrency)}; " +
                    "net=$netConvertedAmount ${getCurrencyName(uiModel.uiState.value.toCurrency)}"
        )
        if (!isSomToEsomConversion()) {
            Log.d(TAG, "Курс обмена: ${getExchangeRate()}")
        }
        Log.d(
            TAG,
            "Комиссия: $fee ${getCurrencyName(uiModel.uiState.value.fromCurrency)}"
        )
    }

    private fun calculateReceivedFromSend(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        val fee = calculateFeePreview(fromAmount)
        val netAmount = (fromAmount - fee).coerceAtLeast(0.0)
        return convertWithoutFee(netAmount)
    }

    private fun calculateSendFromReceived(receivedAmount: Double): Double {
        if (receivedAmount <= 0.0) return 0.0
        return invertConvertWithoutFee(receivedAmount)
    }

    private fun isSomToEsomConversion(): Boolean {
        return (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM && uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM) ||
                (uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM && uiModel.uiState.value.toCurrency == CurrencyEnum.SOM)
    }

    private fun formatInputAmount(amount: Double): String {
        return if (amount == 0.0) {
            "0"
        } else {
            formatCurrencyAmount(amount)
        }
    }

    private fun parseAmount(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.toDecimalAmountOrNull() ?: 0.0
    }

    private fun calculateFeePreview(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        return model.calculateConvertFee(fromAmount, uiModel.uiState.value.fromCurrency, uiModel.uiState.value.toCurrency)
    }

    private fun convertWithoutFee(fromAmount: Double): Double {
        val exchangeRate = getExchangeRate()
        if (exchangeRate == 0.0) return 0.0
        return if (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM) {
            fromAmount / exchangeRate
        } else {
            fromAmount * exchangeRate
        }
    }

    private fun invertConvertWithoutFee(grossOut: Double): Double {
        val exchangeRate = getExchangeRate()
        if (exchangeRate == 0.0) return 0.0
        return if (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM) {
            grossOut * exchangeRate
        } else {
            grossOut / exchangeRate
        }
    }

    private fun getEsomPerUsdOrNull(): Double? {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return null
        val v = settings.esomPerUsd
        return if (v > 0.0) v else null
    }

    private fun getUsdBuyRateOrNull(): Double? {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return null
        return settings.usdBuyRate.takeIf { it > 0.0 } ?: getEsomPerUsdOrNull()
    }

    private fun getUsdSellRateOrNull(): Double? {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return null
        return settings.usdSellRate.takeIf { it > 0.0 } ?: getEsomPerUsdOrNull()
    }

    private fun getExchangeRate(): Double {
        return when {
            uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM && uiModel.uiState.value.toCurrency == CurrencyEnum.SOM -> 1.0
            uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM && uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM -> 1.0
            (uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM) &&
                    uiModel.uiState.value.toCurrency == CurrencyEnum.USDT_TRC20 -> getUsdSellRateOrNull() ?: 1.0
            uiModel.uiState.value.fromCurrency == CurrencyEnum.USDT_TRC20 &&
                    (uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM || uiModel.uiState.value.toCurrency == CurrencyEnum.SOM) ->
                getUsdBuyRateOrNull() ?: 1.0
            else -> 1.0
        }
    }

    private fun handleConvertButtonClick() {
        if (model.swapRes.value is UiState.Loading) return

        val fromAmount = parseAmount(binding.sum.text?.toString())
        if (fromAmount <= 0.0) {
            binding.root.showErrorSnackbar("Введите сумму для обмена")
            return
        }

        val walletBalance = (model.myData.value as? UiState.Success)?.data?.wallets
            ?.find { it.currency == uiModel.uiState.value.fromCurrency }
            ?.balance ?: 0.0

        if (fromAmount > walletBalance) {
            val currencyName = getCurrencyName(uiModel.uiState.value.fromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        Log.d(TAG, "Запуск конвертации:")
        Log.d(TAG, "From: $uiModel.uiState.value.fromCurrency, To: $uiModel.uiState.value.toCurrency, Amount: $fromAmount")
        Log.d(TAG, "Курс: ${getExchangeRate()}")

        showConvertConfirmation(fromAmount)
    }

    private fun showConvertConfirmation(amount: Double) {
        val amountText = "${formatCurrencyAmount(amount)} ${getCurrencyName(uiModel.uiState.value.fromCurrency)}"
        val target = getCurrencyName(uiModel.uiState.value.toCurrency)
        val creditedAmount = calculateReceivedFromSend(amount)
        val sourceFee = calculateFeePreview(amount)
        parentFragmentManager.setFragmentResult(
            TransferConfirmationFragment.DATA_REQUEST_KEY,
            bundleOf(
                TransferConfirmationFragment.TITLE_KEY to getString(
                    R.string.transfer_confirmation_message,
                    amountText,
                    target
                ),
                TransferConfirmationFragment.OPERATION_KEY to OPERATION_CONVERT,
                TransferConfirmationFragment.AMOUNT_KEY to amount,
                TransferConfirmationFragment.CREDITED_AMOUNT_KEY to creditedAmount,
                TransferConfirmationFragment.FEE_KEY to sourceFee,
                TransferConfirmationFragment.TOTAL_DEBITED_KEY to amount,
                TransferConfirmationFragment.FROM_CURRENCY_KEY to uiModel.uiState.value.fromCurrency.name,
                TransferConfirmationFragment.TO_CURRENCY_KEY to uiModel.uiState.value.toCurrency.name,
                TransferConfirmationFragment.OPERATION_TITLE_KEY to getString(R.string.convertation),
                TransferConfirmationFragment.PAID_FROM_KEY to getAccountForSuccess(uiModel.uiState.value.fromCurrency),
                TransferConfirmationFragment.RECIPIENT_KEY to getAccountForSuccess(uiModel.uiState.value.toCurrency)
            )
        )
        findNavController().navigate(NavGraphDirections.startTransferConfirmationFragment())
    }

    private fun formatCurrencyAmount(amount: Double): String {
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun getAccountForSuccess(currency: CurrencyEnum): String {
        val user = (model.myData.value as? UiState.Success)?.data
        val walletAddress = user?.wallets?.firstOrNull { it.currency == currency }?.address.orEmpty()
        return when {
            currency == CurrencyEnum.SOM -> user?.phone.orEmpty()
            walletAddress.isNotBlank() -> walletAddress
            else -> user?.phone.orEmpty()
        }
    }

    private fun currentUserFullName(): String {
        val user = (model.myData.value as? UiState.Success)?.data ?: return ""
        return listOf(user.lastName, user.firstName, user.middleName.orEmpty())
            .filter(String::isNotBlank)
            .joinToString(" ")
    }

    private fun getConversionSide(from: CurrencyEnum, to: CurrencyEnum): ConversionSide? =
        when {
            from == CurrencyEnum.ESOM && to == CurrencyEnum.SOM -> ConversionSide.IN
            from == CurrencyEnum.SOM && to == CurrencyEnum.ESOM -> ConversionSide.OUT
            else -> null
        }

    private fun getCurrencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun slideIn(view: View) {
        view.slideInFromTop(dp(8).toFloat())
    }

    private fun slideOut(view: View, onEnd: (() -> Unit)? = null) {
        view.slideOut(-dp(8).toFloat(), 160L, onEnd)
    }
}
