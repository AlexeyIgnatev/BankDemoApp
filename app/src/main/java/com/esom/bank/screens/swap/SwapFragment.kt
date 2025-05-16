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
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.setTextProgrammatically
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentSwapBinding
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SwapFragment : Fragment() {
    private lateinit var binding: FragmentSwapBinding

    private val args: SwapFragmentArgs by navArgs()
    private val model: MainViewModel by activityViewModels()

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

        binding.somInput.setOnUserTextChangeListener {
            updateAmounts(it.toDoubleOrNull(), null)
        }
        binding.esomInput.setOnUserTextChangeListener {
            updateAmounts(null, it.toDoubleOrNull())
        }

        if (args.direction == 0) {
            binding.somLogoCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.green
                )
            )
            binding.somImage.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.black),
                PorterDuff.Mode.SRC_IN
            )
            binding.somSuffix.text = "Сом"
            binding.somCurrencyName.text = "Фиатный\nСом"

            binding.esomLogoCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.blue
                )
            )
            binding.esomImage.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                ), PorterDuff.Mode.SRC_IN
            )
            binding.esomSuffix.text = "ЕСом"
            binding.esomCurrencyName.text = "Електро\nСом"
        } else {
            binding.esomLogoCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.green
                )
            )
            binding.esomImage.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                ), PorterDuff.Mode.SRC_IN
            )
            binding.esomSuffix.text = "Сом"
            binding.esomCurrencyName.text = "Фиатный\nСом"

            binding.somLogoCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.blue
                )
            )
            binding.somImage.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.white),
                PorterDuff.Mode.SRC_IN
            )
            binding.somSuffix.text = "ЕСом"
            binding.somCurrencyName.text = "Електро\nСом"
        }

        binding.swapBtn.setOnClickListener {
            if (model.swapRes.value is UiState.Loading) {
                return@setOnClickListener
            }

            val fromAmount = binding.somInput.text.toString().toDoubleOrNull()
            val toAmount = binding.esomInput.text.toString().toDoubleOrNull()

            if (fromAmount == null && toAmount == null) {
                binding.root.showErrorSnackbar("Введите сумму для обмена")
            } else if (fromAmount != null && toAmount != null) {
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
                    binding.swapText.isVisible = false
                    binding.indicator.isVisible = true
                }

                is UiState.Error -> {
                    binding.root.showErrorSnackbar(it.message)
                    binding.swapText.isVisible = true
                    binding.indicator.isVisible = false
                }

                is UiState.Success -> {
                    binding.root.showSuccessSnackbar("Обмен совершён")
                    binding.swapText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun updateAmounts(fromAmount: Double?, toAmount: Double?) {
        val platformFee =
            (model.myData.value as? UiState.Success)?.data?.platformFee ?: 0.0

        if (fromAmount == null && toAmount == null) {
            binding.somInput.setTextProgrammatically("")
            binding.esomInput.setTextProgrammatically("")
        } else if (fromAmount != null) {
            val newToAmount = fromAmount * (1 - platformFee)
            val newSomText = fromAmount.format(2)
            if (newSomText != binding.somInput.text.toString() && !binding.somInput.text.toString()
                    .endsWith(".")
            ) {
                binding.somInput.setTextProgrammatically(fromAmount.format(2))
                binding.somInput.setSelection(fromAmount.format(2).length)
            }
            binding.esomInput.setTextProgrammatically(newToAmount.format(2))
            binding.esomInput.setSelection(newToAmount.format(2).length)
        } else if (toAmount != null) {
            val newFromAmount = toAmount / (1 - platformFee)
            val newEsomTExt = toAmount.format(2)
            if (newEsomTExt != binding.esomInput.text.toString() && !binding.esomInput.text.toString()
                    .endsWith(".")
            ) {
                binding.esomInput.setTextProgrammatically(toAmount.format(2))
                binding.esomInput.setSelection(toAmount.format(2).length)
            }
            binding.somInput.setTextProgrammatically(newFromAmount.format(2))
            binding.somInput.setSelection(newFromAmount.format(2).length)
        }
    }
}