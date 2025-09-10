package com.esom.bank.screens.transfer

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
            )
            insets
        }
        initCurrencyIcons()
        updateWalletBalances()
        setupQuickAmounts()
        binding.sumInput.setOnUserTextChangeListener { text -> updateCommissionAndTotal(text.toString()) }
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.currentCurrencyLayout.setOnClickListener {
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
        binding.firstUsdtBtn.setOnClickListener { toggleCurrency(CurrencyEnum.USDT_TRC20) }
        binding.firstBitcoinBtn.setOnClickListener { toggleCurrency(CurrencyEnum.BTC) }
        binding.firstEthBtn.setOnClickListener { toggleCurrency(CurrencyEnum.ETH) }
        binding.firstDigitalBtn.setOnClickListener { toggleCurrency(CurrencyEnum.ESOM) }
        binding.firstSomBtn.setOnClickListener { toggleCurrency(CurrencyEnum.SOM) }
        binding.sendBtn.setOnClickListener { handleTransferButtonClick() }
        model.myData.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateWalletBalances()
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
                    findNavController().navigate(NavGraphDirections.startFailTransferFragment(it.message))
                }
                is UiState.Success -> {
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                    findNavController().popBackStack()
                }
                else -> {}
            }
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

    private fun initCurrencyIcons() {
        setCurrencyUI(CurrencyEnum.ESOM, R.drawable.salam_icon, getString(R.string.digital), showSomIcons = false)
        currentFromCurrency = CurrencyEnum.ESOM
    }

    private fun toggleCurrency(currency: CurrencyEnum) {
        currentFromCurrency = currency
        updateCurrencyIcon(currency)
        updateWalletBalances()
        updateCommissionAndTotal(binding.sumInput.text.toString())
        binding.contact.hint = if (currency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)) {
            "Введите номер телефона"
        } else {
            "Введите адрес кошелька"
        }
        if (currency == CurrencyEnum.SOM) {
            applyPhoneMask()
        } else {
            phoneMaskWatcher?.let { binding.contact.removeTextChangedListener(it) }
            phoneMaskWatcher = null
            binding.contact.setText("")
            binding.contact.inputType = InputType.TYPE_CLASS_TEXT
            binding.contact.filters = arrayOf()
        }
    }

    private fun updateCurrencyIcon(currency: CurrencyEnum) {
        when (currency) {
            CurrencyEnum.SOM -> setCurrencyUI(currency, R.drawable.som_icon, getString(R.string.som), true)
            CurrencyEnum.ESOM -> setCurrencyUI(currency, R.drawable.salam_icon, getString(R.string.digital), false)
            CurrencyEnum.BTC -> setCurrencyUI(currency, R.drawable.bitcoin_icon, getString(R.string.bitcoin), false)
            CurrencyEnum.ETH -> setCurrencyUI(currency, R.drawable.eth_icon, getString(R.string.ethereum), false)
            CurrencyEnum.USDT_TRC20 -> setCurrencyUI(currency, R.drawable.usdt_icon, getString(R.string.usdt), false)
        }
    }

    private fun setCurrencyUI(currency: CurrencyEnum, iconRes: Int, title: String, showSomIcons: Boolean) {
        binding.icon.setImageResource(iconRes)
        binding.currencyTitle.text = title
        binding.peopleIcon.setImageResource(iconRes)
        binding.peopleTitle.text = title
        binding.somIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.totalSomIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
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
        binding.sum.text = currentWallet?.balance?.format(2) ?: "0.00"

        binding.currencyTitle.text = getCurrencyName(currentFromCurrency)
        binding.currency.text = getSuffix(currentFromCurrency, currentWallet?.address)
        binding.peopleTitle.text = getCurrencyName(currentFromCurrency)
    }


    private fun updateCommissionAndTotal(amountText: String) {
        val platformFee = (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val commission = amount * platformFee
        val totalAmount = amount - commission
        binding.comissionValue.text = commission.format(2)
        binding.total.text = totalAmount.format(2)
    }

    private fun applyPhoneMask() {
        val editText = binding.contact
        editText.setText("")
        editText.inputType = InputType.TYPE_CLASS_PHONE
        phoneMaskWatcher?.let { editText.removeTextChangedListener(it) }
        val watcher = object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true
                val digits = s.toString().filter { it.isDigit() }
                val builder = StringBuilder()
                if (digits.isNotEmpty()) builder.append("+996 ")
                if (digits.length >= 3) builder.append("(").append(digits.substring(3, minOf(6, digits.length))).append(") ")
                if (digits.length >= 6) builder.append(digits.substring(6, minOf(9, digits.length)))
                editText.removeTextChangedListener(this)
                editText.setText(builder.toString())
                editText.text?.let { editText.setSelection(it.length) }
                editText.addTextChangedListener(this)
                isEditing = false
            }
        }
        editText.addTextChangedListener(watcher)
        phoneMaskWatcher = watcher
    }

    private fun handleTransferButtonClick() {
        if (model.transferRes.value is UiState.Loading) return
        val sum = binding.sumInput.text.toString().toDoubleOrNull()
        val contactInfo = binding.contact.text.toString().trim()
        if (sum == null) {
            binding.root.showErrorSnackbar("Введите сумму для перевода")
            return
        }
        when (currentFromCurrency) {
            CurrencyEnum.SOM, CurrencyEnum.ESOM -> {
                if (contactInfo.isEmpty() || contactInfo.filter { it.isDigit() }.length < 10) {
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
        val walletBalance = (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == currentFromCurrency }?.balance ?: 0.0
        if (sum > walletBalance) {
            val currencyName = when (currentFromCurrency) {
                CurrencyEnum.SOM -> "Сом"
                CurrencyEnum.ESOM -> "Салам"
                CurrencyEnum.BTC -> "Bitcoin"
                CurrencyEnum.ETH -> "Ethereum"
                CurrencyEnum.USDT_TRC20 -> "USDT"
            }
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }
        val phone = if (currentFromCurrency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)) contactInfo.filter { it.isDigit() } else ""
        val address = if (currentFromCurrency !in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)) contactInfo else null
        if (model.transferRes.value !is UiState.Loading) {
            model.transferToUser(sum, phone, address, currentFromCurrency)
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
            view.translationY = -view.height.toFloat()
            view.animate().translationY(0f).alpha(1f).setDuration(450).start()
        }
    }

    private fun slideOut(view: View) {
        view.animate().translationY(-view.height.toFloat()).alpha(0f).setDuration(450).withEndAction {
            view.visibility = View.GONE
        }.start()
    }
}
