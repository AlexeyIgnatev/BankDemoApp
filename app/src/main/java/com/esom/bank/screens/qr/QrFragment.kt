package com.esom.bank.screens.qr

import QRCodeGenerator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
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
import com.esom.bank.common.utils.QrShareUtils
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentQrBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.UserModel
import com.esom.bank.screens.settigns.SettingsFragment.Companion.formatPhone
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QrFragment : Fragment() {
    private lateinit var binding: FragmentQrBinding
    private val model: MainViewModel by activityViewModels()

    private var phone: String = ""
    private var salamAddress: String = ""
    private var usdtAddress: String = ""
    private var currentUser: UserModel? = null
    private var primaryCurrency: CurrencyEnum = CurrencyEnum.SOM

    @Inject
    lateinit var primaryCurrencyStore: PrimaryCurrencyStore

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
        primaryCurrency = primaryCurrencyStore.get()
        setupPrimaryCurrencyActions()
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
                currentUser = state.data
                renderUserQrCodes(state.data.phone, state.data.wallets.find { it.currency == CurrencyEnum.ESOM }?.address.orEmpty(), state.data.wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }?.address.orEmpty())
            }
        }

        (model.myData.value as? UiState.Success)?.data?.let { user ->
            currentUser = user
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
            shareButton = binding.phoneQrShareBtn,
            text = if (phone.isBlank()) getString(R.string.empty_value) else phone.formatPhone(),
            copyText = phone,
            copySuccessMessage = getString(R.string.qr_copy_phone),
            shareCurrency = primaryCurrency,
            bitmap = if (phone.isBlank()) null else QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = phone,
                currency = primaryCurrency,
                width = 600,
                height = 600
            )
        )
        bindQrCard(
            image = binding.salamQrImage,
            value = binding.salamQrValue,
            button = binding.salamQrCopyBtn,
            shareButton = binding.salamQrShareBtn,
            text = if (salam.isBlank()) getString(R.string.empty_value) else salam,
            copyText = salam,
            copySuccessMessage = getString(R.string.qr_copy_address),
            shareCurrency = CurrencyEnum.ESOM,
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
            shareButton = binding.usdtQrShareBtn,
            text = if (usdt.isBlank()) getString(R.string.empty_value) else usdt,
            copyText = usdt,
            copySuccessMessage = getString(R.string.qr_copy_address),
            shareCurrency = CurrencyEnum.USDT_TRC20,
            bitmap = if (usdt.isBlank()) null else QRCodeGenerator.generateCryptoQRCodeWithScheme(
                address = usdt,
                currency = CurrencyEnum.USDT_TRC20,
                width = 600,
                height = 600
            )
        )
        renderPrimaryCurrency()
    }

    private fun setupPrimaryCurrencyActions() {
        binding.phonePrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.SOM) }
        binding.salamPrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.ESOM) }
        binding.usdtPrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.USDT_TRC20) }
    }

    private fun selectPrimaryCurrency(currency: CurrencyEnum) {
        primaryCurrency = currency
        primaryCurrencyStore.set(currency)
        renderPrimaryCurrency()
        if (phone.isNotBlank()) {
            renderUserQrCodes(phone, salamAddress, usdtAddress)
        }
    }

    private fun renderPrimaryCurrency() {
        listOf(
            binding.phonePrimaryBtn to CurrencyEnum.SOM,
            binding.salamPrimaryBtn to CurrencyEnum.ESOM,
            binding.usdtPrimaryBtn to CurrencyEnum.USDT_TRC20
        ).forEach { (button, currency) ->
            val selected = currency == primaryCurrency
            button.setImageResource(
                if (selected) R.drawable.ic_star_selected else R.drawable.ic_star_unselected
            )
            button.contentDescription = if (selected) {
                "Основная валюта"
            } else {
                "Сделать основной валютой"
            }
        }
        binding.phoneQrTitle.text = "QR для номера телефона (${currencyName(primaryCurrency)})"
    }

    private fun currencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun bindQrCard(
        image: ImageView,
        value: android.widget.TextView,
        button: MaterialButton,
        shareButton: MaterialButton,
        text: String,
        copyText: String,
        copySuccessMessage: String,
        shareCurrency: CurrencyEnum,
        bitmap: Bitmap?
    ) {
        value.text = text
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
        } else {
            image.setImageResource(R.drawable.qr_code)
        }
        button.isEnabled = copyText.isNotBlank()
        shareButton.isEnabled = copyText.isNotBlank()
        button.setOnClickListener {
            if (copyText.isBlank()) return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(copyText, copyText))
            binding.root.showSuccessSnackbar(copySuccessMessage)
        }
        shareButton.setOnClickListener {
            if (copyText.isBlank()) return@setOnClickListener
            shareQrCode(copyText, shareCurrency)
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

    private fun decodeQrFromImageUri(uri: android.net.Uri): String? {
        val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input)
        } ?: return null

        return runCatching {
            QrShareUtils.decodeQrFromBitmap(bitmap)
        }.getOrNull().also {
            bitmap.recycle()
        }
    }

    private fun shareQrCode(address: String, currency: CurrencyEnum) {
        val user = currentUser ?: (model.myData.value as? UiState.Success)?.data
        val displayName = user?.let {
            QrShareUtils.shortUserName(it.firstName, it.middleName, it.lastName)
        }.orEmpty()
        val title = QrShareUtils.buildTitle(currency, displayName)
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
            fileNamePrefix = "qr_${currency.name.lowercase()}",
            chooserTitle = getString(R.string.share)
        )
    }
}
