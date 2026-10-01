package com.esom.bank.screens.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.displayName
import com.esom.bank.common.utils.iconRes
import com.esom.bank.common.utils.views.addRecentTemplateView
import com.esom.bank.common.utils.views.applyPaymentWindowInsets
import com.esom.bank.common.utils.views.formatTemplateAmount
import com.esom.bank.common.utils.views.paymentName
import com.esom.bank.common.utils.views.setQuickAmountClick
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.setupDecimalAmountInput
import com.esom.bank.common.utils.toMoneyDecimalOrZero
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
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

private const val OPERATION_CONVERT = "convert"
private const val CONVERSION_IDEMPOTENCY_KEY = "conversion_idempotency_key"

@AndroidEntryPoint
class SwapFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val uiModel: SwapUiStateViewModel by viewModels()
    private val args: SwapFragmentArgs by navArgs()
    private var _binding: FragmentSwapBinding? = null
    private val binding: FragmentSwapBinding
        get() = _binding ?: error("Binding accessed outside of the view lifecycle")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSwapBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSwapBinding.bind(view)
        bind()
    }



    private fun bind() {
        binding.root.applyPaymentWindowInsets()

        binding.sum.setupDecimalAmountInput()
        binding.peopleSum.setupDecimalAmountInput()
        initInitialIcons()
        setupQuickAmounts()
        setupClickListeners()
        setupTransferConfirmationResultListener()
        val initialAmount = args.amount.toMoneyDecimalOrZero()
        if (initialAmount > BigDecimal.ZERO) {
            binding.sum.setTextProgrammatically(formatTemplateAmount(initialAmount))
            updateAmountsFromSend(initialAmount)
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

    private fun setupQuickAmounts() {
        listOf(
            binding.sum50Layout to BigDecimal("50"),
            binding.sum100Layout to BigDecimal("100"),
            binding.sum1000Layout to BigDecimal("1000"),
            binding.sum10000Layout to BigDecimal("10000")
        ).forEach { (layout, value) ->
            layout.setQuickAmountClick(binding.sum, value) { text ->
                updateAmountsFromSend(parseAmount(text))
            }
        }

        listOf(
            binding.peopleSum50Layout to BigDecimal("50"),
            binding.peopleSum100Layout to BigDecimal("100"),
            binding.peopleSum1000Layout to BigDecimal("1000"),
            binding.peopleSum10000Layout to BigDecimal("10000")
        ).forEach { (layout, value) ->
            layout.setQuickAmountClick(binding.peopleSum, value) { text ->
                updateAmountsFromReceive(parseAmount(text))
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
            val receivedAmount = calculateReceivedFromSend(allAvailable)

            uiModel.setUpdatingAmounts(true)
            binding.sum.setText(formatInputAmount(allAvailable))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            binding.peopleSum.setText(formatInputAmount(receivedAmount))
            binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
            uiModel.setUpdatingAmounts(false)

            updateCommissionAndTotal(allAvailable)
        }
    }

    private fun setupTransferConfirmationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            TransferConfirmationFragment.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (!bundle.getBoolean(TransferConfirmationFragment.CONFIRMED_KEY)) {
                uiModel.clearConversionIdempotencyKey()
                return@setFragmentResultListener
            }
            if (bundle.getString(TransferConfirmationFragment.OPERATION_KEY) != OPERATION_CONVERT) {
                return@setFragmentResultListener
            }

            val fromCurrency = CurrencyEnum.fromNameOrNull(
                bundle.getString(TransferConfirmationFragment.FROM_CURRENCY_KEY)
            ) ?: return@setFragmentResultListener
            val toCurrency = CurrencyEnum.fromNameOrNull(
                bundle.getString(TransferConfirmationFragment.TO_CURRENCY_KEY)
            ) ?: return@setFragmentResultListener
            val amount = bundle.getString(TransferConfirmationFragment.AMOUNT_KEY).orEmpty().toMoneyDecimalOrZero()
            val creditedAmount = bundle.getString(TransferConfirmationFragment.CREDITED_AMOUNT_KEY).orEmpty().toMoneyDecimalOrZero()
            val fee = bundle.getString(TransferConfirmationFragment.FEE_KEY).orEmpty().toMoneyDecimalOrZero()
            val totalDebited = bundle.getString(TransferConfirmationFragment.TOTAL_DEBITED_KEY).orEmpty().toMoneyDecimalOrZero()
            val idempotencyKey = bundle.getString(CONVERSION_IDEMPOTENCY_KEY)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: UUID.randomUUID().toString()

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
            model.convert(fromCurrency, toCurrency, amount, idempotencyKey)
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
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
                    uiModel.clearConversionIdempotencyKey()
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
                    updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
                    executeAutomaticRepeatIfReady()
                }
                is UiState.Error, is UiState.Loading -> Unit
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
        val amount = args.amount.toMoneyDecimalOrZero()
        if (!args.autoExecute || amount <= BigDecimal.ZERO) return
        if (model.fees.value !is UiState.Success || model.settings.value !is UiState.Success) return
        if (!uiModel.startAutomaticRepeat()) return

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
        model.convert(from, to, amount, UUID.randomUUID().toString())
    }

    private fun renderTemplates() {
        val templates = model.getSwapTemplates()
        binding.templatesSection.isVisible = templates.isNotEmpty()
        binding.templatesContainer.removeAllViews()

        templates.forEach { template ->
            val from = CurrencyEnum.fromNameOrNull(template.fromCurrency) ?: return@forEach
            val to = CurrencyEnum.fromNameOrNull(template.toCurrency) ?: return@forEach
            val label = buildString {
                append(paymentName(from))
                append(" -> ")
                append(paymentName(to))
                append('\n')
                append(formatTemplateAmount(template.amount))
            }
            binding.templatesContainer.addRecentTemplateView(label) { applyTemplate(template) }
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
        binding.comissionTitle.text = paymentName(uiModel.uiState.value.fromCurrency)
        binding.secondTitle.text = paymentName(uiModel.uiState.value.toCurrency)
        binding.thirdTitle.text =
            "Комиссия (${paymentName(uiModel.uiState.value.fromCurrency)})"
    }

    private fun initInitialIcons() {
        uiModel.initialize(
            CurrencyEnum.fromNameOrNull(args.from) ?: CurrencyEnum.SOM,
            CurrencyEnum.fromNameOrNull(args.to) ?: CurrencyEnum.ESOM
        )

        updateCurrencyViews()
        updateFromPanelViews()
        updateToPanelViews()

        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()

    }

    private fun updateCurrencyViews() {
        binding.icon.setImageResource(uiModel.uiState.value.fromCurrency.iconRes())
        binding.currencyTitle.text = uiModel.uiState.value.fromCurrency.displayName(requireContext())

        binding.peopleIcon.setImageResource(uiModel.uiState.value.toCurrency.iconRes())
        binding.peopleTitle.text = uiModel.uiState.value.toCurrency.displayName(requireContext())
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

    private fun updateSomIconsVisibility() {
        binding.thirdIconSwap.isVisible = uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM
        binding.somIconSwap.isVisible = uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM
        binding.salamIconSwap.isVisible = uiModel.uiState.value.toCurrency == CurrencyEnum.SOM
        binding.totalSomIcon.isVisible = uiModel.uiState.value.toCurrency == CurrencyEnum.SOM
    }

    private fun updateBalanceDisplay() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return

        val fromWallet = wallets.find { it.currency == uiModel.uiState.value.fromCurrency }
        val fromBalance = fromWallet?.balance ?: BigDecimal.ZERO

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
        title.text = paymentName(currency)
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

    private fun getAvailableFromBalance(): BigDecimal {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return BigDecimal.ZERO
        return wallets.find { it.currency == uiModel.uiState.value.fromCurrency }?.balance ?: BigDecimal.ZERO
    }

    private fun updateAmountsFromSend(fromAmount: BigDecimal) {
        val receivedAmount = calculateReceivedFromSend(fromAmount)

        uiModel.setUpdatingAmounts(true)
        binding.peopleSum.setTextProgrammatically(
            formatInputAmount(receivedAmount)
        )
        binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
        uiModel.setUpdatingAmounts(false)

        updateCommissionAndTotal(fromAmount)
    }

    private fun updateAmountsFromReceive(
        receivedAmount: BigDecimal,
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

        // The receive field is already the desired net target amount. Do not
        // subtract the source fee from it a second time in the preview.
        updateCommissionAndTotal(fromAmount, targetAmountIsNet = true)
    }

    private fun updateCommissionAndTotal(
        fromAmount: BigDecimal? = null,
        targetAmountIsNet: Boolean = false
    ) {
        val grossAmount = fromAmount ?: parseAmount(binding.sum.text?.toString())
        val actualConvertedAmount = convertWithoutFee(grossAmount)
        val fee = calculateFeePreview(grossAmount)
        val convertedFee = convertWithoutFee(fee)
        val netConvertedAmount = if (targetAmountIsNet) {
            parseAmount(binding.peopleSum.text?.toString())
        } else {
            (actualConvertedAmount - convertedFee).max(BigDecimal.ZERO)
        }

        binding.thirdValue.text = formatCurrencyAmount(fee)
        binding.comissionValue.text = formatCurrencyAmount(grossAmount)
        binding.secondValue.text = formatCurrencyAmount(actualConvertedAmount)
        binding.total.text = formatCurrencyAmount(netConvertedAmount)

    }

    private fun calculateReceivedFromSend(fromAmount: BigDecimal): BigDecimal {
        if (fromAmount <= BigDecimal.ZERO) return BigDecimal.ZERO
        val fee = calculateFeePreview(fromAmount)
        val netAmount = (fromAmount - fee).max(BigDecimal.ZERO)
        return convertWithoutFee(netAmount)
    }

    private fun calculateSendFromReceived(receivedAmount: BigDecimal): BigDecimal {
        if (receivedAmount <= BigDecimal.ZERO) return BigDecimal.ZERO

        // The receive field is a net amount. Find the source gross amount that
        // remains after the source-side fee and converts to the requested value.
        // This also works when the configured fee is fixed, percentage-based, or
        // the greater of the two.
        val netSourceAmount = invertConvertWithoutFee(receivedAmount)
        if (netSourceAmount <= BigDecimal.ZERO) return BigDecimal.ZERO

        var lower = netSourceAmount
        var upper = netSourceAmount + calculateFeePreview(netSourceAmount).max(BigDecimal.ONE)
        var guard = 0
        while (calculateReceivedFromSend(upper) < receivedAmount && guard++ < 32) {
            upper = upper.multiply(BigDecimal("2"))
        }

        repeat(80) {
            val middle = lower.add(upper).divide(BigDecimal("2"), 24, RoundingMode.HALF_UP)
            if (calculateReceivedFromSend(middle) < receivedAmount) {
                lower = middle
            } else {
                upper = middle
            }
        }

        return upper.setScale(18, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    private fun isSomToEsomConversion(): Boolean {
        return (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM && uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM) ||
                (uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM && uiModel.uiState.value.toCurrency == CurrencyEnum.SOM)
    }

    private fun formatInputAmount(amount: BigDecimal): String {
        return if (amount.compareTo(BigDecimal.ZERO) == 0) {
            "0"
        } else {
            formatCurrencyAmount(amount)
        }
    }

    private fun parseAmount(value: String?): BigDecimal {
        if (value.isNullOrBlank()) return BigDecimal.ZERO
        return value.toMoneyDecimalOrZero()
    }

    private fun calculateFeePreview(fromAmount: BigDecimal): BigDecimal {
        if (fromAmount <= BigDecimal.ZERO) return BigDecimal.ZERO
        return model.calculateConvertFee(fromAmount, uiModel.uiState.value.fromCurrency, uiModel.uiState.value.toCurrency)
    }

    private fun convertWithoutFee(fromAmount: BigDecimal): BigDecimal {
        val exchangeRate = getExchangeRate()
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return if (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM) {
            fromAmount.divide(exchangeRate, 18, RoundingMode.HALF_UP)
        } else {
            fromAmount.multiply(exchangeRate)
        }
    }

    private fun invertConvertWithoutFee(grossOut: BigDecimal): BigDecimal {
        val exchangeRate = getExchangeRate()
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return if (uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM) {
            grossOut.multiply(exchangeRate)
        } else {
            grossOut.divide(exchangeRate, 18, RoundingMode.HALF_UP)
        }
    }

    private fun getUsdBuyRateOrNull(): BigDecimal? {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return null
        return settings.usdBuyRate.takeIf { it > BigDecimal.ZERO }
    }

    private fun getUsdSellRateOrNull(): BigDecimal? {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return null
        return settings.usdSellRate.takeIf { it > BigDecimal.ZERO }
    }

    private fun getExchangeRate(): BigDecimal {
        return when {
            uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM && uiModel.uiState.value.toCurrency == CurrencyEnum.SOM -> BigDecimal.ONE
            uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM && uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM -> BigDecimal.ONE
            (uiModel.uiState.value.fromCurrency == CurrencyEnum.ESOM || uiModel.uiState.value.fromCurrency == CurrencyEnum.SOM) &&
                    uiModel.uiState.value.toCurrency == CurrencyEnum.USDT_TRC20 -> getUsdSellRateOrNull() ?: BigDecimal.ONE
            uiModel.uiState.value.fromCurrency == CurrencyEnum.USDT_TRC20 &&
                    (uiModel.uiState.value.toCurrency == CurrencyEnum.ESOM || uiModel.uiState.value.toCurrency == CurrencyEnum.SOM) ->
                getUsdBuyRateOrNull() ?: BigDecimal.ONE
            else -> BigDecimal.ONE
        }
    }

    private fun handleConvertButtonClick() {
        if (model.swapRes.value is UiState.Loading) return

        val fromAmount = parseAmount(binding.sum.text?.toString())
        if (fromAmount <= BigDecimal.ZERO) {
            binding.root.showErrorSnackbar("Введите сумму для обмена")
            return
        }

        val walletBalance = (model.myData.value as? UiState.Success)?.data?.wallets
            ?.find { it.currency == uiModel.uiState.value.fromCurrency }
            ?.balance ?: BigDecimal.ZERO

        if (fromAmount > walletBalance) {
            val currencyName = paymentName(uiModel.uiState.value.fromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        showConvertConfirmation(fromAmount)
    }

    private fun showConvertConfirmation(amount: BigDecimal) {
        val amountText = "${formatCurrencyAmount(amount)} ${paymentName(uiModel.uiState.value.fromCurrency)}"
        val target = paymentName(uiModel.uiState.value.toCurrency)
        val idempotencyFingerprint = listOf(
            uiModel.uiState.value.fromCurrency.name,
            uiModel.uiState.value.toCurrency.name,
            amount.toPlainString(),
        ).joinToString(":")
        val idempotencyKey = uiModel.getOrCreateConversionIdempotencyKey(
            idempotencyFingerprint,
        )
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
                TransferConfirmationFragment.AMOUNT_KEY to amount.toPlainString(),
                TransferConfirmationFragment.CREDITED_AMOUNT_KEY to creditedAmount.toPlainString(),
                TransferConfirmationFragment.FEE_KEY to sourceFee.toPlainString(),
                TransferConfirmationFragment.TOTAL_DEBITED_KEY to amount.toPlainString(),
                TransferConfirmationFragment.FROM_CURRENCY_KEY to uiModel.uiState.value.fromCurrency.name,
                TransferConfirmationFragment.TO_CURRENCY_KEY to uiModel.uiState.value.toCurrency.name,
                CONVERSION_IDEMPOTENCY_KEY to idempotencyKey,
                TransferConfirmationFragment.OPERATION_TITLE_KEY to getString(R.string.convertation),
                TransferConfirmationFragment.PAID_FROM_KEY to getAccountForSuccess(uiModel.uiState.value.fromCurrency),
                TransferConfirmationFragment.RECIPIENT_KEY to getAccountForSuccess(uiModel.uiState.value.toCurrency)
            )
        )
        findNavController().navigate(NavGraphDirections.startTransferConfirmationFragment())
    }

    private fun formatCurrencyAmount(amount: BigDecimal): String {
        return amount
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


    private fun slideIn(view: View) {
        val distance = (8 * resources.displayMetrics.density).toFloat()
        view.slideInFromTop(distance)
    }

    private fun slideOut(view: View, onEnd: (() -> Unit)? = null) {
        val distance = (8 * resources.displayMetrics.density).toFloat()
        view.slideOut(-distance, 160L, onEnd)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
