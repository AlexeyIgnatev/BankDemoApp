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
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentTransferBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.settigns.SettingsFragment.Companion.PHONE_NUMBER
import dagger.hilt.android.AndroidEntryPoint
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.watchers.FormatWatcher
import ru.tinkoff.decoro.watchers.MaskFormatWatcher


@AndroidEntryPoint
class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding

    private val model: MainViewModel by activityViewModels()
    private var isPanelShown = false
    private var isPeoplePanelShown = false


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
            val centerIcon = binding.icon.drawable.constantState
            val usdtIcon = binding.usdtIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (usdtIcon == somIcon) {
                binding.usdtIcon.setImageResource(R.drawable.usdt_icon)
                binding.usdtTitle.text = getString(R.string.usdt)

                when (binding.currencyTitle.text.toString()) {
                    getString(R.string.bitcoin) -> {
                        binding.bitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.bitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.ethIcon.setImageResource(R.drawable.som_icon)
                        binding.ethTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.fiatIcon.setImageResource(R.drawable.som_icon)
                        binding.fiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.icon.setImageResource(R.drawable.som_icon)
                        binding.currencyTitle.text = getString(R.string.som)
                    }
                }

                binding.icon.setImageResource(R.drawable.usdt_icon)
                binding.currencyTitle.text = getString(R.string.usdt)
            } else {
                val tempIcon = binding.icon.drawable
                val tempTitle = binding.currencyTitle.text.toString()

                binding.icon.setImageDrawable(binding.usdtIcon.drawable)
                binding.currencyTitle.text = binding.usdtTitle.text.toString()

                binding.usdtIcon.setImageDrawable(tempIcon)
                binding.usdtTitle.text = tempTitle
            }
        }

        binding.firstBitcoinBtn.setOnClickListener {
            val centerIcon = binding.icon.drawable.constantState
            val bitcoinIcon = binding.bitcoinIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (bitcoinIcon == somIcon) {
                binding.bitcoinIcon.setImageResource(R.drawable.bitcoin_icon)
                binding.bitcoinTitle.text = getString(R.string.bitcoin)

                when (binding.currencyTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.usdtIcon.setImageResource(R.drawable.som_icon)
                        binding.usdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.ethIcon.setImageResource(R.drawable.som_icon)
                        binding.ethTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.fiatIcon.setImageResource(R.drawable.som_icon)
                        binding.fiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.icon.setImageResource(R.drawable.som_icon)
                        binding.currencyTitle.text = getString(R.string.som)
                    }
                }

                binding.icon.setImageResource(R.drawable.bitcoin_icon)
                binding.currencyTitle.text = getString(R.string.bitcoin)
            } else {
                val tempIcon = binding.icon.drawable
                val tempTitle = binding.currencyTitle.text.toString()

                binding.icon.setImageDrawable(binding.bitcoinIcon.drawable)
                binding.currencyTitle.text = binding.bitcoinTitle.text.toString()

                binding.bitcoinIcon.setImageDrawable(tempIcon)
                binding.bitcoinTitle.text = tempTitle
            }
        }

        binding.firstEthBtn.setOnClickListener {
            val centerIcon = binding.icon.drawable.constantState
            val ethIcon = binding.ethIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (ethIcon == somIcon) {
                binding.ethIcon.setImageResource(R.drawable.eth_icon)
                binding.ethTitle.text = getString(R.string.ethereum)

                when (binding.currencyTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.usdtIcon.setImageResource(R.drawable.som_icon)
                        binding.usdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.bitcoin) -> {
                        binding.bitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.bitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.fiatIcon.setImageResource(R.drawable.som_icon)
                        binding.fiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.icon.setImageResource(R.drawable.som_icon)
                        binding.currencyTitle.text = getString(R.string.som)
                    }
                }

                binding.icon.setImageResource(R.drawable.eth_icon)
                binding.currencyTitle.text = getString(R.string.ethereum)
            } else {
                val tempIcon = binding.icon.drawable
                val tempTitle = binding.currencyTitle.text.toString()

                binding.icon.setImageDrawable(binding.ethIcon.drawable)
                binding.currencyTitle.text = binding.ethTitle.text.toString()

                binding.ethIcon.setImageDrawable(tempIcon)
                binding.ethTitle.text = tempTitle
            }
        }

        binding.firstDigitalBtn.setOnClickListener {
            val centerIcon = binding.icon.drawable.constantState
            val fiatIcon = binding.fiatIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (fiatIcon == somIcon) {
                binding.fiatIcon.setImageResource(R.drawable.digital_icon)
                binding.fiatTitle.text = getString(R.string.digital)

                when (binding.currencyTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.usdtIcon.setImageResource(R.drawable.som_icon)
                        binding.usdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.bitcoin) -> {
                        binding.bitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.bitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.ethIcon.setImageResource(R.drawable.som_icon)
                        binding.ethTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.icon.setImageResource(R.drawable.som_icon)
                        binding.currencyTitle.text = getString(R.string.som)
                    }
                }

                binding.icon.setImageResource(R.drawable.digital_icon)
                binding.currencyTitle.text = getString(R.string.digital)
            } else {
                val tempIcon = binding.icon.drawable
                val tempTitle = binding.currencyTitle.text.toString()

                binding.icon.setImageDrawable(binding.fiatIcon.drawable)
                binding.currencyTitle.text = binding.fiatTitle.text.toString()

                binding.fiatIcon.setImageDrawable(tempIcon)
                binding.fiatTitle.text = tempTitle
            }
        }


        binding.secondUsdtBtn.setOnClickListener {
            val centerIcon = binding.icon.drawable.constantState
            val usdtIcon = binding.peopleUsdtIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (usdtIcon == somIcon) {
                binding.peopleUsdtIcon.setImageResource(R.drawable.usdt_icon)
                binding.peopleUsdtTitle.text = getString(R.string.usdt)

                when (binding.peopleTitle.text.toString()) {
                    getString(R.string.bitcoin) -> {
                        binding.peopleBitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleBitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.peopleEthIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleEthTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleFiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.peopleIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleTitle.text = getString(R.string.som)
                    }
                }

                binding.peopleIcon.setImageResource(R.drawable.usdt_icon)
                binding.peopleTitle.text = getString(R.string.usdt)
            } else {
                val tempIcon = binding.peopleIcon.drawable
                val tempTitle = binding.peopleTitle.text.toString()

                binding.peopleIcon.setImageDrawable(binding.peopleUsdtIcon.drawable)
                binding.peopleTitle.text = binding.peopleUsdtTitle.text.toString()

                binding.peopleUsdtIcon.setImageDrawable(tempIcon)
                binding.peopleUsdtTitle.text = tempTitle
            }
        }

        binding.secondBitcoinBtn.setOnClickListener {
            val centerIcon = binding.peopleIcon.drawable.constantState
            val bitcoinIcon = binding.peopleBitcoinIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (bitcoinIcon == somIcon) {
                binding.peopleBitcoinIcon.setImageResource(R.drawable.bitcoin_icon)
                binding.peopleBitcoinTitle.text = getString(R.string.bitcoin)

                when (binding.currencyTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.peopleUsdtIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleUsdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.peopleEthIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleEthTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleFiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.peopleIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleTitle.text = getString(R.string.som)
                    }
                }

                binding.peopleIcon.setImageResource(R.drawable.bitcoin_icon)
                binding.peopleTitle.text = getString(R.string.bitcoin)
            } else {
                val tempIcon = binding.peopleIcon.drawable
                val tempTitle = binding.peopleTitle.text.toString()

                binding.peopleIcon.setImageDrawable(binding.peopleBitcoinIcon.drawable)
                binding.peopleTitle.text = binding.peopleBitcoinTitle.text.toString()

                binding.peopleBitcoinIcon.setImageDrawable(tempIcon)
                binding.peopleBitcoinTitle.text = tempTitle
            }
        }

        binding.secondEthBtn.setOnClickListener {
            val centerIcon = binding.peopleIcon.drawable.constantState
            val ethIcon = binding.peopleEthIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (ethIcon == somIcon) {
                binding.peopleEthIcon.setImageResource(R.drawable.eth_icon)
                binding.peopleEthTitle.text = getString(R.string.ethereum)

                when (binding.peopleTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.peopleUsdtIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleUsdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.bitcoin) -> {
                        binding.peopleBitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleBitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.digital) -> {
                        binding.peopleFiatIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleFiatTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.peopleIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleTitle.text = getString(R.string.som)
                    }
                }

                binding.peopleIcon.setImageResource(R.drawable.eth_icon)
                binding.peopleTitle.text = getString(R.string.ethereum)
            } else {
                val tempIcon = binding.peopleIcon.drawable
                val tempTitle = binding.peopleTitle.text.toString()

                binding.peopleIcon.setImageDrawable(binding.peopleEthIcon.drawable)
                binding.peopleTitle.text = binding.peopleEthTitle.text.toString()

                binding.peopleEthIcon.setImageDrawable(tempIcon)
                binding.peopleEthTitle.text = tempTitle
            }
        }

        binding.secondDigitalBtn.setOnClickListener {
            val centerIcon = binding.peopleIcon.drawable.constantState
            val fiatIcon = binding.peopleFiatIcon.drawable.constantState
            val somIcon = resources.getDrawable(R.drawable.som_icon).constantState

            if (fiatIcon == somIcon) {
                binding.peopleFiatIcon.setImageResource(R.drawable.digital_icon)
                binding.peopleFiatTitle.text = getString(R.string.digital)

                when (binding.peopleTitle.text.toString()) {
                    getString(R.string.usdt) -> {
                        binding.peopleUsdtIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleUsdtTitle.text = getString(R.string.som)
                    }
                    getString(R.string.bitcoin) -> {
                        binding.peopleBitcoinIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleBitcoinTitle.text = getString(R.string.som)
                    }
                    getString(R.string.ethereum) -> {
                        binding.peopleEthIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleEthTitle.text = getString(R.string.som)
                    }
                    else -> {
                        binding.peopleIcon.setImageResource(R.drawable.som_icon)
                        binding.peopleTitle.text = getString(R.string.som)
                    }
                }

                binding.peopleIcon.setImageResource(R.drawable.digital_icon)
                binding.peopleTitle.text = getString(R.string.digital)
            } else {
                val tempIcon = binding.peopleIcon.drawable
                val tempTitle = binding.peopleTitle.text.toString()

                binding.peopleIcon.setImageDrawable(binding.peopleFiatIcon.drawable)
                binding.peopleTitle.text = binding.peopleFiatTitle.text.toString()

                binding.peopleFiatIcon.setImageDrawable(tempIcon)
                binding.peopleFiatTitle.text = tempTitle
            }
        }

//        val mask = MaskImpl.createTerminated(PHONE_NUMBER)
//        val watcher: FormatWatcher = MaskFormatWatcher(mask)
//        watcher.installOn(binding.receiverInput)

        binding.sendBtn.setOnClickListener {
            if (model.transferRes.value is UiState.Loading) {
                return@setOnClickListener
            }

            val sum = binding.sumInput.text.toString().toDoubleOrNull()
            val phone = "+996 (555) 000-000"

            if (sum == null) {
                binding.root.showErrorSnackbar("Введите сумму для перевода")
            } else if (phone.length != PHONE_NUMBER.size) {
                binding.root.showErrorSnackbar("Введите номер телефона получателя")
            } else {
                val tokenBalance =
                    (model.myData.value as? UiState.Success)?.data?.balance?.esomBalance ?: 0.0

                if (tokenBalance < sum) {
                    binding.root.showErrorSnackbar("Недостаточно ЕСом на балансе")
                    return@setOnClickListener
                }

                if (model.transferRes.value !is UiState.Loading) {
                    model.transferToUser(sum, phone.filter { it in "0123456789" })
                }
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