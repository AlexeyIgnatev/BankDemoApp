package com.esom.bank.screens.swap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.esom.bank.screens.main.enums.CurrencyEnum
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

    companion object {
        private const val TAG = "SwapFragment"
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

        initInitialIcons()
        setupQuickAmounts()
        setupClickListeners()

        updateBalanceDisplay()
        updateCommissionAndTotal()
    }

    private fun setupQuickAmounts() {
        listOf(
            binding.sum50Layout to "50",
            binding.sum100Layout to "100",
            binding.sum1000Layout to "1000",
            binding.sum10000Layout to "10000"
        ).forEach { (layout, value) ->
            layout.setOnClickListener {
                binding.sumInput.setText(value)
                updateCommissionAndTotal()
            }
        }
    }

    private fun setupClickListeners() {
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }

        binding.currentCurrencyLayout.setOnClickListener {
            if (isPanelShown) {
                binding.peopleLayout.isClickable = true
                slideOut(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.GONE
            } else {
                binding.peopleLayout.isClickable = false
                binding.typeCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.VISIBLE
            }
            isPanelShown = !isPanelShown
        }

        binding.peopleLayout.setOnClickListener {
            if (isPeoplePanelShown) {
                binding.currentCurrencyLayout.isClickable = true
                it.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            } else {
                binding.currentCurrencyLayout.isClickable = false
                it.elevation = 20f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            isPeoplePanelShown = !isPeoplePanelShown
        }

        fun closeAllPanels() {
            if (isPanelShown) {
                binding.peopleLayout.isClickable = true
                slideOut(binding.typeCurrencyLayout)
                binding.backgroundConversationLayout.visibility = View.GONE
                isPanelShown = false
            }
            if (isPeoplePanelShown) {
                binding.currentCurrencyLayout.isClickable = true
                binding.peopleLayout.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
                isPeoplePanelShown = false
            }
        }

        binding.backgroundConversationLayout.setOnClickListener {
            closeAllPanels()
        }

        binding.firstUsdtBtn.setOnClickListener {
            val tempIcon = binding.icon.drawable
            val tempTitle = binding.currencyTitle.text.toString()

            binding.icon.setImageDrawable(binding.usdtIcon.drawable)
            binding.currencyTitle.text = binding.usdtTitle.text.toString()

            binding.usdtIcon.setImageDrawable(tempIcon)
            binding.usdtTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.firstBitcoinBtn.setOnClickListener {
            val tempIcon = binding.icon.drawable
            val tempTitle = binding.currencyTitle.text.toString()

            binding.icon.setImageDrawable(binding.bitcoinIcon.drawable)
            binding.currencyTitle.text = binding.bitcoinTitle.text.toString()

            binding.bitcoinIcon.setImageDrawable(tempIcon)
            binding.bitcoinTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.firstEthBtn.setOnClickListener {
            val tempIcon = binding.icon.drawable
            val tempTitle = binding.currencyTitle.text.toString()

            binding.icon.setImageDrawable(binding.ethIcon.drawable)
            binding.currencyTitle.text = binding.ethTitle.text.toString()

            binding.ethIcon.setImageDrawable(tempIcon)
            binding.ethTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.firstDigitalBtn.setOnClickListener {
            val tempIcon = binding.icon.drawable
            val tempTitle = binding.currencyTitle.text.toString()

            binding.icon.setImageDrawable(binding.fiatIcon.drawable)
            binding.currencyTitle.text = binding.fiatTitle.text.toString()

            binding.fiatIcon.setImageDrawable(tempIcon)
            binding.fiatTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.secondUsdtBtn.setOnClickListener {
            val tempIcon = binding.peopleIcon.drawable
            val tempTitle = binding.peopleTitle.text.toString()

            binding.peopleIcon.setImageDrawable(binding.peopleUsdtIcon.drawable)
            binding.peopleTitle.text = binding.peopleUsdtTitle.text.toString()

            binding.peopleUsdtIcon.setImageDrawable(tempIcon)
            binding.peopleUsdtTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.secondBitcoinBtn.setOnClickListener {
            val tempIcon = binding.peopleIcon.drawable
            val tempTitle = binding.peopleTitle.text.toString()

            binding.peopleIcon.setImageDrawable(binding.peopleBitcoinIcon.drawable)
            binding.peopleTitle.text = binding.peopleBitcoinTitle.text.toString()

            binding.peopleBitcoinIcon.setImageDrawable(tempIcon)
            binding.peopleBitcoinTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.secondEthBtn.setOnClickListener {
            val tempIcon = binding.peopleIcon.drawable
            val tempTitle = binding.peopleTitle.text.toString()

            binding.peopleIcon.setImageDrawable(binding.peopleEthIcon.drawable)
            binding.peopleTitle.text = binding.peopleEthTitle.text.toString()

            binding.peopleEthIcon.setImageDrawable(tempIcon)
            binding.peopleEthTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.secondDigitalBtn.setOnClickListener {
            val tempIcon = binding.peopleIcon.drawable
            val tempTitle = binding.peopleTitle.text.toString()

            binding.peopleIcon.setImageDrawable(binding.peopleFiatIcon.drawable)
            binding.peopleTitle.text = binding.peopleFiatTitle.text.toString()

            binding.peopleFiatIcon.setImageDrawable(tempIcon)
            binding.peopleFiatTitle.text = tempTitle

            updateCurrentCurrencies()
            updateBalanceDisplay()
            updateCommissionAndTotal()
            closeAllPanels()
        }

        binding.sumInput.setOnUserTextChangeListener {
            updateCommissionAndTotal()
        }

        binding.sendBtn.setOnClickListener { handleConvertButtonClick() }
        model.myData.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is UiState.Success -> {
                    updateBalanceDisplay()
                    updateCommissionAndTotal()
                }

                else -> {}
            }
        }
        model.swapRes.observe(viewLifecycleOwner) {
            binding.sendText.isVisible = it !is UiState.Loading
            binding.indicator.isVisible = it is UiState.Loading

            when (it) {
                is UiState.Error -> findNavController().navigate(
                    NavGraphDirections.startFailTransferFragment(it.message)
                )

                is UiState.Success -> {
                    model.updateUserData()
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                }

                else -> {}
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
        binding.comissionTitle.text = getCurrencyName(currentFromCurrency)
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
        Log.d(TAG, "allCurrencies: $allCurrencies")

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

        Log.d(TAG, "firstPanelCurrencies (main): $firstPanelCurrencies")
        Log.d(TAG, "secondPanelCurrencies (people): $secondPanelCurrencies")

        val first = firstPanelCurrencies
        val second = secondPanelCurrencies

        binding.usdtIcon.setImageResource(getCurrencyIcon(first[0]))
        binding.usdtTitle.text = getCurrencyString(first[0])

        binding.bitcoinIcon.setImageResource(getCurrencyIcon(first[1]))
        binding.bitcoinTitle.text = getCurrencyString(first[1])

        binding.ethIcon.setImageResource(getCurrencyIcon(first[2]))
        binding.ethTitle.text = getCurrencyString(first[2])

        binding.fiatIcon.setImageResource(getCurrencyIcon(first[3]))
        binding.fiatTitle.text = getCurrencyString(first[3])

        binding.peopleUsdtIcon.setImageResource(getCurrencyIcon(second[0]))
        binding.peopleUsdtTitle.text = getCurrencyString(second[0])

        binding.peopleBitcoinIcon.setImageResource(getCurrencyIcon(second[1]))
        binding.peopleBitcoinTitle.text = getCurrencyString(second[1])

        binding.peopleEthIcon.setImageResource(getCurrencyIcon(second[2]))
        binding.peopleEthTitle.text = getCurrencyString(second[2])

        binding.peopleFiatIcon.setImageResource(getCurrencyIcon(second[3]))
        binding.peopleFiatTitle.text = getCurrencyString(second[3])

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
        binding.somIcon.isVisible = currentFromCurrency == CurrencyEnum.SOM
        binding.peopleSomIcon.isVisible = currentToCurrency == CurrencyEnum.SOM

        val showFromSomIcons = currentFromCurrency == CurrencyEnum.SOM
        binding.somIcon50.isVisible = showFromSomIcons
        binding.somIcon100.isVisible = showFromSomIcons
        binding.somIcon1000.isVisible = showFromSomIcons
        binding.somIcon10000.isVisible = showFromSomIcons

        binding.thirdIconSwap.isVisible = currentFromCurrency == CurrencyEnum.SOM
        binding.somIconSwap.isVisible = currentFromCurrency == CurrencyEnum.SOM
        binding.salamIconSwap.isVisible = currentToCurrency == CurrencyEnum.SOM
        binding.totalSomIcon.isVisible = currentToCurrency == CurrencyEnum.SOM
    }

    private fun updateBalanceDisplay() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return

        val fromWallet = wallets.find { it.currency == currentFromCurrency }
        val fromBalance = fromWallet?.balance ?: 0.0
        binding.sum.text = fromBalance.formatBalanceNew()

        val toWallet = wallets.find { it.currency == currentToCurrency }
        val toBalance = toWallet?.balance ?: 0.0
        binding.peopleSum.text = toBalance.formatBalanceNew()

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
        binding.sum2.text = (wallets.find { it.currency == firstBitcoinCurrency }?.balance
            ?: 0.0).formatBalanceNew()
        binding.sum3.text =
            (wallets.find { it.currency == firstEthCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.sum4.text = (wallets.find { it.currency == firstDigitalCurrency }?.balance
            ?: 0.0).formatBalanceNew()

        binding.peopleSum1.text =
            (wallets.find { it.currency == secondUsdtCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum2.text = (wallets.find { it.currency == secondBitcoinCurrency }?.balance
            ?: 0.0).formatBalanceNew()
        binding.peopleSum3.text =
            (wallets.find { it.currency == secondEthCurrency }?.balance ?: 0.0).formatBalanceNew()
        binding.peopleSum4.text = (wallets.find { it.currency == secondDigitalCurrency }?.balance
            ?: 0.0).formatBalanceNew()

        val fromSuffix = fromWallet?.address?.takeLast(3) ?: ""
        val toSuffix = toWallet?.address?.takeLast(3) ?: ""

        binding.currency.text = "*$fromSuffix"
        binding.contact.text = "*$toSuffix"

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

    private fun updateCommissionAndTotal() {
        val fromAmount = binding.sumInput.text.toString().toDoubleOrNull() ?: 0.0
        val fee = calculateFee(fromAmount)

        val convertedAmount = if (isSomToEsomConversion()) {
            fromAmount - fee
        } else {
            val exchangeRate = getExchangeRate()
            if(currentFromCurrency == CurrencyEnum.SOM || currentFromCurrency == CurrencyEnum.ESOM) {
                (fromAmount - fee) / exchangeRate
            } else {
                (fromAmount - fee) * exchangeRate
            }
        }

        binding.thirdValue.text = formatAmount(fee)
        binding.comissionValue.text = formatAmount(fromAmount)
        binding.secondValue.text = formatAmount(convertedAmount)
        binding.total.text = formatAmount(convertedAmount)

        Log.d(
            TAG,
            "Конвертация: $fromAmount ${getCurrencyName(currentFromCurrency)} -> $convertedAmount ${
                getCurrencyName(currentToCurrency)
            }"
        )
        if (!isSomToEsomConversion()) {
            Log.d(TAG, "Курс обмена: ${getExchangeRate()}")
        }
        Log.d(TAG, "Комиссия: $fee ${getCurrencyName(currentFromCurrency)}")
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

    private fun calculateFee(amount: Double): Double {
        val settings = (model.settings.value as? UiState.Success)?.data ?: return 0.0
        return when {
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM ->
                amount * (settings.esomSomConversionFeePct / 100.0)

            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM ->
                amount * (settings.esomSomConversionFeePct / 100.0)

            else -> 0.0
        }
    }

    private fun getExchangeRate(): Double {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return 1.0

        return when {
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> 1.0
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> 1.0

            (currentFromCurrency == CurrencyEnum.ESOM || currentFromCurrency == CurrencyEnum.SOM)
                    && currentToCurrency != CurrencyEnum.ESOM && currentToCurrency != CurrencyEnum.SOM -> {
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
        val fromAmount = binding.sumInput.text.toString().toDoubleOrNull() ?: run {
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

        model.convert(currentFromCurrency, currentToCurrency, fromAmount)
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