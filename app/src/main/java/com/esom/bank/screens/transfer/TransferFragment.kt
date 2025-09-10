package com.esom.bank.screens.transfer

import android.os.Bundle
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
    private var isPeoplePanelShown = false

    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var currentToCurrency: CurrencyEnum = CurrencyEnum.SOM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        binding.sum50Layout.setOnClickListener {
            binding.sumInput.setText("50")
            updateCommissionAndTotal("50")
        }
        binding.sum100Layout.setOnClickListener {
            binding.sumInput.setText("100")
            updateCommissionAndTotal("100")
        }
        binding.sum1000Layout.setOnClickListener {
            binding.sumInput.setText("1000")
            updateCommissionAndTotal("1000")
        }
        binding.sum10000Layout.setOnClickListener {
            binding.sumInput.setText("10000")
            updateCommissionAndTotal("10000")
        }

        binding.sumInput.setOnUserTextChangeListener { text ->
            updateCommissionAndTotal(text.toString())
        }

        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

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
            if(isPeoplePanelShown) {
                binding.currentCurrencyLayout.isClickable = true
                it.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            } else {
                binding.currentCurrencyLayout.isClickable = false
                it.elevation = 15f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            isPeoplePanelShown = !isPeoplePanelShown
        }

        binding.firstUsdtBtn.setOnClickListener {
            toggleCurrency(true, CurrencyEnum.USDT_TRC20)
        }

        binding.firstBitcoinBtn.setOnClickListener {
            toggleCurrency(true, CurrencyEnum.BTC)
        }

        binding.firstEthBtn.setOnClickListener {
            toggleCurrency(true, CurrencyEnum.ETH)
        }

        binding.firstDigitalBtn.setOnClickListener {
            toggleCurrency(true, CurrencyEnum.ESOM)
        }

        binding.secondUsdtBtn.setOnClickListener {
            toggleCurrency(false, CurrencyEnum.USDT_TRC20)
        }

        binding.secondBitcoinBtn.setOnClickListener {
            toggleCurrency(false, CurrencyEnum.BTC)
        }

        binding.secondEthBtn.setOnClickListener {
            toggleCurrency(false, CurrencyEnum.ETH)
        }

        binding.secondDigitalBtn.setOnClickListener {
            toggleCurrency(false, CurrencyEnum.SOM)
        }

        binding.sendBtn.setOnClickListener {
            handleTransferButtonClick()
        }

        model.myData.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is UiState.Success -> {
                    updateWalletBalances()
                    updateCommissionAndTotal(binding.sumInput.text.toString())
                }
                else -> {}
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
            }
        }
    }

    private fun initCurrencyIcons() {
        binding.fiatIcon.setImageResource(R.drawable.salam_icon)
        binding.fiatTitle.text = getString(R.string.digital)
        binding.icon.setImageResource(R.drawable.salam_icon)
        binding.currencyTitle.text = getString(R.string.digital)
        binding.somIcon.visibility = View.GONE

        binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
        binding.peopleFiatTitle.text = getString(R.string.som)
        binding.peopleIcon.setImageResource(R.drawable.som_icon)
        binding.peopleTitle.text = getString(R.string.som)

        currentFromCurrency = CurrencyEnum.ESOM
        currentToCurrency = CurrencyEnum.SOM
    }

    private fun toggleCurrency(isFromCurrency: Boolean, currency: CurrencyEnum) {
        if (isFromCurrency) {
            currentFromCurrency = currency
            updateCurrencyIcon(true, currency)
        } else {
            currentToCurrency = currency
            updateCurrencyIcon(false, currency)
        }
        updateWalletBalances()
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun updateCurrencyIcon(isFromCurrency: Boolean, currency: CurrencyEnum) {
        val iconView = if (isFromCurrency) binding.icon else binding.peopleIcon
        val titleView = if (isFromCurrency) binding.currencyTitle else binding.peopleTitle

        when (currency) {
            CurrencyEnum.SOM -> {
                iconView.setImageResource(R.drawable.som_icon)
                titleView.text = getString(R.string.som)
                binding.somIcon.visibility = View.VISIBLE
            }
            CurrencyEnum.ESOM -> {
                iconView.setImageResource(R.drawable.salam_icon)
                titleView.text = getString(R.string.digital)
                binding.somIcon.visibility = View.GONE
            }
            CurrencyEnum.BTC -> {
                iconView.setImageResource(R.drawable.bitcoin_icon)
                titleView.text = getString(R.string.bitcoin)
                binding.somIcon.visibility = View.GONE
            }
            CurrencyEnum.ETH -> {
                iconView.setImageResource(R.drawable.eth_icon)
                titleView.text = getString(R.string.ethereum)
                binding.somIcon.visibility = View.GONE
            }
            CurrencyEnum.USDT_TRC20 -> {
                iconView.setImageResource(R.drawable.usdt_icon)
                titleView.text = getString(R.string.usdt)
                binding.somIcon.visibility = View.GONE
            }
        }
    }

    private fun updateWalletBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return

        val usdtBalance = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }?.balance?.format(2) ?: "0.0"
        val btcBalance = wallets.find { it.currency == CurrencyEnum.BTC }?.balance?.format(2) ?: "0.0"
        val ethBalance = wallets.find { it.currency == CurrencyEnum.ETH }?.balance?.format(2) ?: "0.0"
        val esomBalance = wallets.find { it.currency == CurrencyEnum.ESOM }?.balance?.format(2) ?: "0.0"

        binding.sum1.text = usdtBalance
        binding.sum2.text = btcBalance
        binding.sum3.text = ethBalance
        binding.sum4.text = esomBalance

        val currentBalance = wallets.find { it.currency == currentFromCurrency }?.balance?.format(2) ?: "0.0"
        binding.sum.text = currentBalance
    }

    private fun updateCommissionAndTotal(amountText: String) {
        val platformFee = (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0
        val amount = amountText.toDoubleOrNull() ?: 0.0

        // Рассчитываем комиссию
        val commission = amount * platformFee
        val totalAmount = amount - commission

        binding.comissionValue.text = commission.format(2)
        binding.total.text = totalAmount.format(2)
    }

    private fun handleTransferButtonClick() {
        if (model.transferRes.value is UiState.Loading) {
            return
        }

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

        val walletBalance = (model.myData.value as? UiState.Success)?.data?.wallets
            ?.find { it.currency == currentFromCurrency }
            ?.balance ?: 0.0

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

        val phone = if (currentFromCurrency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)) {
            contactInfo.filter { it.isDigit() }
        } else {
            ""
        }

        val address = if (currentFromCurrency !in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)) {
            contactInfo
        } else {
            null
        }

        if (model.transferRes.value !is UiState.Loading) {
            model.transferToUser(sum, phone, address, currentFromCurrency)
        }
    }

    private fun slideIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.post {
            view.translationY = -view.height.toFloat()
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(450)
                .start()
        }
    }

    private fun slideOut(view: View) {
        view.animate()
            .translationY(-view.height.toFloat())
            .alpha(0f)
            .setDuration(450)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }
}