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

            val fromCurrency = runCatching {
                CurrencyEnum.valueOf(bundle.getString(TransferConfirmationFragment.FROM_CURRENCY_KEY).orEmpty())
            }.getOrNull() ?: return@setFragmentResultListener
            val toCurrency = runCatching {
                CurrencyEnum.valueOf(bundle.getString(TransferConfirmationFragment.TO_CURRENCY_KEY).orEmpty())
            }.getOrNull() ?: return@setFragmentResultListener
            val amount = bundle.getDouble(TransferConfirmationFragment.AMOUNT_KEY)

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = fromCurrency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.convertation),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty(),
                    receiptNumber = ""
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
            swapFromCurrency(binding.usdtIcon.drawable, binding.usdtTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.firstBitcoinBtn.setOnClickListener {
            swapFromCurrency(binding.bitcoinIcon.drawable, binding.bitcoinTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.firstEthBtn.setOnClickListener {
            swapFromCurrency(binding.ethIcon.drawable, binding.ethTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.firstDigitalBtn.setOnClickListener {
            swapFromCurrency(binding.fiatIcon.drawable, binding.fiatTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.secondUsdtBtn.setOnClickListener {
            swapToCurrency(binding.peopleUsdtIcon.drawable, binding.peopleUsdtTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.secondBitcoinBtn.setOnClickListener {
            swapToCurrency(
                binding.peopleBitcoinIcon.drawable,
                binding.peopleBitcoinTitle.text.toString()
            )
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.secondEthBtn.setOnClickListener {
            swapToCurrency(binding.peopleEthIcon.drawable, binding.peopleEthTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
            closeAllPanels()
        }

        binding.secondDigitalBtn.setOnClickListener {
            swapToCurrency(binding.peopleFiatIcon.drawable, binding.peopleFiatTitle.text.toString())
            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateAmountsFromSend(parseAmount(binding.sum.text?.toString()))
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
                        "Settings loaded: esom_som_conversion_fee_pct=${state.data.esomSomConversionFeePct}"
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
    }

    private fun swapFromCurrency(drawable: android.graphics.drawable.Drawable, title: String) {
        val tempIcon = binding.icon.drawable
        val tempTitle = binding.currencyTitle.text.toString()

        binding.icon.setImageDrawable(drawable)
        binding.currencyTitle.text = title

        when (title) {
            binding.usdtTitle.text.toString() -> {
                binding.usdtIcon.setImageDrawable(tempIcon)
                binding.usdtTitle.text = tempTitle
            }

            binding.bitcoinTitle.text.toString() -> {
                binding.bitcoinIcon.setImageDrawable(tempIcon)
                binding.bitcoinTitle.text = tempTitle
            }

            binding.ethTitle.text.toString() -> {
                binding.ethIcon.setImageDrawable(tempIcon)
                binding.ethTitle.text = tempTitle
            }

            binding.fiatTitle.text.toString() -> {
                binding.fiatIcon.setImageDrawable(tempIcon)
                binding.fiatTitle.text = tempTitle
            }
        }
    }

    private fun swapToCurrency(drawable: android.graphics.drawable.Drawable, title: String) {
        val tempIcon = binding.peopleIcon.drawable
        val tempTitle = binding.peopleTitle.text.toString()

        binding.peopleIcon.setImageDrawable(drawable)
        binding.peopleTitle.text = title

        when (title) {
            binding.peopleUsdtTitle.text.toString() -> {
                binding.peopleUsdtIcon.setImageDrawable(tempIcon)
                binding.peopleUsdtTitle.text = tempTitle
            }

            binding.peopleBitcoinTitle.text.toString() -> {
                binding.peopleBitcoinIcon.setImageDrawable(tempIcon)
                binding.peopleBitcoinTitle.text = tempTitle
            }

            binding.peopleEthTitle.text.toString() -> {
                binding.peopleEthIcon.setImageDrawable(tempIcon)
                binding.peopleEthTitle.text = tempTitle
            }

            binding.peopleFiatTitle.text.toString() -> {
                binding.peopleFiatIcon.setImageDrawable(tempIcon)
                binding.peopleFiatTitle.text = tempTitle
            }
        }
    }

    private fun updateCurrentCurrencies() {
        currentFromCurrency = when (binding.currencyTitle.text.toString()) {
            getString(R.string.som) -> CurrencyEnum.SOM
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            else -> CurrencyEnum.SOM
        }

        currentToCurrency = when (binding.peopleTitle.text.toString()) {
            getString(R.string.som) -> CurrencyEnum.SOM
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            else -> CurrencyEnum.ESOM
        }

        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()
    }

    private fun updateCommissionTitles() {
        binding.comissionTitle.text = if (isSomToEsomConversion()) {
            getCurrencyName(currentFromCurrency)
        } else {
            getCurrencyName(currentToCurrency)
        }
        binding.secondTitle.text = getCurrencyName(currentToCurrency)
    }

    private fun initInitialIcons() {
        currentFromCurrency = args.from
        currentToCurrency = args.to

        Log.d(TAG, "=== initInitialIcons() ===")
        Log.d(TAG, "currentFromCurrency: $currentFromCurrency")
        Log.d(TAG, "currentToCurrency: $currentToCurrency")

        binding.icon.setImageResource(getCurrencyIcon(currentFromCurrency))
        binding.currencyTitle.text = getCurrencyString(currentFromCurrency)

        binding.peopleIcon.setImageResource(getCurrencyIcon(currentToCurrency))
        binding.peopleTitle.text = getCurrencyString(currentToCurrency)

        val allCurrencies = CurrencyEnum.values().toList()

        val firstPanelCurrencies = allCurrencies
            .filter { it != currentFromCurrency }
            .take(4)

        val baseSecondList = allCurrencies
            .filter { it != currentToCurrency }

        val secondPanelCurrencies = if (currentToCurrency == CurrencyEnum.ESOM) {
            val modifiedList = baseSecondList
                .filter { it != CurrencyEnum.ESOM }
                .toMutableList()

            if (!modifiedList.contains(CurrencyEnum.USDT_TRC20)) {
                modifiedList.add(0, CurrencyEnum.USDT_TRC20)
            }

            modifiedList.take(4)
        } else {
            baseSecondList.take(4)
        }

        binding.usdtIcon.setImageResource(getCurrencyIcon(firstPanelCurrencies[0]))
        binding.usdtTitle.text = getCurrencyString(firstPanelCurrencies[0])

        binding.bitcoinIcon.setImageResource(getCurrencyIcon(firstPanelCurrencies[1]))
        binding.bitcoinTitle.text = getCurrencyString(firstPanelCurrencies[1])

        binding.ethIcon.setImageResource(getCurrencyIcon(firstPanelCurrencies[2]))
        binding.ethTitle.text = getCurrencyString(firstPanelCurrencies[2])

        binding.fiatIcon.setImageResource(getCurrencyIcon(firstPanelCurrencies[3]))
        binding.fiatTitle.text = getCurrencyString(firstPanelCurrencies[3])

        binding.peopleUsdtIcon.setImageResource(getCurrencyIcon(secondPanelCurrencies[0]))
        binding.peopleUsdtTitle.text = getCurrencyString(secondPanelCurrencies[0])

        binding.peopleBitcoinIcon.setImageResource(getCurrencyIcon(secondPanelCurrencies[1]))
        binding.peopleBitcoinTitle.text = getCurrencyString(secondPanelCurrencies[1])

        binding.peopleEthIcon.setImageResource(getCurrencyIcon(secondPanelCurrencies[2]))
        binding.peopleEthTitle.text = getCurrencyString(secondPanelCurrencies[2])

        binding.peopleFiatIcon.setImageResource(getCurrencyIcon(secondPanelCurrencies[3]))
        binding.peopleFiatTitle.text = getCurrencyString(secondPanelCurrencies[3])

        updateBalanceDisplay()
        updateSomIconsVisibility()
        updateCommissionTitles()

        Log.d(TAG, "=== END initInitialIcons() ===")
    }

    private fun getCurrencyIcon(currency: CurrencyEnum): Int = when (currency) {
        CurrencyEnum.SOM -> R.drawable.som_icon
        CurrencyEnum.ESOM -> R.drawable.salam_icon
        CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
        CurrencyEnum.BTC -> R.drawable.bitcoin_icon
        CurrencyEnum.ETH -> R.drawable.eth_icon
    }

    private fun getCurrencyString(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> getString(R.string.som)
        CurrencyEnum.ESOM -> getString(R.string.digital)
        CurrencyEnum.USDT_TRC20 -> getString(R.string.usdt)
        CurrencyEnum.BTC -> getString(R.string.bitcoin)
        CurrencyEnum.ETH -> getString(R.string.ethereum)
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

        val firstUsdtCurrency = when (binding.usdtTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.USDT_TRC20
        }

        val firstBitcoinCurrency = when (binding.bitcoinTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.BTC
        }

        val firstEthCurrency = when (binding.ethTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.ETH
        }

        val firstDigitalCurrency = when (binding.fiatTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.ESOM
        }

        val secondUsdtCurrency = when (binding.peopleUsdtTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.USDT_TRC20
        }

        val secondBitcoinCurrency = when (binding.peopleBitcoinTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.BTC
        }

        val secondEthCurrency = when (binding.peopleEthTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.ETH
        }

        val secondDigitalCurrency = when (binding.peopleFiatTitle.text.toString()) {
            getString(R.string.usdt) -> CurrencyEnum.USDT_TRC20
            getString(R.string.bitcoin) -> CurrencyEnum.BTC
            getString(R.string.ethereum) -> CurrencyEnum.ETH
            getString(R.string.digital) -> CurrencyEnum.ESOM
            getString(R.string.som) -> CurrencyEnum.SOM
            else -> CurrencyEnum.SOM
        }

        binding.sum1.text =
            (wallets.find { it.currency == firstUsdtCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.sum2.text =
            (wallets.find { it.currency == firstBitcoinCurrency }?.balance
                ?: 0.0).formatBalanceNew()
        binding.sum3.text =
            (wallets.find { it.currency == firstEthCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.sum4.text =
            (wallets.find { it.currency == firstDigitalCurrency }?.balance
                ?: 0.0).formatBalanceNew()

        binding.peopleSum1.text =
            (wallets.find { it.currency == secondUsdtCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum2.text =
            (wallets.find { it.currency == secondBitcoinCurrency }?.balance
                ?: 0.0).formatBalanceNew()
        binding.peopleSum3.text =
            (wallets.find { it.currency == secondEthCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum4.text =
            (wallets.find { it.currency == secondDigitalCurrency }?.balance
                ?: 0.0).formatBalanceNew()

        val firstUsdtWallet = wallets.find { it.currency == firstUsdtCurrency }
        val firstBitcoinWallet = wallets.find { it.currency == firstBitcoinCurrency }
        val firstEthWallet = wallets.find { it.currency == firstEthCurrency }
        val firstDigitalWallet = wallets.find { it.currency == firstDigitalCurrency }

        val secondUsdtWallet = wallets.find { it.currency == secondUsdtCurrency }
        val secondBitcoinWallet = wallets.find { it.currency == secondBitcoinCurrency }
        val secondEthWallet = wallets.find { it.currency == secondEthCurrency }
        val secondDigitalWallet = wallets.find { it.currency == secondDigitalCurrency }

        binding.usdt.text = "*${firstUsdtWallet?.address?.takeLast(3) ?: ""}"
        binding.bitcoin.text = "*${firstBitcoinWallet?.address?.takeLast(3) ?: ""}"
        binding.eth.text = "*${firstEthWallet?.address?.takeLast(3) ?: ""}"
        binding.fiat.text = "*${firstDigitalWallet?.address?.takeLast(3) ?: ""}"

        binding.peopleUsdt.text = "*${secondUsdtWallet?.address?.takeLast(3) ?: ""}"
        binding.peopleBitcoin.text = "*${secondBitcoinWallet?.address?.takeLast(3) ?: ""}"
        binding.peopleEth.text = "*${secondEthWallet?.address?.takeLast(3) ?: ""}"
        binding.peopleFiat.text = "*${secondDigitalWallet?.address?.takeLast(3) ?: ""}"
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
        binding.comissionValue.text = formatAmount(actualFromAmount)
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
        if (isSomToEsomConversion()) {
            val fee = calculateSomEsomFee(fromAmount)
            return (fromAmount - fee).coerceAtLeast(0.0)
        }

        val grossOut = convertWithoutFee(fromAmount)
        val feePct = getTradeFeePercentForPair(currentFromCurrency, currentToCurrency)
        val feeOut = grossOut * (feePct / 100.0)
        return (grossOut - feeOut).coerceAtLeast(0.0)
    }

    private fun calculateSendFromReceived(receivedAmount: Double): Double {
        if (receivedAmount <= 0.0) return 0.0

        return if (isSomToEsomConversion()) {
            val settings = (model.settings.value as? UiState.Success)?.data ?: return 0.0
            val feePercent = settings.esomSomConversionFeePct.coerceAtLeast(0.0)
            val minFee = settings.esomSomConversionFeeMin.coerceAtLeast(0.0)
            val p = feePercent / 100.0

            val byPercent = if (p < 1.0) receivedAmount / (1.0 - p) else Double.POSITIVE_INFINITY
            val feeByPercent = byPercent * p
            if (byPercent.isFinite() && feeByPercent >= minFee) {
                byPercent
            } else {
                receivedAmount + minFee
            }
        } else {
            val feePct = getTradeFeePercentForPair(currentFromCurrency, currentToCurrency)
            val multiplier = 1.0 - feePct / 100.0
            if (multiplier <= 0.0) return 0.0
            val grossOut = receivedAmount / multiplier
            invertConvertWithoutFee(grossOut)
        }
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
        val settingsState = model.settings.value
        if (settingsState !is UiState.Success) {
            Log.w(TAG, "calculateFee: settings are not ready, returning 0. amount=$amount")
            return 0.0
        }
        val settings = settingsState.data
        val feePercent = settings.esomSomConversionFeePct.coerceAtLeast(0.0)
        val minFee = settings.esomSomConversionFeeMin.coerceAtLeast(0.0)
        val feeByPct = amount * (feePercent / 100.0)
        return maxOf(feeByPct, minFee)
    }

    private fun calculateFeePreview(fromAmount: Double): Double {
        if (fromAmount <= 0.0) return 0.0
        return if (isSomToEsomConversion()) {
            calculateSomEsomFee(fromAmount)
        } else {
            val grossOut = convertWithoutFee(fromAmount)
            val feePct = getTradeFeePercentForPair(currentFromCurrency, currentToCurrency)
            grossOut * (feePct / 100.0)
        }
    }

    private fun getTradeFeePercentForPair(from: CurrencyEnum, to: CurrencyEnum): Double {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return 0.0
        fun feeForAsset(asset: CurrencyEnum): Double = when (asset) {
            CurrencyEnum.BTC -> settings.btcTradeFeePct
            CurrencyEnum.ETH -> settings.ethTradeFeePct
            CurrencyEnum.USDT_TRC20 -> settings.usdtTradeFeePct
            else -> 0.0
        }
        return when {
            from == CurrencyEnum.ESOM || from == CurrencyEnum.SOM -> feeForAsset(to)
            to == CurrencyEnum.ESOM || to == CurrencyEnum.SOM -> feeForAsset(from)
            else -> maxOf(feeForAsset(from), feeForAsset(to))
        }
    }

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
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return 1.0
        val esomPerUsd = getEsomPerUsdOrNull()

        return when {
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> 1.0
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> 1.0
            (currentFromCurrency == CurrencyEnum.ESOM || currentFromCurrency == CurrencyEnum.SOM) &&
                    currentToCurrency == CurrencyEnum.USDT_TRC20 -> esomPerUsd ?: 1.0
            currentFromCurrency == CurrencyEnum.USDT_TRC20 &&
                    (currentToCurrency == CurrencyEnum.ESOM || currentToCurrency == CurrencyEnum.SOM) -> esomPerUsd ?: 1.0

            (currentFromCurrency == CurrencyEnum.ESOM || currentFromCurrency == CurrencyEnum.SOM) &&
                    currentToCurrency != CurrencyEnum.ESOM && currentToCurrency != CurrencyEnum.SOM -> {
                val toWallet = wallets.find { it.currency == currentToCurrency }
                toWallet?.sellRate ?: 1.0
            }

            currentToCurrency == CurrencyEnum.SOM -> {
                val fromWallet = wallets.find { it.currency == currentFromCurrency }
                fromWallet?.sellRate ?: 1.0
            }

            currentFromCurrency == CurrencyEnum.SOM -> {
                val toWallet = wallets.find { it.currency == currentToCurrency }
                toWallet?.buyRate ?: 1.0
            }

            else -> {
                val fromWallet = wallets.find { it.currency == currentFromCurrency }
                val toWallet = wallets.find { it.currency == currentToCurrency }

                if (fromWallet != null && toWallet != null) {
                    val somAmount = 1.0 * fromWallet.sellRate
                    somAmount / toWallet.buyRate
                } else {
                    1.0
                }
            }
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

    private fun getCurrencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
        CurrencyEnum.BTC -> "BTC"
        CurrencyEnum.ETH -> "ETH"
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
