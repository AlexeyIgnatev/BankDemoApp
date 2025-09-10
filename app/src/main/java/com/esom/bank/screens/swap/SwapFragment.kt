package com.esom.bank.screens.swap

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentSwapBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
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
        binding.sum50Layout.setOnClickListener {
            binding.sumInput.setText("50")
        }
        binding.sum100Layout.setOnClickListener {
            binding.sumInput.setText("100")
        }
        binding.sum1000Layout.setOnClickListener {
            binding.sumInput.setText("1000")
        }
        binding.sum10000Layout.setOnClickListener {
            binding.sumInput.setText("10000")
        }
        val somBalance = (model.myData.value as? UiState.Success)?.data?.wallets
            ?.find { it.currency == CurrencyEnum.SOM }
            ?.balance ?: 0.0

        binding.sum.text = somBalance.toString()
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
                it.elevation = 20f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            isPeoplePanelShown = !isPeoplePanelShown
        }

        binding.firstDigitalBtn.setOnClickListener {
            toggleCurrency(true)
        }

        binding.secondDigitalBtn.setOnClickListener {
            toggleCurrency(false)
        }

        binding.sumInput.setOnUserTextChangeListener {
            updateAmounts(it.toDoubleOrNull(), null)
        }

        binding.sendBtn.setOnClickListener {
            handleConvertButtonClick()
        }

        model.swapRes.observe(viewLifecycleOwner) {
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

    private fun initInitialIcons() {
        if(args.direction == 0) {
            binding.fiatIcon.setImageResource(R.drawable.salam_icon)
            binding.fiatTitle.text = getString(R.string.digital)
            binding.icon.setImageResource(R.drawable.som_icon)
            binding.currencyTitle.text = getString(R.string.som)
            currentFromCurrency = CurrencyEnum.SOM

            binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
            binding.peopleFiatTitle.text = getString(R.string.som)
            binding.peopleIcon.setImageResource(R.drawable.salam_icon)
            binding.peopleTitle.text = getString(R.string.digital)
            currentToCurrency = CurrencyEnum.ESOM
        } else {
            binding.somIcon.visibility = View.GONE
            binding.fiatIcon.setImageResource(R.drawable.som_icon)
            binding.fiatTitle.text = getString(R.string.som)
            binding.icon.setImageResource(R.drawable.salam_icon)
            binding.currencyTitle.text = getString(R.string.digital)
            currentFromCurrency = CurrencyEnum.ESOM

            binding.peopleFiatIcon.setImageResource(R.drawable.salam_icon)
            binding.peopleFiatTitle.text = getString(R.string.digital)
            binding.peopleIcon.setImageResource(R.drawable.som_icon)
            binding.peopleTitle.text = getString(R.string.som)
            currentToCurrency = CurrencyEnum.SOM
        }
    }

    private fun toggleCurrency(isFirstButton: Boolean) {
        if (isFirstButton) {
            if (currentFromCurrency == CurrencyEnum.ESOM) {
                binding.fiatIcon.setImageResource(R.drawable.salam_icon)
                binding.fiatTitle.text = getString(R.string.digital)
                binding.icon.setImageResource(R.drawable.som_icon)
                binding.currencyTitle.text = getString(R.string.som)
                currentFromCurrency = CurrencyEnum.SOM
                binding.somIcon.visibility = View.VISIBLE
            } else {
                binding.fiatIcon.setImageResource(R.drawable.som_icon)
                binding.fiatTitle.text = getString(R.string.som)
                binding.icon.setImageResource(R.drawable.salam_icon)
                binding.currencyTitle.text = getString(R.string.digital)
                currentFromCurrency = CurrencyEnum.ESOM
                binding.somIcon.visibility = View.GONE
            }
        } else {
            if (currentToCurrency == CurrencyEnum.SOM) {
                binding.peopleFiatIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleFiatTitle.text = getString(R.string.digital)
                binding.peopleIcon.setImageResource(R.drawable.som_icon)
                binding.peopleTitle.text = getString(R.string.som)
                currentToCurrency = CurrencyEnum.ESOM
            } else {
                binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                binding.peopleFiatTitle.text = getString(R.string.som)
                binding.peopleIcon.setImageResource(R.drawable.salam_icon)
                binding.peopleTitle.text = getString(R.string.digital)
                currentToCurrency = CurrencyEnum.SOM
            }
        }
    }

    private fun handleConvertButtonClick() {
        if (model.swapRes.value is UiState.Loading) {
            return
        }

        val fromAmount = binding.sumInput.text.toString().toDoubleOrNull()

        if (fromAmount == null) {
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
            currentFromCurrency == CurrencyEnum.SOM && currentToCurrency == CurrencyEnum.ESOM -> {
                model.transferFromFiat(fromAmount)
            }
            currentFromCurrency == CurrencyEnum.ESOM && currentToCurrency == CurrencyEnum.SOM -> {
                model.transferToFiat(fromAmount)
            }
            else -> {
                binding.root.showErrorSnackbar("Невозможно конвертировать между одинаковыми валютами")
            }
        }
    }

    private fun updateAmounts(fromAmount: Double?, toAmount: Double?) {
        val platformFee =
            (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0

        if (fromAmount == null && toAmount == null) {
            binding.sumInput.setTextProgrammatically("")
        } else if (fromAmount != null) {
            val newToAmount = fromAmount * (1 - platformFee)
            val newSomText = fromAmount.format(2)
            if (newSomText != binding.sumInput.text.toString() && !binding.sumInput.text.toString()
                    .endsWith(".")
            ) {
                binding.sumInput.setTextProgrammatically(fromAmount.format(2))
                binding.sumInput.setSelection(fromAmount.format(2).length)
            }
        } else if (toAmount != null) {
            val newFromAmount = toAmount / (1 - platformFee)
            binding.sumInput.setTextProgrammatically(newFromAmount.format(2))
            binding.sumInput.setSelection(newFromAmount.format(2).length)
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