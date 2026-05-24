package com.esom.bank.screens.transfer

import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
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
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.applyKyrgyzPhoneMask
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.isCompleteKyrgyzPhone
import com.esom.bank.common.utils.views.kyrgyzPhoneDigits
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentTransferBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding
    private val model: MainViewModel by activityViewModels()
    private var isPanelShown = false
    private var phoneMaskWatcher: TextWatcher? = null
    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var isToPhoneNumber = false
    private val args: TransferFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferBinding.inflate(inflater, container, false)
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

        currentFromCurrency = args.currency
        isToPhoneNumber = currentFromCurrency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
        updateCurrencyIcon(currentFromCurrency)
        setContactHint()
        setupChangeButton()
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        initInitialBalances()
        setupQuickAmounts()
        binding.sumInput.setOnUserTextChangeListener { text ->
            updateCommissionAndTotal(text.toString())
        }

        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.currentCurrencyLayout.setOnClickListener { toggleCurrencyPanel() }

        binding.firstUsdtBtn.setOnClickListener { selectCurrency(CurrencyEnum.USDT_TRC20) }
        binding.firstBitcoinBtn.setOnClickListener { selectCurrency(CurrencyEnum.BTC) }
        binding.firstEthBtn.setOnClickListener { selectCurrency(CurrencyEnum.ETH) }
        binding.firstDigitalBtn.setOnClickListener { selectCurrency(CurrencyEnum.ESOM) }
        binding.firstSomBtn.setOnClickListener { selectCurrency(CurrencyEnum.SOM) }

        binding.sendBtn.setOnClickListener { handleTransferButtonClick() }

        model.myData.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateWalletBalances()
                initInitialBalances()
                updateCommissionAndTotal(binding.sumInput.text.toString())
            }
        }

        model.transferRes.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    binding.sendText.isVisible = false
                    binding.indicator.isVisible = true
                }

                is UiState.Error -> {
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(
                        NavGraphDirections.startFailTransferFragment(it.message)
                    )
                }

                is UiState.Success -> {
                    model.updateUserData()
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                }

                else -> {}
            }
        }
    }

    private fun setupChangeButton() {
        val isCryptoCurrency = currentFromCurrency in listOf(
            CurrencyEnum.BTC, CurrencyEnum.ETH, CurrencyEnum.USDT_TRC20
        )
        binding.changeLayout.isVisible = isCryptoCurrency
        binding.changeLayout.setOnClickListener {
            if (currentFromCurrency in listOf(
                    CurrencyEnum.BTC,
                    CurrencyEnum.ETH,
                    CurrencyEnum.USDT_TRC20
                )
            ) {
                isToPhoneNumber = !isToPhoneNumber
                updateContactType()
            }
        }
    }

    private fun updateContactType() {
        setContactHint()
        binding.contact.setText("")
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun updateWalletBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone
        fun getSuffix(currency: CurrencyEnum, walletAddress: String?): String {
            return when (currency) {
                CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
                else -> walletAddress?.takeLast(3)?.let { "*$it" } ?: ""
            }
        }

        val walletUSDT = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val walletBTC = wallets.find { it.currency == CurrencyEnum.BTC }
        val walletETH = wallets.find { it.currency == CurrencyEnum.ETH }
        val walletESOM = wallets.find { it.currency == CurrencyEnum.ESOM }
        val walletSOM = wallets.find { it.currency == CurrencyEnum.SOM }
        binding.usdt.text = getSuffix(CurrencyEnum.USDT_TRC20, walletUSDT?.address)
        binding.bitcoin.text = getSuffix(CurrencyEnum.BTC, walletBTC?.address)
        binding.eth.text = getSuffix(CurrencyEnum.ETH, walletETH?.address)
        binding.fiat.text = getSuffix(CurrencyEnum.ESOM, walletESOM?.address)
        binding.som.text = getSuffix(CurrencyEnum.SOM, walletSOM?.address)
        val currentWallet = wallets.find { it.currency == currentFromCurrency }
        binding.sum.text =
            currentWallet?.balance?.formatBalanceNew() ?: "0"
        binding.currencyTitle.text = getCurrencyName(currentFromCurrency)
        binding.currency.text = getSuffix(currentFromCurrency, currentWallet?.address)

        binding.peopleTitle.text = getCurrencyName(currentFromCurrency)
        binding.peopleIcon.setImageResource(
            when (currentFromCurrency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.BTC -> R.drawable.bitcoin_icon
                CurrencyEnum.ETH -> R.drawable.eth_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )
    }

    private fun setContactHint() {
        binding.contact.hint = if (isToPhoneNumber) {
            "Введите номер телефона"
        } else {
            "Введите адрес кошелька"
        }
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
                updateCommissionAndTotal(value)
            }
        }
    }

    private fun initInitialBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val walletUSDT = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val walletBTC = wallets.find { it.currency == CurrencyEnum.BTC }
        val walletETH = wallets.find { it.currency == CurrencyEnum.ETH }
        val walletESOM = wallets.find { it.currency == CurrencyEnum.ESOM }
        val walletSOM = wallets.find { it.currency == CurrencyEnum.SOM }
        binding.sum1.text =
            walletUSDT?.balance?.formatBalanceNew() ?: "0"
        binding.sum2.text =
            walletBTC?.balance?.formatBalanceNew() ?: "0"
        binding.sum3.text =
            walletETH?.balance?.formatBalanceNew() ?: "0"
        binding.sum4.text =
            walletESOM?.balance?.formatBalanceNew() ?: "0"
        binding.sum5.text =
            walletSOM?.balance?.formatBalanceNew() ?: "0"
        updateWalletBalances()
    }

    private fun toggleCurrencyPanel() {
        if (isPanelShown) {
            slideOut(binding.typeCurrencyLayout)
            binding.backgroundConversationLayout.visibility = View.GONE
        } else {
            binding.typeCurrencyLayout.visibility = View.VISIBLE
            slideIn(binding.typeCurrencyLayout)
            binding.backgroundConversationLayout.visibility = View.VISIBLE
        }
        isPanelShown = !isPanelShown
    }

    private fun selectCurrency(currency: CurrencyEnum) {
        toggleCurrency(currency)
        if (isPanelShown) toggleCurrencyPanel()
    }

    private fun toggleCurrency(currency: CurrencyEnum) {
        val wasToPhoneNumber = isToPhoneNumber
        currentFromCurrency = currency
        isToPhoneNumber = currency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
        if (wasToPhoneNumber != isToPhoneNumber) {
            binding.contact.setText("")
        }
        updateCurrencyIcon(currency)
        updateWalletBalances()
        updateCommissionAndTotal(binding.sumInput.text.toString())
        setupChangeButton()
        setContactHint()
        if (isToPhoneNumber) {
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

            CurrencyEnum.BTC -> setCurrencyUI(
                R.drawable.bitcoin_icon,
                getString(R.string.bitcoin),
                false
            )

            CurrencyEnum.ETH -> setCurrencyUI(
                R.drawable.eth_icon,
                getString(R.string.ethereum),
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
        val settings = (model.settings.value as? UiState.Success)?.data ?: return
        val amount = amountText.toDoubleOrNull() ?: 0.0

        val commission = when {
            isToPhoneNumber -> 0.0
            else -> {
                when (currentFromCurrency) {
                    CurrencyEnum.BTC -> settings.btcWithdrawFeeFixed
                    CurrencyEnum.ETH -> settings.ethWithdrawFeeFixed
                    CurrencyEnum.USDT_TRC20 -> settings.usdtWithdrawFeeFixed
                    else -> 0.0
                }
            }
        }

        val totalAmount = amount - commission

        binding.comissionValue.text = commission.formatBalanceNew()
        binding.total.text = formatTotalAmount(totalAmount)
    }

    private fun formatTotalAmount(amount: Double): String {
        return if (amount % 1 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.formatBalanceNew()
        }
    }

    private fun applyPhoneMask() {
        val editText = binding.contact
        phoneMaskWatcher?.let { editText.removeTextChangedListener(it) }
        phoneMaskWatcher = null

        editText.setText("")
        editText.setHorizontallyScrolling(false)
        editText.isSingleLine = false
        editText.maxLines = 3

        phoneMaskWatcher = editText.applyKyrgyzPhoneMask()
    }

    private fun removePhoneMask() {
        phoneMaskWatcher?.let { binding.contact.removeTextChangedListener(it) }
        phoneMaskWatcher = null
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

        val settings = (model.settings.value as? UiState.Success)?.data
        if (settings != null && !isToPhoneNumber) {
            val minAmount = when (currentFromCurrency) {
                CurrencyEnum.BTC -> settings.minWithdrawBtc
                CurrencyEnum.ETH -> settings.minWithdrawEth
                CurrencyEnum.USDT_TRC20 -> settings.minWithdrawUsdtTrc20
                else -> 0.0
            }
            if (sum < minAmount) {
                val currencyName = getCurrencyName(currentFromCurrency)
                binding.root.showErrorSnackbar("Минимальная сумма для вывода $currencyName: ${minAmount.formatBalanceNew()}")
                return
            }
        }

        when {
            isToPhoneNumber -> {
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
            (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == currentFromCurrency }?.balance
                ?: 0.0
        if (sum > walletBalance) {
            val currencyName = getCurrencyName(currentFromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        val phone = if (isToPhoneNumber) contactInfo.kyrgyzPhoneDigits() else null
        val address = if (!isToPhoneNumber) contactInfo else null

        if (model.transferRes.value !is UiState.Loading) {
            model.transferToUser(sum, phone ?: "", address, currentFromCurrency)
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
