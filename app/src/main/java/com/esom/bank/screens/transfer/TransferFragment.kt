package com.esom.bank.screens.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentTransferBinding
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

        val mask = MaskImpl.createTerminated(PHONE_NUMBER)
        val watcher: FormatWatcher = MaskFormatWatcher(mask)
        watcher.installOn(binding.receiverInput)

        binding.sendBtn.setOnClickListener {
            val sum = binding.somInput.text.toString().toDoubleOrNull()
            val phone = binding.receiverInput.text.toString()

            if (sum == null) {
                binding.root.showErrorSnackbar("Введите сумму для перевода")
            } else if (phone.length != PHONE_NUMBER.size) {
                binding.root.showErrorSnackbar("Введите номер телефона получателя")
            } else {
                val tokenBalance = (model.tokenBalance.value as? UiState.Success)?.data ?: 0.0

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
                is UiState.Loading -> {}

                is UiState.Error -> {
                    binding.root.showErrorSnackbar(it.message)
                }

                is UiState.Success -> {
                    binding.root.showSuccessSnackbar("Перевод совершён")
                    findNavController().popBackStack()
                }
            }
        }
    }
}