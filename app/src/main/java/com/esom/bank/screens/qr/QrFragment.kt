package com.esom.bank.screens.qr

import QRCodeGenerator
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Bitmap
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.AppQrCode
import com.esom.bank.common.utils.QrShareUtils
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.common.utils.views.showToast
import com.esom.bank.databinding.FragmentQrBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.settigns.SettingsFragment.Companion.formatPhone
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class QrFragment : Fragment() {
    private var _binding: FragmentQrBinding? = null
    private val binding: FragmentQrBinding
        get() = _binding ?: error("Binding accessed outside of the view lifecycle")
    private val model: MainViewModel by activityViewModels()
    private val uiModel: QrUiStateViewModel by viewModels()
    private var displayedQrBitmaps: Triple<Bitmap?, Bitmap?, Bitmap?>? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        uiModel.setCameraRequestInFlight(false)
        if (granted && isAdded && _binding != null) {
            startEmbeddedScanner()
        } else if (!granted && isAdded && _binding != null) {
            requireContext().showToast(getString(R.string.qr_camera_permission_required))
            findNavController().navigateUp()
        }
    }

    private val qrGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || _binding == null || !isAdded) return@registerForActivityResult
        val content = decodeQrFromImageUri(uri)
        if (_binding == null || !isAdded) return@registerForActivityResult
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
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.modeToggleGroup.doOnApplyWindowInsets { target, insets, _ ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            target.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = resources.getDimensionPixelSize(R.dimen._10dp) + statusBar
            }
            insets
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

        uiModel.initializePrimaryCurrency(model.getPrimaryCurrency())
        setupPrimaryCurrencyActions()
        setupQrCardActions()
        setupTabs()
        setupScanActions()
        setupEmbeddedScanner()
        observeUserData()
        showScanMode()
    }

    private fun setupTabs() {
        binding.showTabBtn.setOnClickListener {
            if (binding.showContainer.isVisible) showScanMode() else showShowMode()
        }
    }

    private fun setupScanActions() {
        binding.scanGalleryBtn.setOnClickListener { qrGalleryLauncher.launch("image/*") }
        binding.flashlightBtn.setOnClickListener {
            val enabled = uiModel.toggleTorch()
            if (enabled) binding.fullScreenScanner.setTorchOn() else binding.fullScreenScanner.setTorchOff()
            binding.flashlightBtn.alpha = if (enabled) 1f else 0.72f
        }
    }

    private fun setupEmbeddedScanner() {
        binding.fullScreenScanner.statusView.visibility = View.GONE
        binding.fullScreenScanner.viewFinder.visibility = View.GONE
        binding.fullScreenScanner.decodeContinuous { result ->
            if (_binding == null || !isAdded) return@decodeContinuous
            if (uiModel.uiState.value.scanHandled || result.text.isNullOrBlank()) return@decodeContinuous
            uiModel.setScanHandled(true)
            _binding?.fullScreenScanner?.pause()
            handleScannedContent(result.text)
        }
        ensureCameraAndStart()
    }

    private fun ensureCameraAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startEmbeddedScanner()
        } else {
            if (!uiModel.uiState.value.cameraRequestInFlight) {
                uiModel.setCameraRequestInFlight(true)
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startEmbeddedScanner() {
        if (!binding.scanContainer.isVisible) return
        uiModel.setScanHandled(false)
        binding.fullScreenScanner.resume()
    }

    private fun observeUserData() {
        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                val phone = state.data.phone
                val salamAddress = state.data.wallets.find { it.currency == CurrencyEnum.ESOM }?.address.orEmpty()
                val usdtAddress = state.data.wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }?.address.orEmpty()
                uiModel.updateAddresses(phone, salamAddress, usdtAddress)
                if (binding.showContainer.isVisible) renderUserQrCodes(phone, salamAddress, usdtAddress)
            }
        }
    }

    private fun renderUserQrCodes(phone: String, salam: String, usdt: String) {
        uiModel.updateAddresses(phone, salam, usdt)
        val generation = uiModel.nextRenderGeneration()
        viewLifecycleOwner.lifecycleScope.launch {
            val generatedBitmaps = mutableListOf<Bitmap>()
            val bitmaps = try {
                withContext(Dispatchers.Default) {
                    fun generate(address: String, currency: CurrencyEnum): Bitmap {
                        return QRCodeGenerator.generateCryptoQRCodeWithScheme(
                            address = address,
                            currency = currency,
                            width = QR_SIZE,
                            height = QR_SIZE
                        ).also { generatedBitmaps += it }
                    }
                    Triple(
                        if (phone.isBlank()) null else generate(phone, uiModel.uiState.value.primaryCurrency),
                        if (salam.isBlank()) null else generate(salam, CurrencyEnum.ESOM),
                        if (usdt.isBlank()) null else generate(usdt, CurrencyEnum.USDT_TRC20)
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                generatedBitmaps.forEach { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
                throw cancelled
            }
            if (generation != uiModel.uiState.value.renderGeneration) {
                recycleQrBitmaps(bitmaps)
                return@launch
            }
            val currentBinding = _binding ?: run {
                recycleQrBitmaps(bitmaps)
                return@launch
            }
            displayedQrBitmaps?.let(::recycleQrBitmaps)
            displayedQrBitmaps = bitmaps
            bindQrCard(
            image = currentBinding.phoneQrImage,
            value = currentBinding.phoneQrValue,
            text = if (phone.isBlank()) getString(R.string.empty_value) else phone.formatPhone(),
            bitmap = bitmaps.first
            )
            bindQrCard(
            image = currentBinding.salamQrImage,
            value = currentBinding.salamQrValue,
            text = if (salam.isBlank()) getString(R.string.empty_value) else salam,
            bitmap = bitmaps.second
            )
            bindQrCard(
            image = currentBinding.usdtQrImage,
            value = currentBinding.usdtQrValue,
            text = if (usdt.isBlank()) getString(R.string.empty_value) else usdt,
            bitmap = bitmaps.third
            )
            renderPrimaryCurrency()
        }
    }

    private fun setupPrimaryCurrencyActions() {
        binding.phonePrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.SOM) }
        binding.salamPrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.ESOM) }
        binding.usdtPrimaryBtn.setOnClickListener { selectPrimaryCurrency(CurrencyEnum.USDT_TRC20) }
    }

    private fun setupQrCardActions() {
        bindQrCardActions(
            copyButton = binding.phoneQrCopyBtn,
            shareButton = binding.phoneQrShareBtn,
            valueProvider = { uiModel.uiState.value.phone },
            currencyProvider = { uiModel.uiState.value.primaryCurrency },
            copySuccessMessage = getString(R.string.qr_copy_phone)
        )
        bindQrCardActions(
            copyButton = binding.salamQrCopyBtn,
            shareButton = binding.salamQrShareBtn,
            valueProvider = { uiModel.uiState.value.salamAddress },
            currencyProvider = { CurrencyEnum.ESOM },
            copySuccessMessage = getString(R.string.qr_copy_address)
        )
        bindQrCardActions(
            copyButton = binding.usdtQrCopyBtn,
            shareButton = binding.usdtQrShareBtn,
            valueProvider = { uiModel.uiState.value.usdtAddress },
            currencyProvider = { CurrencyEnum.USDT_TRC20 },
            copySuccessMessage = getString(R.string.qr_copy_address)
        )
    }

    private fun bindQrCardActions(
        copyButton: MaterialButton,
        shareButton: MaterialButton,
        valueProvider: () -> String,
        currencyProvider: () -> CurrencyEnum,
        copySuccessMessage: String
    ) {
        copyButton.isEnabled = true
        shareButton.isEnabled = true
        copyButton.setOnClickListener {
            val value = valueProvider().trim()
            if (value.isBlank()) {
                binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
                return@setOnClickListener
            }
            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(value, value))
            binding.root.showSuccessSnackbar(copySuccessMessage)
        }
        shareButton.setOnClickListener {
            val value = valueProvider().trim()
            if (value.isBlank()) {
                binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
                return@setOnClickListener
            }
            shareQrCode(value, currencyProvider())
        }
    }

    private fun selectPrimaryCurrency(currency: CurrencyEnum) {
        uiModel.selectPrimaryCurrency(currency)
        model.setPrimaryCurrency(currency)
        renderPrimaryCurrency()
        if (uiModel.uiState.value.phone.isNotBlank()) {
            renderUserQrCodes(uiModel.uiState.value.phone, uiModel.uiState.value.salamAddress, uiModel.uiState.value.usdtAddress)
        }
    }

    private fun renderPrimaryCurrency() {
        listOf(
            binding.phonePrimaryBtn to CurrencyEnum.SOM,
            binding.salamPrimaryBtn to CurrencyEnum.ESOM,
            binding.usdtPrimaryBtn to CurrencyEnum.USDT_TRC20
        ).forEach { (button, currency) ->
            val selected = currency == uiModel.uiState.value.primaryCurrency
            button.setImageResource(
                if (selected) R.drawable.ic_star_selected else R.drawable.ic_star_unselected
            )
            button.contentDescription = if (selected) {
                "Основная валюта"
            } else {
                "Сделать основной валютой"
            }
        }
        binding.phoneQrTitle.text = "QR для номера телефона (${currencyName(uiModel.uiState.value.primaryCurrency)})"
    }

    private fun currencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun bindQrCard(
        image: ImageView,
        value: android.widget.TextView,
        text: String,
        bitmap: Bitmap?
    ) {
        value.text = text
        if (bitmap != null) {
            image.setImageBitmap(bitmap)
        } else {
            image.setImageResource(R.drawable.qr_code)
        }
    }

    private fun recycleQrBitmaps(bitmaps: Triple<Bitmap?, Bitmap?, Bitmap?>) {
        bitmaps.toList().forEach { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun showScanMode() {
        binding.scanContainer.isVisible = true
        binding.scanFrame.isVisible = true
        binding.showContainer.isVisible = false
        binding.scanTabBtn.isSelected = true
        binding.showTabBtn.isSelected = false
        binding.showTabBtn.text = "Показать QR"
        binding.showTabBtn.backgroundTintList = ColorStateList.valueOf(Color.argb(239, 255, 255, 255))
        binding.showTabBtn.setTextColor(Color.rgb(167, 25, 36))
        binding.showTabBtn.iconTint = ColorStateList.valueOf(Color.rgb(167, 25, 36))
        binding.showTabBtn.setIconResource(R.drawable.ic_qr_scan)
        if (_binding != null) ensureCameraAndStart()
    }

    private fun showShowMode() {
        binding.fullScreenScanner.pause()
        binding.scanFrame.isVisible = false
        binding.scanContainer.isVisible = false
        binding.showContainer.isVisible = true
        binding.scanTabBtn.isSelected = false
        binding.showTabBtn.isSelected = true
        binding.showTabBtn.text = "Сканировать"
        binding.showTabBtn.backgroundTintList = ColorStateList.valueOf(Color.rgb(230, 35, 36))
        binding.showTabBtn.setTextColor(Color.WHITE)
        binding.showTabBtn.iconTint = ColorStateList.valueOf(Color.WHITE)
        binding.showTabBtn.setIconResource(R.drawable.ic_qr_scan)
        binding.showContainer.minimumHeight = binding.root.height
        binding.scrollView.scrollTo(0, 0)
        renderUserQrCodes(uiModel.uiState.value.phone, uiModel.uiState.value.salamAddress, uiModel.uiState.value.usdtAddress)
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

        val direction = NavGraphDirections.startTransferFragment(currency.name, payload.contact)
        if (findNavController().currentDestination?.id == R.id.mainQrFragment) {
            findParentNavController().navigate(direction)
        } else {
            findNavController().navigate(direction)
        }
    }

    private fun decodeQrFromImageUri(uri: android.net.Uri): String? {
        val resolver = context?.contentResolver ?: return null
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = calculateDecodeSampleSize(bounds.outWidth, bounds.outHeight, MAX_QR_IMAGE_DIMENSION)
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = resolver.openInputStream(uri)?.use { input ->
            android.graphics.BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        return runCatching {
            QrShareUtils.decodeQrFromBitmap(bitmap)
        }.getOrNull().also {
            bitmap.recycle()
        }
    }

    private fun calculateDecodeSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > maxDimension) {
            sample *= 2
        }
        return sample
    }

    private fun shareQrCode(address: String, currency: CurrencyEnum) {
        val user = (model.myData.value as? UiState.Success)?.data
        val displayName = user?.let {
            QrShareUtils.shortUserName(it.firstName, it.middleName, it.lastName)
        }.orEmpty()
        val title = QrShareUtils.buildTitle(currency, displayName)
        val qrBitmap = QrShareUtils.createQrBitmap(address, currency)
        try {
            val shareBitmap = QrShareUtils.createShareBitmap(
                context = requireContext(),
                title = title,
                subtitle = null,
                qrBitmap = qrBitmap
            )
            try {
                QrShareUtils.shareBitmap(
                    context = requireContext(),
                    bitmap = shareBitmap,
                    fileNamePrefix = "qr_${currency.name.lowercase()}",
                    chooserTitle = getString(R.string.share)
                )
            } finally {
                shareBitmap.recycle()
            }
        } finally {
            qrBitmap.recycle()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && binding.scanContainer.isVisible) ensureCameraAndStart()
    }

    override fun onPause() {
        if (_binding != null) binding.fullScreenScanner.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.fullScreenScanner?.pause()
        _binding?.fullScreenScanner?.setTorchOff()
        _binding?.let { currentBinding ->
            currentBinding.phoneQrImage.setImageDrawable(null)
            currentBinding.salamQrImage.setImageDrawable(null)
            currentBinding.usdtQrImage.setImageDrawable(null)
        }
        displayedQrBitmaps?.let(::recycleQrBitmaps)
        displayedQrBitmaps = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val QR_SIZE = 360
        private const val MAX_QR_IMAGE_DIMENSION = 2048
    }
}
