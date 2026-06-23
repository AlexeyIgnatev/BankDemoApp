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
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import dagger.hilt.android.AndroidEntryPoint

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
            binding.sum.setText(formatInputAmount(allAvailable))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            updateAmountsFromSend(allAvailable)
        }

        binding.peopleSumAllLayout.setOnClickListener {
            val allAvailable = getAvailableFromBalance()
            val maxReceived = calculateReceivedFromSend(allAvailable)

            isUpdatingAmounts = true
            binding.sum.setText(formatInputAmount(allAvailable))
            binding.sum.setSelection(binding.sum.text?.length ?: 0)
            binding.peopleSum.setText(formatInputAmount(maxReceived))
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
            selectFromCurrency(fromPanelCurrencies.getOrNull(0) ?: return@setOnClickListener)
            closeAllPanels()
        }

        binding.firstBitcoinBtn.setOnClickListener {
            selectFromCurrency(fromPanelCurrencies.getOrNull(1) ?: return@setOnClickListener)
            closeAllPanels()
        }

        binding.secondUsdtBtn.setOnClickListener {
            selectToCurrency(toPanelCurrencies.getOrNull(0) ?: return@setOnClickListener)
            closeAllPanels()
        }

        binding.secondBitcoinBtn.setOnClickListener {
            selectToCurrency(toPanelCurrencies.getOrNull(1) ?: return@setOnClickListener)
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
        binding.usdtIcon.setImageResource(getCurrencyIcon(fromPanelCurrencies.getOrNull(0) ?: CurrencyEnum.SOM))
        binding.usdtTitle.text = getCurrencyString(fromPanelCurrencies.getOrNull(0) ?: CurrencyEnum.SOM)
        binding.usdt.visibility = View.VISIBLE
        binding.usdtView.visibility = View.VISIBLE

        binding.bitcoinIcon.setImageResource(getCurrencyIcon(fromPanelCurrencies.getOrNull(1) ?: CurrencyEnum.ESOM))
        binding.bitcoinTitle.text = getCurrencyString(fromPanelCurrencies.getOrNull(1) ?: CurrencyEnum.ESOM)
        binding.bitcoinIcon.visibility = View.VISIBLE
        binding.bitcoinTitle.visibility = View.VISIBLE
        binding.bitcoin.visibility = View.VISIBLE
        binding.bitcoinView.visibility = View.VISIBLE

        binding.ethIcon.visibility = View.GONE
        binding.ethTitle.visibility = View.GONE
        binding.eth.visibility = View.GONE
        binding.ethView.visibility = View.GONE
        binding.firstEthBtn.visibility = View.GONE
        binding.firstDigitalBtn.visibility = View.GONE

        binding.fiatIcon.visibility = View.GONE
        binding.fiatTitle.visibility = View.GONE
        binding.fiat.visibility = View.GONE
    }

    private fun updateToPanelViews() {
        binding.peopleUsdtIcon.setImageResource(getCurrencyIcon(toPanelCurrencies.getOrNull(0) ?: CurrencyEnum.SOM))
        binding.peopleUsdtTitle.text = getCurrencyString(toPanelCurrencies.getOrNull(0) ?: CurrencyEnum.SOM)
        binding.peopleUsdtIcon.visibility = View.VISIBLE
        binding.peopleUsdtTitle.visibility = View.VISIBLE
        binding.peopleUsdt.visibility = View.VISIBLE
        binding.peopleUsdtView.visibility = View.VISIBLE

        binding.peopleBitcoinIcon.setImageResource(getCurrencyIcon(toPanelCurrencies.getOrNull(1) ?: CurrencyEnum.ESOM))
        binding.peopleBitcoinTitle.text = getCurrencyString(toPanelCurrencies.getOrNull(1) ?: CurrencyEnum.ESOM)
        binding.peopleBitcoinIcon.visibility = View.VISIBLE
        binding.peopleBitcoinTitle.visibility = View.VISIBLE
        binding.peopleBitcoin.visibility = View.VISIBLE
        binding.peopleBitcoinView.visibility = View.VISIBLE

        binding.peopleEthIcon.visibility = View.GONE
        binding.peopleEthTitle.visibility = View.GONE
        binding.peopleEth.visibility = View.GONE
        binding.peopleEthView.visibility = View.GONE
        binding.secondEthBtn.visibility = View.GONE
        binding.secondDigitalBtn.visibility = View.GONE

        binding.peopleFiatIcon.visibility = View.GONE
        binding.peopleFiatTitle.visibility = View.GONE
        binding.peopleFiat.visibility = View.GONE
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

        val firstCurrency = fromPanelCurrencies.getOrElse(0) { CurrencyEnum.USDT_TRC20 }
        val secondCurrency = fromPanelCurrencies.getOrElse(1) { CurrencyEnum.ESOM }

        val firstTargetCurrency = toPanelCurrencies.getOrElse(0) { CurrencyEnum.USDT_TRC20 }
        val secondTargetCurrency = toPanelCurrencies.getOrElse(1) { CurrencyEnum.SOM }

        binding.sum1.text =
            (wallets.find { it.currency == firstCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.sum2.text =
            (wallets.find { it.currency == secondCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.sum3.text = "0"
        binding.sum4.text = "0"

        binding.peopleSum1.text =
            (wallets.find { it.currency == firstTargetCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum2.text =
            (wallets.find { it.currency == secondTargetCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum3.text = "0"
        binding.peopleSum4.text = "0"

        val firstWallet = wallets.find { it.currency == firstCurrency }
        val secondWallet = wallets.find { it.currency == secondCurrency }
        val firstTargetWallet = wallets.find { it.currency == firstTargetCurrency }
        val secondTargetWallet = wallets.find { it.currency == secondTargetCurrency }

        binding.usdt.text = "*${firstWallet?.address?.takeLast(3) ?: ""}"
        binding.bitcoin.text = "*${secondWallet?.address?.takeLast(3) ?: ""}"
        binding.eth.text = ""
        binding.fiat.text = ""

        binding.peopleUsdt.text = "*${firstTargetWallet?.address?.takeLast(3) ?: ""}"
        binding.peopleBitcoin.text = "*${secondTargetWallet?.address?.takeLast(3) ?: ""}"
        binding.peopleEth.text = ""
        binding.peopleFiat.text = ""
    }

    private fun getAvailableFromBalance(): Double {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return 0.0
        return wallets.find { it.currency == currentFromCurrency }?.balance ?: 0.0
    }

    private fun updateAmountsFromSend(fromAmount: Double) {
        val receivedAmount = calculateReceivedFromSend(fromAmount)

        isUpdatingAmounts = true
        binding.peopleSum.setText(formatInputAmount(receivedAmount))
        binding.peopleSum.setSelection(binding.peopleSum.text?.length ?: 0)
        isUpdatingAmounts = false

        updateCommissionAndTotal(fromAmount, receivedAmount)
    }

    private fun updateAmountsFromReceive(receivedAmount: Double) {
        val fromAmount = calculateSendFromReceived(receivedAmount)

        isUpdatingAmounts = true
        binding.sum.setText(formatInputAmount(fromAmount))
        binding.sum.setSelection(binding.sum.text?.length ?: 0)
        isUpdatingAmounts = false

        updateCommissionAndTotal(fromAmount, receivedAmount)
    }

    private fun updateCommissionAndTotal(
        fromAmount: Double? = null,
        convertedAmount: Double? = null
    ) {
        val actualFromAmount = fromAmount ?: parseAmount(binding.sum.text?.toString())
        val actualConvertedAmount = convertedAmount ?: calculateReceivedFromSend(actualFromAmount)
        val fee = calculateFeePreview(actualFromAmount)

        binding.thirdValue.text = formatAmount(fee)
        binding.comissionValue.text = actualFromAmount.toString()
        binding.secondValue.text = formatAmount(actualConvertedAmount)
        binding.total.text = formatAmount(actualConvertedAmount)

        Log.d(
            TAG,
            "Конвертация: $actualFromAmount ${getCurrencyName(currentFromCurrency)} -> " +
                    "$actualConvertedAmount ${getCurrencyName(currentToCurrency)}"
        )
        if (!isSomToEsomConversion()) {
            Log.d(TAG, "Курс обмена: ${getExchangeRate()}")
        }
        Log.d(TAG, "Комиссия: $fee ${getCurrencyName(currentFromCurrency)}")
    }

    private fun calculateReceivedFromSend(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        val fee = calculateFeePreview(fromAmount)
        val netSourceAmount = (fromAmount - fee).coerceAtLeast(0.0)
        return convertWithoutFee(netSourceAmount)
    }

    private fun calculateSendFromReceived(receivedAmount: Double): Double {
        if (receivedAmount <= 0.0) return 0.0
        val netSourceAmount = invertConvertWithoutFee(receivedAmount)
        val feeModel = currentConvertFeeModel() ?: return netSourceAmount
        val percent = feeModel.percentFee.coerceAtLeast(0.0) / 100.0
        if (percent >= 1.0) return 0.0
        return (netSourceAmount + feeModel.fixedFee.coerceAtLeast(0.0)) / (1.0 - percent)
    }

    private fun isSomToEsomConversion(): Boolean {
        return (currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM) ||
                (currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM)
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.formatBalanceNew()
        }
    }

    private fun formatInputAmount(amount: Double): String {
        return if (amount == 0.0) {
            "0"
        } else {
            formatAmount(amount)
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

    private fun getExchangeRate(): Double {
        val esomPerUsd = getEsomPerUsdOrNull()

        return when {
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> 1.0
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> 1.0
            (currentFromCurrency == CurrencyEnum.ESOM || currentFromCurrency == CurrencyEnum.SOM) &&
                    currentToCurrency == CurrencyEnum.USDT_TRC20 -> esomPerUsd ?: 1.0
            currentFromCurrency == CurrencyEnum.USDT_TRC20 &&
                    (currentToCurrency == CurrencyEnum.ESOM || currentToCurrency == CurrencyEnum.SOM) -> esomPerUsd ?: 1.0
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
        val amountText = "${amount.formatBalanceNew()} ${getCurrencyName(currentFromCurrency)}"
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
