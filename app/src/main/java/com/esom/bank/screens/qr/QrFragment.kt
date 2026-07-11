package com.esom.bank.screens.qr

import QRCodeGenerator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.AppQrCode
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentQrBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.settigns.SettingsFragment.Companion.formatPhone
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QrFragment : Fragment() {
    private lateinit var binding: FragmentQrBinding
    private val model: MainViewModel by activityViewModels()

    private var phone: String = ""
    private var salamAddress: String = ""
    private var usdtAddress: String = ""

    private val qrCameraLauncher = registerForActivityResult(ScanContract()) { result ->
        handleScannedContent(result.contents)
    }

    private val qrGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val content = decodeQrFromImageUri(uri)
        if (content.isNullOrBlank()) {
            binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
        } else {
            handleScannedContent(content)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { target, insets, rect ->
            target.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        setupTabs()
        setupScanActions()
        observeUserData()
        showScanMode()
    }

    private fun setupTabs() {
        binding.modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.scan_tab_btn -> showScanMode()
                R.id.show_tab_btn -> showShowMode()
            }
        }

        binding.modeToggleGroup.check(R.id.scan_tab_btn)
    }

    private fun setupScanActions() {
        binding.scanCameraBtn.setOnClickListener { startCameraQrScan() }
        binding.scanGalleryBtn.setOnClickListener { qrGalleryLauncher.launch("image/*") }
    }

    private fun observeUserData() {
        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                renderUserQrCodes(state.data.phone, state.data.wallets.find { it.currency == CurrencyEnum.ESOM }?.address.orEmpty(), state.data.wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }?.address.orEmpty())
            }
        }

        (model.myData.value as? UiState.Success)?.data?.let { user ->
            renderUserQrCodes(
                phone = user.phone,
                salam = user.wallets.find { it.currency == CurrencyEnum.ESOM }?.address.orEmpty(),
                usdt = user.wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }?.address.orEmpty()
            )
        }
    }

    private fun renderUserQrCodes(phone: String, salam: String, usdt: String) {
        this.phone = phone
        this.salamAddress = salam
        this.usdtAddress = usdt

        bindQrCard(
            image = binding.phoneQrImage,
            value = binding.phoneQrValue,
            button = binding.phoneQrCopyBtn,
            text = if (phone.isBlank()) getString(R.string.empty_value) else phone.formatPhone(),
            copyText = phone,
            copySuccessMessage = getString(R.string.qr_copy_phone),
            bitmap = if (phone.isBlank()) null else QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = phone,
                currency = CurrencyEnum.SOM,
                width = 600,
                height = 600
            )
        )
        bindQrCard(
            image = binding.salamQrImage,
            value = binding.salamQrValue,
            button = binding.salamQrCopyBtn,
            text = if (salam.isBlank()) getString(R.string.empty_value) else salam,
            copyText = salam,
            copySuccessMessage = getString(R.string.qr_copy_address),
            bitmap = if (salam.isBlank()) null else QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = salam,
                currency = CurrencyEnum.ESOM,
                width = 600,
                height = 600
            )
        )
        bindQrCard(
            image = binding.usdtQrImage,
            value = binding.usdtQrValue,
            button = binding.usdtQrCopyBtn,
            text = if (usdt.isBlank()) getString(R.string.empty_value) else usdt,
            copyText = usdt,
            copySuccessMessage = getString(R.string.qr_copy_address),
            bitmap = if (usdt.isBlank()) null else QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = usdt,
                currency = CurrencyEnum.USDT_TRC20,
                width = 600,
                height = 600
            )
        )
    }

    private fun bindQrCard(
        image: ImageView,
        value: android.widget.TextView,
        button: MaterialButton,
        text: String,
        copyText: String,
        copySuccessMessage: String,
        bitmap: Bitmap?
    ) {
        value.text = text
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
        } else {
            image.setImageResource(R.drawable.qr_code)
        }
        button.isEnabled = copyText.isNotBlank()
        button.setOnClickListener {
            if (copyText.isBlank()) return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(copyText, copyText))
            binding.root.showSuccessSnackbar(copySuccessMessage)
        }
    }

    private fun showScanMode() {
        binding.scanContainer.isVisible = true
        binding.showContainer.isVisible = false
        binding.scanTabBtn.isSelected = true
        binding.showTabBtn.isSelected = false
    }

    private fun showShowMode() {
        binding.scanContainer.isVisible = false
        binding.showContainer.isVisible = true
        binding.scanTabBtn.isSelected = false
        binding.showTabBtn.isSelected = true
    }

    private fun startCameraQrScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt(getString(R.string.scan_qr))
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        qrCameraLauncher.launch(options)
    }

    private fun handleScannedContent(rawContent: String?) {
        val payload = AppQrCode.parsePayload(rawContent.orEmpty())
        if (payload == null) {
            binding.root.showErrorSnackbar(getString(R.string.qr_invalid_scan))
            return
        }

        val currency = payload.currency ?: run {
            binding.root.showErrorSnackbar(getString(R.string.qr_invalid_scan))
            return
        }

        findParentNavController().navigate(
            NavGraphDirections.startTransferFragment(currency.name, payload.contact)
        )
    }

    private fun decodeQrFromImageUri(uri: Uri): String? {
        val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        return runCatching {
            decodeQrFromBitmap(bitmap)
        }.getOrNull().also {
            bitmap.recycle()
        }
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        return MultiFormatReader().decode(binaryBitmap, hints).text
    }
}
