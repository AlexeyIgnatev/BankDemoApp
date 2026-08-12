package com.esom.bank.screens.receive

import QRCodeGenerator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.QrShareUtils
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
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
        val currency = CurrencyEnum.fromNameOrNull(args.currency) ?: CurrencyEnum.SOM

        when (currency) {
            CurrencyEnum.SOM ->
                binding.opinion.text =
                    getString(R.string.receiving_opinion, getString(R.string.som))
            CurrencyEnum.ESOM ->
                binding.opinion.text =
                    getString(R.string.receiving_opinion, getString(R.string.digital))
            CurrencyEnum.USDT_TRC20 ->
                binding.opinion.text =
                    getString(R.string.receiving_opinion, getString(R.string.usdt))
        }
        if (currency == CurrencyEnum.SOM)
            binding.contact.text = args.contact.formatPhone()
        else binding.contact.text = args.contact
        val qrBitmap = QRCodeGenerator.generateCryptoQRCodeWithScheme(
            address = args.contact,
            currency = currency,
            width = 600,
            height = 600
        )
        binding.qrIcon.setImageBitmap(qrBitmap)
        binding.qrIcon.scaleType = ImageView.ScaleType.FIT_CENTER
        binding.qrIcon.scaleX = 1.1f
        binding.qrIcon.scaleY = 1.1f
        binding.qrIcon.adjustViewBounds = true
        if (currency != CurrencyEnum.SOM) {
            binding.copyOpinion.text = getString(R.string.copy_address)
        }


        binding.copyBtn.setOnClickListener {
            val phone =
                (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == currency }?.address
                    ?: return@setOnClickListener
            val clipboard: ClipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(phone, phone)
            clipboard.setPrimaryClip(clip)
            if (currency == CurrencyEnum.SOM)
                binding.root.showSuccessSnackbar(getString(R.string.phone_copy_success))
            else
                binding.root.showSuccessSnackbar(getString(R.string.adres_success_copy))
        }

        binding.shareBtn.setOnClickListener {
            shareQrCode(currency, args.contact)
        }
    }

    private fun shareQrCode(currency: CurrencyEnum, address: String) {
        val user = (model.myData.value as? UiState.Success)?.data
        val shortName = user?.let {
            QrShareUtils.shortUserName(it.firstName, it.middleName, it.lastName)
        }.orEmpty()
        val title = QrShareUtils.buildTitle(currency, shortName)
        val qrBitmap = QrShareUtils.createQrBitmap(address, currency)
        val shareBitmap = QrShareUtils.createShareBitmap(
            context = requireContext(),
            title = title,
            subtitle = null,
            qrBitmap = qrBitmap
        )
        QrShareUtils.shareBitmap(
            context = requireContext(),
            bitmap = shareBitmap,
            fileNamePrefix = "receive_${currency.name.lowercase()}",
            chooserTitle = getString(R.string.share)
        )
    }
}
