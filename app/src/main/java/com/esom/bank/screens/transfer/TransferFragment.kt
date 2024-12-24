package com.esom.bank.screens.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentTransferBinding
import dagger.hilt.android.AndroidEntryPoint
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot
import ru.tinkoff.decoro.watchers.FormatWatcher
import ru.tinkoff.decoro.watchers.MaskFormatWatcher


@AndroidEntryPoint
class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    val PHONE_NUMBER: Array<Slot> = arrayOf(
        PredefinedSlots.hardcodedSlot('+'),
        PredefinedSlots.hardcodedSlot('9'),
        PredefinedSlots.hardcodedSlot('9'),
        PredefinedSlots.hardcodedSlot('6'),
        PredefinedSlots.hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.hardcodedSlot('(').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.hardcodedSlot(')').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.hardcodedSlot('-').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
        PredefinedSlots.hardcodedSlot('-').withTags(Slot.TAG_DECORATION),
        PredefinedSlots.digit(),
        PredefinedSlots.digit(),
    )

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
                binding.root.showSuccessSnackbar("Перевод совершён")
                findNavController().popBackStack()
            }
        }
    }
}