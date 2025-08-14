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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SwapFragment : Fragment() {
    private lateinit var binding: FragmentSwapBinding

    private val args: SwapFragmentArgs by navArgs()
    private val model: MainViewModel by activityViewModels()
    private var isPanelShown = false
    private var isPeoplePanelShown = false

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

        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

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

        binding.peopleLayout.setOnClickListener {
            if(isPeoplePanelShown) {
                it.elevation = 0f
                slideOut(binding.peopleCurrencyLayout)
            } else {
                it.elevation = 20f
                binding.peopleCurrencyLayout.visibility = View.VISIBLE
                slideIn(binding.peopleCurrencyLayout)
            }
            isPeoplePanelShown = !isPeoplePanelShown
        }

        binding.sumInput.setOnUserTextChangeListener {
            updateAmounts(it.toDoubleOrNull(), null)
        }

        binding.sendBtn.setOnClickListener {
            if (model.swapRes.value is UiState.Loading) {
                return@setOnClickListener
            }

            val fromAmount = binding.sumInput.text.toString().toDoubleOrNull()

            if (fromAmount == null) {
                binding.root.showErrorSnackbar("Введите сумму для обмена")
            } else {
                if (args.direction == 0) {
                    val somBalance =
                        (model.myData.value as? UiState.Success)?.data?.balance?.somBalance ?: 0.0
                    if (fromAmount <= somBalance) {
                        if (model.swapRes.value !is UiState.Loading) {
                            model.transferFromFiat(fromAmount)
                        }
                    } else {
                        binding.root.showErrorSnackbar("Недостаточно Сом на балансе")
                    }
                } else {
                    val tokenBalance =
                        (model.myData.value as? UiState.Success)?.data?.balance?.esomBalance ?: 0.0
                    if (fromAmount <= tokenBalance) {
                        if (model.swapRes.value !is UiState.Loading) {
                            model.transferToFiat(fromAmount)
                        }
                    } else {
                        binding.root.showErrorSnackbar("Недостаточно ЕСом на балансе")
                    }
                }
            }
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