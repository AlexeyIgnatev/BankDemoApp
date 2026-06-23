package com.esom.bank.screens.transfer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
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
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.applyKyrgyzPhoneMask
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.isCompleteKyrgyzPhone
import com.esom.bank.common.utils.views.kyrgyzPhoneDigits
import com.esom.bank.common.utils.views.setOnUserTextChangeListener
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentTransferBinding
import com.esom.bank.screens.main.dialog.TransferConfirmationFragment
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.SuccessOperationModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding
    private val model: MainViewModel by activityViewModels()
    private var isPanelShown = false
    private var phoneMaskWatcher: TextWatcher? = null
    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var isToPhoneNumber = false
    private val args: TransferFragmentArgs by navArgs()
    private val qrCameraLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (content.isNullOrBlank()) {
            binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
        } else {
            fillContactFromQr(content)
        }
    }
    private val qrGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val content = decodeQrFromImageUri(uri)
        if (content.isNullOrBlank()) {
            binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
        } else {
            fillContactFromQr(content)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if (imeBottom == 0) {
                    systemBarsBottom
                } else {
                    imeBottom
                }
            )
            insets
        }
        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().popBackStack()
        }

        currentFromCurrency = CurrencyEnum.fromNameOrNull(args.currency) ?: CurrencyEnum.SOM
        isToPhoneNumber = currentFromCurrency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
        updateCurrencyIcon(currentFromCurrency)
        setContactHint()
        setupChangeButton()
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        model.getFees()
        initInitialBalances()
        setupQuickAmounts()
        binding.sumInput.setOnUserTextChangeListener { text ->
            updateCommissionAndTotal(text.toString())
        }

        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.currentCurrencyLayout.setOnClickListener { toggleCurrencyPanel() }
        binding.qrScanBtn.setOnClickListener { showQrSourceDialog() }

        binding.firstUsdtBtn.setOnClickListener { selectCurrency(CurrencyEnum.USDT_TRC20) }
        binding.firstDigitalBtn.setOnClickListener { selectCurrency(CurrencyEnum.ESOM) }
        binding.firstSomBtn.setOnClickListener { selectCurrency(CurrencyEnum.SOM) }

        binding.sendBtn.setOnClickListener { handleTransferButtonClick() }
        setupTransferConfirmationResultListener()

        model.myData.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateWalletBalances()
                initInitialBalances()
                updateCommissionAndTotal(binding.sumInput.text.toString())
            }
        }

        model.fees.observe(viewLifecycleOwner) {
            if (it is UiState.Success) {
                updateCommissionAndTotal(binding.sumInput.text.toString())
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
                    findNavController().navigate(
                        NavGraphDirections.startFailTransferFragment(it.message)
                    )
                }

                is UiState.Success -> {
                    model.updateUserData()
                    model.updateLastSuccessOperationReceipt(
                        transactionId = it.data.transactionId,
                        receiptNumber = it.data.receiptNumber
                    )
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
                }

                else -> {}
            }
        }
    }

    private fun setupTransferConfirmationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            TransferConfirmationFragment.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (!bundle.getBoolean(TransferConfirmationFragment.CONFIRMED_KEY)) return@setFragmentResultListener
            if (bundle.getString(TransferConfirmationFragment.OPERATION_KEY) != OPERATION_TRANSFER) {
                return@setFragmentResultListener
            }

            val amount = bundle.getDouble(TransferConfirmationFragment.AMOUNT_KEY)
            val currencyName = bundle.getString(TransferConfirmationFragment.FROM_CURRENCY_KEY).orEmpty()
            val currency = CurrencyEnum.fromNameOrNull(currencyName)
                ?: return@setFragmentResultListener
            val phone = bundle.getString(TransferConfirmationFragment.PHONE_KEY).orEmpty()
            val address = bundle.getString(TransferConfirmationFragment.ADDRESS_KEY)

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = currency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.transfer),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty(),
                    receiptNumber = "",
                    fee = calculateTransferCommission(amount, currency)
                )
            )
            model.transferToUser(amount, phone, address, currency)
        }
    }

    private fun setupChangeButton() {
        val isCryptoCurrency = currentFromCurrency == CurrencyEnum.USDT_TRC20
        binding.changeLayout.isVisible = isCryptoCurrency
        binding.changeLayout.setOnClickListener {
            if (currentFromCurrency == CurrencyEnum.USDT_TRC20) {
                isToPhoneNumber = !isToPhoneNumber
                updateContactType()
            }
        }
    }

    private fun updateContactType() {
        setContactHint()
        binding.contact.setText("")
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun showQrSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.qr_scan_source_title)
            .setItems(
                arrayOf(
                    getString(R.string.qr_scan_camera),
                    getString(R.string.qr_scan_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> startCameraQrScan()
                    1 -> qrGalleryLauncher.launch("image/*")
                }
            }
            .show()
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

    private fun fillContactFromQr(rawContent: String) {
        val contact = normalizeQrContact(rawContent)
        if (!isValidQrContact(contact)) {
            binding.root.showErrorSnackbar(getString(R.string.qr_scan_empty))
            return
        }
        applyContactInputModeForQr(contact)
        binding.contact.setText(contact)
        binding.contact.setSelection(binding.contact.text?.length ?: 0)
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun normalizeQrContact(rawContent: String): String {
        val value = rawContent.trim()
        parseInternalQrContact(value)?.let { return it }

        val schemeMatch = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):(.*)$")
            .find(value)
        if (schemeMatch != null) {
            val scheme = schemeMatch.groupValues[1].lowercase(Locale.US)
            val payload = schemeMatch.groupValues[2]
                .removePrefix("//")
                .substringBefore("?")
                .substringBefore("&")
                .trim()
            if (scheme in QR_CONTACT_SCHEMES && payload.isNotBlank()) {
                return payload
            }
        }

        val uri = runCatching { Uri.parse(value) }.getOrNull()
        val scheme = uri?.scheme?.lowercase(Locale.US)
        val queryContact = if (uri?.isHierarchical == true) {
            firstNotBlank(
                uri.getQueryParameter("address"),
                uri.getQueryParameter("to"),
                uri.getQueryParameter("phone")
            )
        } else {
            ""
        }
        if (queryContact.isNotBlank()) return queryContact

        return when (scheme) {
            "usdt", "tether", "tel" -> {
                uri.schemeSpecificPart
                    ?.removePrefix("//")
                    ?.substringBefore("?")
                    ?.substringBefore("&")
                    ?.trim()
                    .orEmpty()
            }
            else -> value
        }
    }

    private fun parseInternalQrContact(value: String): String? {
        if (!value.startsWith(APP_QR_PREFIX)) return null
        return value
            .split("|")
            .firstOrNull { it.startsWith("$APP_QR_CONTACT_KEY=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun applyContactInputModeForQr(contact: String) {
        when {
            isQrPhoneContact(contact) -> {
                if (!isToPhoneNumber) {
                    isToPhoneNumber = true
                    applyPhoneMask()
                    setContactHint()
                }
            }
            currentFromCurrency != CurrencyEnum.SOM -> {
                isToPhoneNumber = false
                removePhoneMask()
                setContactHint()
            }
        }
    }

    private fun isValidQrContact(contact: String): Boolean {
        val compact = contact.trim()
        if (compact.isBlank()) return false
        if (compact.equals("test", ignoreCase = true) || compact.equals("\u0442\u0435\u0441\u0442", ignoreCase = true)) {
            return false
        }
        return isQrPhoneContact(compact) || isQrWalletContact(compact)
    }

    private fun isQrPhoneContact(contact: String): Boolean {
        val digitsCount = contact.count { it.isDigit() }
        return digitsCount >= MIN_QR_PHONE_DIGITS && !isQrWalletContact(contact)
    }

    private fun isQrWalletContact(contact: String): Boolean {
        val compact = contact.trim()
        return compact.length >= MIN_QR_WALLET_LENGTH &&
            compact.any { it.isLetter() } &&
            compact.any { it.isDigit() }
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

    private fun firstNotBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

    private fun updateWalletBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone
        fun getSuffix(currency: CurrencyEnum, walletAddress: String?): String {
            return when (currency) {
                CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
                else -> walletAddress?.takeLast(3)?.let { "*$it" } ?: ""
            }
        }

        val walletUSDT = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val walletESOM = wallets.find { it.currency == CurrencyEnum.ESOM }
        val walletSOM = wallets.find { it.currency == CurrencyEnum.SOM }

        binding.firstUsdtBtn.visibility = View.VISIBLE
        binding.firstDigitalBtn.visibility = View.VISIBLE
        binding.firstSomBtn.visibility = View.VISIBLE
        binding.firstBitcoinBtn.visibility = View.GONE
        binding.firstEthBtn.visibility = View.GONE

        binding.usdtIcon.visibility = View.VISIBLE
        binding.usdtTitle.visibility = View.VISIBLE
        binding.usdt.visibility = View.VISIBLE
        binding.usdtView.visibility = View.VISIBLE

        binding.bitcoinIcon.visibility = View.GONE
        binding.bitcoinTitle.visibility = View.GONE
        binding.bitcoin.visibility = View.GONE
        binding.bitcoinView.visibility = View.GONE

        binding.ethIcon.visibility = View.GONE
        binding.ethTitle.visibility = View.GONE
        binding.eth.visibility = View.GONE
        binding.ethView.visibility = View.GONE

        binding.fiatIcon.visibility = View.VISIBLE
        binding.fiatTitle.visibility = View.VISIBLE
        binding.fiat.visibility = View.VISIBLE
        binding.fiatView.visibility = View.VISIBLE

        binding.currencySomIcon.visibility = View.VISIBLE
        binding.somTitle.visibility = View.VISIBLE
        binding.som.visibility = View.VISIBLE

        binding.secondUsdtBtn.visibility = View.VISIBLE
        binding.secondBitcoinBtn.visibility = View.VISIBLE
        binding.secondDigitalBtn.visibility = View.VISIBLE
        binding.secondEthBtn.visibility = View.GONE

        binding.peopleUsdtIcon.visibility = View.VISIBLE
        binding.peopleUsdtTitle.visibility = View.VISIBLE
        binding.peopleUsdt.visibility = View.VISIBLE
        binding.peopleUsdtView.visibility = View.VISIBLE

        binding.peopleBitcoinIcon.visibility = View.VISIBLE
        binding.peopleBitcoinTitle.visibility = View.VISIBLE
        binding.peopleBitcoin.visibility = View.VISIBLE
        binding.peopleBitcoinView.visibility = View.VISIBLE

        binding.peopleEthIcon.visibility = View.GONE
        binding.peopleEthTitle.visibility = View.GONE
        binding.peopleEth.visibility = View.GONE
        binding.peopleEthView.visibility = View.GONE

        binding.peopleFiatIcon.visibility = View.VISIBLE
        binding.peopleFiatTitle.visibility = View.VISIBLE
        binding.peopleFiat.visibility = View.VISIBLE

        binding.usdt.text = getSuffix(CurrencyEnum.USDT_TRC20, walletUSDT?.address)
        binding.fiat.text = getSuffix(CurrencyEnum.ESOM, walletESOM?.address)
        binding.som.text = getSuffix(CurrencyEnum.SOM, walletSOM?.address)
        binding.bitcoin.text = ""
        binding.eth.text = ""
        val currentWallet = wallets.find { it.currency == currentFromCurrency }
        binding.sum.text =
            currentWallet?.balance?.formatBalanceNew() ?: "0"
        binding.currencyTitle.text = getCurrencyName(currentFromCurrency)
        binding.currency.text = getSuffix(currentFromCurrency, currentWallet?.address)

        binding.peopleTitle.text = getCurrencyName(currentFromCurrency)
        binding.peopleIcon.setImageResource(
            when (currentFromCurrency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )
        binding.peopleUsdt.text = getSuffix(CurrencyEnum.USDT_TRC20, walletUSDT?.address)
        binding.peopleBitcoin.text = getSuffix(CurrencyEnum.ESOM, walletESOM?.address)
        binding.peopleFiat.text = getSuffix(CurrencyEnum.SOM, walletSOM?.address)
        binding.peopleEth.text = ""
    }

    private fun setContactHint() {
        binding.contact.hint = if (isToPhoneNumber) {
            "Введите номер телефона"
        } else {
            "Введите адрес кошелька"
        }
    }

    private fun setupQuickAmounts() {
        listOf(
            binding.sum50Layout to "50",
            binding.sum100Layout to "100",
            binding.sum1000Layout to "1000",
            binding.sum10000Layout to "10000"
        ).forEach { (layout, value) ->
            layout.setOnClickListener {
                binding.sumInput.setText(value)
                updateCommissionAndTotal(value)
            }
        }
    }

    private fun initInitialBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val walletUSDT = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val walletESOM = wallets.find { it.currency == CurrencyEnum.ESOM }
        val walletSOM = wallets.find { it.currency == CurrencyEnum.SOM }
        binding.sum1.text =
            walletUSDT?.balance?.formatBalanceNew() ?: "0"
        binding.sum2.text =
            walletESOM?.balance?.formatBalanceNew() ?: "0"
        binding.sum3.text =
            walletSOM?.balance?.formatBalanceNew() ?: "0"
        updateWalletBalances()
    }

    private fun toggleCurrencyPanel() {
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

    private fun selectCurrency(currency: CurrencyEnum) {
        toggleCurrency(currency)
        if (isPanelShown) toggleCurrencyPanel()
    }

    private fun toggleCurrency(currency: CurrencyEnum) {
        val wasToPhoneNumber = isToPhoneNumber
        currentFromCurrency = currency
        isToPhoneNumber = currency in listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
        if (wasToPhoneNumber != isToPhoneNumber) {
            binding.contact.setText("")
        }
        updateCurrencyIcon(currency)
        updateWalletBalances()
        updateCommissionAndTotal(binding.sumInput.text.toString())
        setupChangeButton()
        setContactHint()
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
    }

    private fun updateCurrencyIcon(currency: CurrencyEnum) {
        when (currency) {
            CurrencyEnum.SOM -> setCurrencyUI(R.drawable.som_icon, getString(R.string.som), true)
            CurrencyEnum.ESOM -> setCurrencyUI(
                R.drawable.salam_icon,
                getString(R.string.digital),
                false
            )

            CurrencyEnum.USDT_TRC20 -> setCurrencyUI(
                R.drawable.usdt_icon,
                getString(R.string.usdt),
                false
            )
        }
    }

    private fun setCurrencyUI(iconRes: Int, title: String, showSomIcons: Boolean) {
        binding.icon.setImageResource(iconRes)
        binding.currencyTitle.text = title
        binding.comissionSomIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.totalSomIcon.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon50.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon100.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon1000.visibility = if (showSomIcons) View.VISIBLE else View.GONE
        binding.somIcon10000.visibility = if (showSomIcons) View.VISIBLE else View.GONE
    }

    private fun updateCommissionAndTotal(amountText: String) {
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val commission = calculateTransferCommission(amount, currentFromCurrency)

        val totalAmount = amount - commission

        binding.comissionValue.text = commission.formatBalanceNew()
        binding.total.text = formatTotalAmount(totalAmount)
    }

    private fun calculateTransferCommission(amount: Double, currency: CurrencyEnum): Double {
        if (amount <= 0.0) return 0.0
        return model.calculateTransferFee(amount, currency)
    }

    private fun formatTotalAmount(amount: Double): String {
        return if (amount % 1 == 0.0) {
            amount.toLong().toString()
        } else {
            amount.formatBalanceNew()
        }
    }

    private fun applyPhoneMask() {
        val editText = binding.contact
        phoneMaskWatcher?.let { editText.removeTextChangedListener(it) }
        phoneMaskWatcher = null

        editText.setText("")
        editText.setHorizontallyScrolling(false)
        editText.isSingleLine = false
        editText.maxLines = 3

        phoneMaskWatcher = editText.applyKyrgyzPhoneMask()
    }

    private fun removePhoneMask() {
        phoneMaskWatcher?.let { binding.contact.removeTextChangedListener(it) }
        phoneMaskWatcher = null
        binding.contact.setText("")
        binding.contact.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        binding.contact.filters = arrayOf()

        binding.contact.setHorizontallyScrolling(false)
        binding.contact.isSingleLine = false
        binding.contact.maxLines = 3
    }

    private fun handleTransferButtonClick() {
        if (model.transferRes.value is UiState.Loading) return
        val sum = binding.sumInput.text.toString().toDoubleOrNull()
        val contactInfo = binding.contact.text.toString().trim()
        if (sum == null) {
            binding.root.showErrorSnackbar("Введите сумму для перевода")
            return
        }

        when {
            isToPhoneNumber -> {
                if (!contactInfo.isCompleteKyrgyzPhone()) {
                    binding.root.showErrorSnackbar("Введите корректный номер телефона")
                    return
                }
            }

            else -> {
                if (contactInfo.isEmpty()) {
                    binding.root.showErrorSnackbar("Введите адрес получателя")
                    return
                }
                if (contactInfo.length < 20) {
                    binding.root.showErrorSnackbar("Адрес слишком короткий")
                    return
                }
            }
        }

        val walletBalance =
            (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == currentFromCurrency }?.balance
                ?: 0.0
        if (sum > walletBalance) {
            val currencyName = getCurrencyName(currentFromCurrency)
            binding.root.showErrorSnackbar("Недостаточно $currencyName на балансе")
            return
        }

        val phone = if (isToPhoneNumber) contactInfo.kyrgyzPhoneDigits() else null
        val address = if (!isToPhoneNumber) contactInfo else null

        if (model.transferRes.value !is UiState.Loading) {
            val recipient = if (isToPhoneNumber) {
                contactInfo
            } else {
                address.orEmpty()
            }
            showTransferConfirmation(
                amount = sum,
                phone = phone.orEmpty(),
                address = address,
                recipient = recipient
            )
        }
    }

    private fun showTransferConfirmation(
        amount: Double,
        phone: String,
        address: String?,
        recipient: String
    ) {
        val amountText = "${amount.formatBalanceNew()} ${getCurrencyName(currentFromCurrency)}"
        parentFragmentManager.setFragmentResult(
            TransferConfirmationFragment.DATA_REQUEST_KEY,
            bundleOf(
                TransferConfirmationFragment.TITLE_KEY to getString(
                    R.string.transfer_confirmation_message,
                    amountText,
                    recipient
                ),
                TransferConfirmationFragment.OPERATION_KEY to OPERATION_TRANSFER,
                TransferConfirmationFragment.AMOUNT_KEY to amount,
                TransferConfirmationFragment.FROM_CURRENCY_KEY to currentFromCurrency.name,
                TransferConfirmationFragment.PHONE_KEY to phone,
                TransferConfirmationFragment.ADDRESS_KEY to address,
                TransferConfirmationFragment.OPERATION_TITLE_KEY to getString(R.string.transfer),
                TransferConfirmationFragment.PAID_FROM_KEY to getCurrentUserAccountForSuccess(),
                TransferConfirmationFragment.RECIPIENT_KEY to recipient
            )
        )
        findNavController().navigate(NavGraphDirections.startTransferConfirmationFragment())
    }

    private fun getCurrentUserAccountForSuccess(): String {
        val user = (model.myData.value as? UiState.Success)?.data
        val walletAddress = user?.wallets?.firstOrNull { it.currency == currentFromCurrency }?.address.orEmpty()
        return when {
            currentFromCurrency == CurrencyEnum.SOM -> user?.phone.orEmpty()
            walletAddress.isNotBlank() -> walletAddress
            else -> user?.phone.orEmpty()
        }
    }

    companion object {
        private const val OPERATION_TRANSFER = "transfer"
        private const val APP_QR_PREFIX = "ESOM_BANK_QR"
        private const val APP_QR_CONTACT_KEY = "contact"
        private const val MIN_QR_PHONE_DIGITS = 7
        private const val MIN_QR_WALLET_LENGTH = 20
        private val QR_CONTACT_SCHEMES = setOf("usdt", "tether", "tel")
    }

    private fun getCurrencyName(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun slideIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.post {
            view.translationY = view.height.toFloat()
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(450)
                .start()
        }
    }

    private fun slideOut(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(450)
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }
}
