package com.esom.bank.screens.swap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentSwapBinding
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.dialog.TransferConfirmationFragment
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class SwapFragment : Fragment() {

    private lateinit var binding: FragmentSwapBinding
    private val args: SwapFragmentArgs by navArgs()
    private val model: MainViewModel by activityViewModels()

    private var isPanelShown = false
    private var isPeoplePanelShown = false
    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.SOM
    private var currentToCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var fromPanelCurrencies: List<CurrencyEnum> = emptyList()
    private var toPanelCurrencies: List<CurrencyEnum> = emptyList()

    private var isUpdatingAmounts = false

    companion object {
        private const val TAG = "SwapFragment"
        private const val OPERATION_CONVERT = "convert"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSwapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.doOnApplyWindowInsets { rootView, insets, rect ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            rootView.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if (imeBottom == 0) systemBarsBottom else imeBottom
            )
            insets
        }

        initInitialIcons()
        setupQuickAmounts()
        setupClickListeners()
        setupTransferConfirmationResultListener()

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
    }

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
            binding.sum.setText(formatInputAmount(allAvailable, currentFromCurrency))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            updateAmountsFromSend(allAvailable)
        }

        binding.peopleSumAllLayout.setOnClickListener {
            val allAvailable = getAvailableFromBalance()
            val maxReceived = calculateReceivedFromSend(allAvailable)

            isUpdatingAmounts = true
            binding.sum.setText(formatInputAmount(allAvailable, currentFromCurrency))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            binding.peopleSum.setText(formatInputAmount(maxReceived, currentToCurrency))
            binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
            isUpdatingAmounts = false

            updateCommissionAndTotal(allAvailable, maxReceived)
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

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = fromCurrency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.convertation),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty(),
                    receiptNumber = "",
                    fee = calculateFeePreview(amount),
                    creditedAmount = creditedAmount,
                    conversionSide = getConversionSide(fromCurrency, toCurrency),
                    targetCurrency = toCurrency
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
            if (isPeoplePanelShown) return@setOnClickListener

            if (isPanelShown) {
                binding.peopleCurrenciesBtn.isClickable = true
                slideOut(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.GONE
            } else {
                binding.peopleCurrenciesBtn.isClickable = false
                binding.typeCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.VISIBLE
            }
            isPanelShown = !isPanelShown
        }

        binding.peopleCurrenciesBtn.setOnClickListener {
            if (isPanelShown) return@setOnClickListener

            if (isPeoplePanelShown) {
                binding.currenciesBtn.isClickable = true
                binding.peopleLayout.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            } else {
                binding.currenciesBtn.isClickable = false
                binding.peopleLayout.elevation = 20f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            isPeoplePanelShown = !isPeoplePanelShown
        }

        fun closeAllPanels() {
            if (isPanelShown) {
                binding.peopleCurrenciesBtn.isClickable = true
                slideOut(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.GONE
                isPanelShown = false
            }
            if (isPeoplePanelShown) {
                binding.currenciesBtn.isClickable = true
                binding.peopleLayout.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
                isPeoplePanelShown = false
            }
        }

        binding.backgroundConversationLayout.setOnClickListener {
            closeAllPanels()
        }

        binding.firstUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.USDT_TRC20)
            }
            closeAllPanels()
        }

        binding.firstDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.ESOM)
            }
            closeAllPanels()
        }

        binding.firstSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in fromPanelCurrencies) {
                selectFromCurrency(CurrencyEnum.SOM)
            }
            closeAllPanels()
        }

        binding.peopleUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.USDT_TRC20)
            }
            closeAllPanels()
        }

        binding.peopleDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.ESOM)
            }
            closeAllPanels()
        }

        binding.peopleSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in toPanelCurrencies) {
                selectToCurrency(CurrencyEnum.SOM)
            }
            closeAllPanels()
        }

        binding.sum.setOnUserTextChangeListener {
            if (isUpdatingAmounts) return@setOnUserTextChangeListener
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
        }

        binding.peopleSum.setOnUserTextChangeListener {
            if (isUpdatingAmounts) return@setOnUserTextChangeListener
            updateAmountsFromReceive(parseAmount(binding.peopleSum.text?.toString()))
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
                    findNavController().navigate(
                        NavGraphDirections.startFailTransferFragment(it.message)
                    )
                }

                is UiState.Success -> {
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
                }
                is UiState.Error -> {
                    Log.e(TAG, "Settings load error: ${state.message}")
                }
                is UiState.Loading -> {
                    Log.d(TAG, "Settings loading...")
                }
            }
        }

        model.fees.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            }
        }
    }

    private fun selectFromCurrency(currency: CurrencyEnum) {
        val previousCurrency = currentFromCurrency
        currentFromCurrency = currency
        fromPanelCurrencies = fromPanelCurrencies.map {
            if (it == currency) previousCurrency else it
        }
        updateCurrencyViews()
        updateFromPanelViews()
        onCurrencySelectionChanged()
    }

    private fun selectToCurrency(currency: CurrencyEnum) {
        val previousCurrency = currentToCurrency
        currentToCurrency = currency
        toPanelCurrencies = toPanelCurrencies.map {
            if (it == currency) previousCurrency else it
        }
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
        binding.comissionTitle.text = getCurrencyName(currentFromCurrency)
        binding.secondTitle.text = getCurrencyName(currentToCurrency)
    }

    private fun initInitialIcons() {
        currentFromCurrency = CurrencyEnum.fromNameOrNull(args.from) ?: CurrencyEnum.SOM
        currentToCurrency = CurrencyEnum.fromNameOrNull(args.to) ?: CurrencyEnum.ESOM

        Log.d(TAG, "=== initInitialIcons() ===")
        Log.d(TAG, "currentFromCurrency: $currentFromCurrency")
        Log.d(TAG, "currentToCurrency: $currentToCurrency")

        val allCurrencies = CurrencyEnum.supportedValues

        fromPanelCurrencies = allCurrencies
            .filter { it != currentFromCurrency }

        toPanelCurrencies = allCurrencies
            .filter { it != currentToCurrency }

        updateCurrencyViews()
        updateFromPanelViews()
        updateToPanelViews()

        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()

        Log.d(TAG, "=== END initInitialIcons() ===")
    }

    private fun updateCurrencyViews() {
        binding.icon.setImageResource(getCurrencyIcon(currentFromCurrency))
        binding.currencyTitle.text = getCurrencyString(currentFromCurrency)

        binding.peopleIcon.setImageResource(getCurrencyIcon(currentToCurrency))
        binding.peopleTitle.text = getCurrencyString(currentToCurrency)
    }

    private fun updateFromPanelViews() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone
        val usdtWallet = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val esomWallet = wallets.find { it.currency == CurrencyEnum.ESOM }
        val somWallet = wallets.find { it.currency == CurrencyEnum.SOM }

        binding.firstUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in fromPanelCurrencies
        binding.firstDigitalBtn.isVisible = CurrencyEnum.ESOM in fromPanelCurrencies
        binding.firstSomBtn.isVisible = CurrencyEnum.SOM in fromPanelCurrencies

        bindPanelRow(
            icon = binding.firstUsdtIcon,
            title = binding.firstUsdtTitle,
            suffix = binding.firstUsdt,
            balance = binding.firstSum1,
            divider = binding.firstUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in fromPanelCurrencies },
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
            currency = CurrencyEnum.ESOM.takeIf { it in fromPanelCurrencies },
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
            currency = CurrencyEnum.SOM.takeIf { it in fromPanelCurrencies },
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

        binding.peopleUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in toPanelCurrencies
        binding.peopleDigitalBtn.isVisible = CurrencyEnum.ESOM in toPanelCurrencies
        binding.peopleSomBtn.isVisible = CurrencyEnum.SOM in toPanelCurrencies

        bindPanelRow(
            icon = binding.peopleUsdtIcon,
            title = binding.peopleUsdtTitle,
            suffix = binding.peopleUsdt,
            balance = binding.peopleSum1,
            divider = binding.peopleUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in toPanelCurrencies },
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
            currency = CurrencyEnum.ESOM.takeIf { it in toPanelCurrencies },
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
            currency = CurrencyEnum.SOM.takeIf { it in toPanelCurrencies },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
    }

    private fun getCurrencyIcon(currency: CurrencyEnum): Int = when (currency) {
        CurrencyEnum.SOM -> R.drawable.som_icon
        CurrencyEnum.ESOM -> R.drawable.salam_icon
        CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
    }

    private fun getCurrencyString(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> getString(R.string.som)
        CurrencyEnum.ESOM -> getString(R.string.digital)
        CurrencyEnum.USDT_TRC20 -> getString(R.string.usdt)
    }

    private fun updateSomIconsVisibility() {
        binding.thirdIconSwap.isVisible = currentFromCurrency == CurrencyEnum.SOM
        binding.somIconSwap.isVisible = currentFromCurrency == CurrencyEnum.SOM
        binding.salamIconSwap.isVisible = currentToCurrency == CurrencyEnum.SOM
        binding.totalSomIcon.isVisible = currentToCurrency == CurrencyEnum.SOM
    }

    private fun updateBalanceDisplay() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return

        val fromWallet = wallets.find { it.currency == currentFromCurrency }
        val fromBalance = fromWallet?.balance ?: 0.0

        binding.sendAvailableTitle.text =
            getString(R.string.available_title, fromBalance.formatBalanceNew())

        updateFromPanelViews()
        updateToPanelViews()
    }

    private fun bindPanelRow(
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

    private fun getAvailableFromBalance(): Double {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return 0.0
        return wallets.find { it.currency == currentFromCurrency }?.balance ?: 0.0
    }

    private fun updateAmountsFromSend(fromAmount: Double) {
        val receivedAmount = calculateReceivedFromSend(fromAmount)

        isUpdatingAmounts = true
        binding.peopleSum.setText(formatInputAmount(receivedAmount, currentToCurrency))
        binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
        isUpdatingAmounts = false

        updateCommissionAndTotal(fromAmount, receivedAmount)
    }

    private fun updateAmountsFromReceive(receivedAmount: Double) {
        val fromAmount = calculateSendFromReceived(receivedAmount)

        isUpdatingAmounts = true
        binding.sum.setText(formatInputAmount(fromAmount, currentFromCurrency))
        binding.sum.setSelection(binding.sum.text?.length ?: 0)
        isUpdatingAmounts = false

        updateCommissionAndTotal(fromAmount, receivedAmount)
    }

    private fun updateCommissionAndTotal(
        fromAmount: Double? = null,
        convertedAmount: Double? = null
    ) {
        val actualTotalAmount = fromAmount ?: parseAmount(binding.sum.text?.toString())
        val actualConvertedAmount = convertedAmount ?: calculateReceivedFromSend(actualTotalAmount)
        val baseAmount = calculateBaseAmountFromTotal(actualTotalAmount)
        val fee = calculateFeePreview(baseAmount)
        val totalAmount = baseAmount + fee

        binding.thirdValue.text = formatCurrencyAmount(fee, currentFromCurrency)
        binding.comissionValue.text = formatCurrencyAmount(totalAmount, currentFromCurrency)
        binding.secondValue.text = formatCurrencyAmount(actualConvertedAmount, currentToCurrency)
        binding.total.text = formatCurrencyAmount(totalAmount, currentFromCurrency)

        Log.d(
            TAG,
            "Конвертация: $totalAmount ${getCurrencyName(currentFromCurrency)} -> " +
                    "$actualConvertedAmount ${getCurrencyName(currentToCurrency)}"
        )
        if (!isSomToEsomConversion()) {
            Log.d(TAG, "Курс обмена: ${getExchangeRate()}")
        }
        Log.d(TAG, "Комиссия: $fee ${getCurrencyName(currentFromCurrency)}")
    }

    private fun calculateReceivedFromSend(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        val baseAmount = calculateBaseAmountFromTotal(fromAmount)
        return convertWithoutFee(baseAmount)
    }

    private fun calculateSendFromReceived(receivedAmount: Double): Double {
        if (receivedAmount <= 0.0) return 0.0
        val baseAmount = invertConvertWithoutFee(receivedAmount)
        val fee = calculateFeePreview(baseAmount)
        return baseAmount + fee
    }

    private fun isSomToEsomConversion(): Boolean {
        return (currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM) ||
                (currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM)
    }

    private fun formatAmount(amount: Double): String {
        return formatCurrencyAmount(amount, currentFromCurrency)
    }

    private fun formatInputAmount(amount: Double, currency: CurrencyEnum): String {
        return if (amount == 0.0) {
            "0"
        } else {
            formatCurrencyAmount(amount, currency)
        }
    }

    private fun parseAmount(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun calculateSomEsomFee(amount: Double): Double {
        return calculateFeePreview(amount)
    }

    private fun calculateFeePreview(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        return model.calculateConvertFee(fromAmount, currentFromCurrency, currentToCurrency)
    }

    private fun calculateBaseAmountFromTotal(totalAmount: Double): Double {
        if (totalAmount <= 0.0) return 0.0

        val feeModel = currentConvertFeeModel() ?: return totalAmount
        val percent = feeModel.percentFee.coerceAtLeast(0.0) / 100.0
        val fixedFee = feeModel.fixedFee.coerceAtLeast(0.0)

        if (percent <= 0.0) {
            return (totalAmount - fixedFee).coerceAtLeast(0.0)
        }

        val fixedThresholdBase = fixedFee / percent
        val fixedRegimeBase = (totalAmount - fixedFee).coerceAtLeast(0.0)
        if (fixedFee > 0.0 && fixedRegimeBase <= fixedThresholdBase) {
            return fixedRegimeBase
        }

        val percentRegimeBase = totalAmount / (1.0 + percent)
        return if (percentRegimeBase > fixedThresholdBase) {
            percentRegimeBase
        } else {
            fixedRegimeBase
        }
    }

    private fun currentConvertFeeModel() =
        model.feeForConvertOperation(currentFromCurrency, currentToCurrency)

    private fun convertWithoutFee(fromAmount: Double): Double {
        val exchangeRate = getExchangeRate()
        if (exchangeRate == 0.0) return 0.0
        return if (currentFromCurrency == CurrencyEnum.SOM || currentFromCurrency == CurrencyEnum.ESOM) {
            fromAmount / exchangeRate
        } else {
            fromAmount * exchangeRate
        }
    }

    private fun invertConvertWithoutFee(grossOut: Double): Double {
        val exchangeRate = getExchangeRate()
        if (exchangeRate == 0.0) return 0.0
        return if (currentFromCurrency == CurrencyEnum.SOM || currentFromCurrency == CurrencyEnum.ESOM) {
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

    private fun getUsdtRateOrNull(): Double? {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return null
        return wallets.firstOrNull { it.currency == CurrencyEnum.USDT_TRC20 }
            ?.buyRate
            ?.takeIf { it > 0.0 }
    }

    private fun getExchangeRate(): Double {
        val usdtRate = getUsdtRateOrNull() ?: getEsomPerUsdOrNull()

        return when {
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> 1.0
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> 1.0
            (currentFromCurrency == CurrencyEnum.ESOM || currentFromCurrency == CurrencyEnum.SOM) &&
                    currentToCurrency == CurrencyEnum.USDT_TRC20 -> usdtRate ?: 1.0
            currentFromCurrency == CurrencyEnum.USDT_TRC20 &&
                    (currentToCurrency == CurrencyEnum.ESOM || currentToCurrency == CurrencyEnum.SOM) -> usdtRate ?: 1.0
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
            ?.find { it.currency == currentFromCurrency }
            ?.balance ?: 0.0

        if (fromAmount > walletBalance) {
            val currencyName = getCurrencyName(currentFromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        Log.d(TAG, "Запуск конвертации:")
        Log.d(TAG, "From: $currentFromCurrency, To: $currentToCurrency, Amount: $fromAmount")
        Log.d(TAG, "Курс: ${getExchangeRate()}")

        showConvertConfirmation(fromAmount)
    }

    private fun showConvertConfirmation(amount: Double) {
        val amountText = "${formatCurrencyAmount(amount, currentFromCurrency)} ${getCurrencyName(currentFromCurrency)}"
        val target = getCurrencyName(currentToCurrency)
        val creditedAmount = calculateReceivedFromSend(amount)
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
                TransferConfirmationFragment.FROM_CURRENCY_KEY to currentFromCurrency.name,
                TransferConfirmationFragment.TO_CURRENCY_KEY to currentToCurrency.name,
                TransferConfirmationFragment.OPERATION_TITLE_KEY to getString(R.string.convertation),
                TransferConfirmationFragment.PAID_FROM_KEY to getAccountForSuccess(currentFromCurrency),
                TransferConfirmationFragment.RECIPIENT_KEY to getAccountForSuccess(currentToCurrency)
            )
        )
        findNavController().navigate(NavGraphDirections.startTransferConfirmationFragment())
    }

    private fun formatCurrencyAmount(amount: Double, currency: CurrencyEnum): String {
        val scale = when (currency) {
            CurrencyEnum.USDT_TRC20 -> 6
            CurrencyEnum.SOM, CurrencyEnum.ESOM -> 2
        }

        return BigDecimal.valueOf(amount)
            .setScale(scale, RoundingMode.DOWN)
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
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.post {
            view.translationY = view.height.toFloat()
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(450)
                .start()
        }
    }

    private fun slideOut(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(450)
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }
}
