package com.esom.bank.screens.swap

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
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.setTextProgrammatically
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
    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var currentToCurrency: CurrencyEnum = CurrencyEnum.SOM

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
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
            )
            insets
        }

        initInitialIcons()
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

        updateBalanceDisplay()

        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.currentCurrencyLayout.setOnClickListener { togglePanel(true) }
        binding.peopleLayout.setOnClickListener { togglePanel(false) }
        binding.firstDigitalBtn.setOnClickListener { toggleCurrency(true) }
        binding.secondDigitalBtn.setOnClickListener { toggleCurrency(false) }

        binding.sumInput.setOnUserTextChangeListener {
            updateAmounts(it.toDoubleOrNull(), null)
            updateCommissionAndTotal()
        }

        binding.sendBtn.setOnClickListener { handleConvertButtonClick() }

        model.swapRes.observe(viewLifecycleOwner) {
            binding.sendText.isVisible = it !is UiState.Loading
            binding.indicator.isVisible = it is UiState.Loading

            when (it) {
                is UiState.Error -> findNavController().navigate(NavGraphDirections.startFailTransferFragment(it.message))
                is UiState.Success -> {
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                    findNavController().popBackStack()
                }
                else -> {}
            }
        }
    }

    private fun initInitialIcons() {
        if (args.direction == 0) {
            currentFromCurrency = CurrencyEnum.SOM
            currentToCurrency = CurrencyEnum.ESOM
            binding.somIcon.visibility = View.VISIBLE
            binding.somIcon50.visibility = View.VISIBLE
            binding.somIcon100.visibility = View.VISIBLE
            binding.somIcon1000.visibility = View.VISIBLE
            binding.somIcon10000.visibility = View.VISIBLE
            binding.comissionTitle.text = getString(R.string.som)
            binding.somIconSwap.visibility = View.VISIBLE
            binding.secondTitle.text = getString(R.string.digital)
            binding.salamIconSwap.visibility = View.GONE
            binding.totalSomIcon.visibility = View.GONE

            binding.fiatIcon.setImageResource(R.drawable.salam_icon)
            binding.fiatTitle.text = getString(R.string.digital)
            binding.icon.setImageResource(R.drawable.som_icon)
            binding.currencyTitle.text = getString(R.string.som)

            binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
            binding.peopleFiatTitle.text = getString(R.string.som)
            binding.peopleIcon.setImageResource(R.drawable.salam_icon)
            binding.peopleTitle.text = getString(R.string.digital)
        } else {
            currentFromCurrency = CurrencyEnum.ESOM
            currentToCurrency = CurrencyEnum.SOM
            binding.somIcon.visibility = View.GONE
            binding.somIcon50.visibility = View.GONE
            binding.somIcon100.visibility = View.GONE
            binding.somIcon1000.visibility = View.GONE
            binding.somIcon10000.visibility = View.GONE
            binding.comissionTitle.text = getString(R.string.digital)
            binding.somIconSwap.visibility = View.VISIBLE
            binding.secondTitle.text = getString(R.string.som)
            binding.salamIconSwap.visibility = View.VISIBLE
            binding.totalSomIcon.visibility = View.VISIBLE

            binding.fiatIcon.setImageResource(R.drawable.som_icon)
            binding.fiatTitle.text = getString(R.string.som)
            binding.icon.setImageResource(R.drawable.salam_icon)
            binding.currencyTitle.text = getString(R.string.digital)

            binding.peopleFiatIcon.setImageResource(R.drawable.salam_icon)
            binding.peopleFiatTitle.text = getString(R.string.digital)
            binding.peopleIcon.setImageResource(R.drawable.som_icon)
            binding.peopleTitle.text = getString(R.string.som)
        }
        updateBalanceDisplay()
    }

    private fun toggleCurrency(isFirstButton: Boolean) {
        if (isFirstButton) {
            if (currentFromCurrency == CurrencyEnum.ESOM) {
                currentFromCurrency = CurrencyEnum.SOM
                currentToCurrency = CurrencyEnum.ESOM

                binding.fiatIcon.setImageResource(R.drawable.salam_icon)
                binding.fiatTitle.text = getString(R.string.digital)
                binding.icon.setImageResource(R.drawable.som_icon)
                binding.currencyTitle.text = getString(R.string.som)

                binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                binding.peopleFiatTitle.text = getString(R.string.som)
                binding.peopleIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleTitle.text = getString(R.string.digital)
            } else {
                currentFromCurrency = CurrencyEnum.ESOM
                currentToCurrency = CurrencyEnum.SOM

                binding.fiatIcon.setImageResource(R.drawable.som_icon)
                binding.fiatTitle.text = getString(R.string.som)
                binding.icon.setImageResource(R.drawable.salam_icon)
                binding.currencyTitle.text = getString(R.string.digital)

                binding.peopleFiatIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleFiatTitle.text = getString(R.string.digital)
                binding.peopleIcon.setImageResource(R.drawable.som_icon)
                binding.peopleTitle.text = getString(R.string.som)
            }
        } else {
            if (currentToCurrency == CurrencyEnum.SOM) {
                currentToCurrency = CurrencyEnum.ESOM
                currentFromCurrency = CurrencyEnum.SOM

                binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                binding.peopleFiatTitle.text = getString(R.string.som)
                binding.peopleIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleTitle.text = getString(R.string.digital)

                binding.fiatIcon.setImageResource(R.drawable.salam_icon)
                binding.fiatTitle.text = getString(R.string.digital)
                binding.icon.setImageResource(R.drawable.som_icon)
                binding.currencyTitle.text = getString(R.string.som)
            } else {
                currentToCurrency = CurrencyEnum.SOM
                currentFromCurrency = CurrencyEnum.ESOM

                binding.peopleFiatIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleFiatTitle.text = getString(R.string.digital)
                binding.peopleIcon.setImageResource(R.drawable.som_icon)
                binding.peopleTitle.text = getString(R.string.som)

                binding.fiatIcon.setImageResource(R.drawable.som_icon)
                binding.fiatTitle.text = getString(R.string.som)
                binding.icon.setImageResource(R.drawable.salam_icon)
                binding.currencyTitle.text = getString(R.string.digital)
            }
        }

        if (currentFromCurrency == CurrencyEnum.SOM) {
            binding.somIcon.visibility = View.VISIBLE
            binding.somIcon50.visibility = View.VISIBLE
            binding.somIcon100.visibility = View.VISIBLE
            binding.somIcon1000.visibility = View.VISIBLE
            binding.somIcon10000.visibility = View.VISIBLE
            binding.comissionTitle.text = getString(R.string.som)
            binding.somIconSwap.visibility = View.VISIBLE
            binding.secondTitle.text = getString(R.string.digital)
            binding.salamIconSwap.visibility = View.GONE
            binding.totalSomIcon.visibility = View.GONE
        } else {
            binding.somIcon.visibility = View.GONE
            binding.somIcon50.visibility = View.GONE
            binding.somIcon100.visibility = View.GONE
            binding.somIcon1000.visibility = View.GONE
            binding.somIcon10000.visibility = View.GONE
            binding.comissionTitle.text = getString(R.string.digital)
            binding.somIconSwap.visibility = View.GONE
            binding.secondTitle.text = getString(R.string.som)
            binding.salamIconSwap.visibility = View.VISIBLE
            binding.totalSomIcon.visibility = View.VISIBLE
        }

        updateBalanceDisplay()
        updateCommissionAndTotal()
    }

    private fun updateBalanceDisplay() {
        val wallet = (model.myData.value as? UiState.Success)?.data?.wallets
            ?.find { it.currency == currentFromCurrency }

        val walletBalance = wallet?.balance ?: 0.0
        binding.sum.text = walletBalance.format(2)

        val walletSuffix = when (currentFromCurrency) {
            CurrencyEnum.SOM -> (model.myData.value as? UiState.Success)?.data?.phone?.takeLast(3)
            else -> wallet?.address?.takeLast(3)
        } ?: ""
        binding.currency.text = "*$walletSuffix"

        val contactSuffix = (model.myData.value as? UiState.Success)?.data?.phone?.takeLast(3)
        binding.contact.text = "*$contactSuffix"
    }

    private fun updateCommissionAndTotal() {
        val platformFee = (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0
        val fromAmount = binding.sumInput.text.toString().toDoubleOrNull() ?: 0.0

        val exchangeRate = when {
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM ->
                (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == CurrencyEnum.ESOM }?.buyRate ?: 1.0
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM ->
                (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == CurrencyEnum.ESOM }?.sellRate ?: 1.0
            else -> 1.0
        }

        val secondValue = fromAmount * exchangeRate
        val commissionAmount = if (currentFromCurrency == CurrencyEnum.SOM) secondValue * platformFee else fromAmount * platformFee
        val totalAmount = secondValue - commissionAmount

        binding.comissionValue.text = fromAmount.format(2)
        binding.secondValue.text = secondValue.format(2)
        binding.total.text = totalAmount.format(2)
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
            val currencyName = if (currentFromCurrency == CurrencyEnum.SOM) "Сом" else "Салам"
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        when {
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> model.transferFromFiat(fromAmount)
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> model.transferToFiat(fromAmount)
            else -> binding.root.showErrorSnackbar("Невозможно конвертировать между одинаковыми валютами")
        }
    }

    private fun updateAmounts(fromAmount: Double?, toAmount: Double?) {
        val platformFee = (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0

        when {
            fromAmount != null -> {
                val formatted = fromAmount.format(2)
                if (formatted != binding.sumInput.text.toString() && !binding.sumInput.text.toString().endsWith(".")) {
                    binding.sumInput.setTextProgrammatically(formatted)
                    binding.sumInput.setSelection(formatted.length)
                }
            }
            toAmount != null -> {
                val newFromAmount = toAmount / (1 - platformFee)
                binding.sumInput.setTextProgrammatically(newFromAmount.format(2))
                binding.sumInput.setSelection(newFromAmount.format(2).length)
            }
            else -> binding.sumInput.setTextProgrammatically("")
        }

        updateCommissionAndTotal()
    }

    private fun togglePanel(isCurrencyPanel: Boolean) {
        if (isCurrencyPanel) {
            binding.peopleLayout.isClickable = !isPanelShown
            if (isPanelShown) slideOut(binding.typeCurrencyLayout) else slideIn(binding.typeCurrencyLayout)
            binding.backgroundConversationLayout.visibility = if (isPanelShown) View.GONE else View.VISIBLE
            isPanelShown = !isPanelShown
        } else {
            binding.currentCurrencyLayout.isClickable = !isPeoplePanelShown
            binding.peopleCurrencyLayout.visibility = if (isPeoplePanelShown) View.GONE else View.VISIBLE
            if (!isPeoplePanelShown) slideIn(binding.peopleCurrencyLayout) else slideOut(binding.peopleCurrencyLayout)
            binding.peopleLayout.elevation = if (isPeoplePanelShown) 0f else 20f
            isPeoplePanelShown = !isPeoplePanelShown
        }
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
