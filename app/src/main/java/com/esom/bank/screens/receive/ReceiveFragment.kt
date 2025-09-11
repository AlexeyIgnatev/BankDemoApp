package com.esom.bank.screens.receive

import QRCodeGenerator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentReceiveBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.settigns.SettingsFragment.Companion.formatPhone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiveFragment : Fragment() {
    private lateinit var binding: FragmentReceiveBinding
    private val args: ReceiveFragmentArgs by navArgs()

    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReceiveBinding.inflate(inflater, container, false)
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
        if(args.currency == CurrencyEnum.SOM)
            binding.contact.text = args.contact.formatPhone()
        else binding.contact.text = args.contact
        if(args.currency != CurrencyEnum.SOM) {
            val qrBitmap = QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = args.contact,
                currency = args.currency
            )
            binding.qrIcon.setImageBitmap(qrBitmap)
        }


        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.copyBtn.setOnClickListener {
            val phone =
                (model.myData.value as? UiState.Success)?.data?.phone ?: return@setOnClickListener
            val clipboard: ClipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(phone, phone)
            clipboard.setPrimaryClip(clip)

            binding.root.showSuccessSnackbar("Номер телефона скопирован")
        }
    }
}