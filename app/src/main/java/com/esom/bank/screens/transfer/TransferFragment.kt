package com.esom.bank.screens.transfer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import com.esom.bank.common.utils.AppQrCode
import com.esom.bank.common.utils.QrShareUtils
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
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.templates.data.RecentTemplateStore
import com.esom.bank.screens.templates.data.TransferTemplate
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TransferFragment : Fragment() {
    private lateinit var binding: FragmentTransferBinding
    private val model: MainViewModel by activityViewModels()
    private var isPanelShown = false
    private var phoneMaskWatcher: TextWatcher? = null
    private var currentFromCurrency: CurrencyEnum = CurrencyEnum.ESOM
    private var isToPhoneNumber = false
    private var currencyPanelOptions: List<CurrencyEnum> = emptyList()
    private var pendingTemplate: TransferTemplate? = null
    private val args: TransferFragmentArgs by navArgs()

    @Inject
    lateinit var recentTemplateStore: RecentTemplateStore
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
        updateCurrencyOptionsPanel()
        setContactHint()
        setupChangeButton()
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }
        if (args.contact.isNotBlank()) {
            binding.contact.setText(args.contact)
            binding.contact.setSelection(binding.contact.text?.length ?: 0)
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

        binding.firstUsdtBtn.setOnClickListener {
            if (CurrencyEnum.USDT_TRC20 in currencyPanelOptions) {
                selectCurrency(CurrencyEnum.USDT_TRC20)
            }
        }
        binding.firstDigitalBtn.setOnClickListener {
            if (CurrencyEnum.ESOM in currencyPanelOptions) {
                selectCurrency(CurrencyEnum.ESOM)
            }
        }
        binding.firstSomBtn.setOnClickListener {
            if (CurrencyEnum.SOM in currencyPanelOptions) {
                selectCurrency(CurrencyEnum.SOM)
            }
        }

        binding.sendBtn.setOnClickListener { handleTransferButtonClick() }
        setupTransferConfirmationResultListener()
        renderTemplates()

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
                    pendingTemplate = null
                    binding.sendText.isVisible = true
                    binding.indicator.isVisible = false
                    findNavController().navigate(
                        NavGraphDirections.startFailTransferFragment(it.message)
                    )
                }

                is UiState.Success -> {
                    pendingTemplate?.let(recentTemplateStore::addTransferTemplate)
                    pendingTemplate = null
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

    private fun updateCurrencyOptionsPanel() {
        updateWalletBalances()
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
            val recipient = bundle.getString(TransferConfirmationFragment.RECIPIENT_KEY).orEmpty()
            val fee = bundle.getDouble(TransferConfirmationFragment.FEE_KEY)
            val totalDebited = bundle.getDouble(TransferConfirmationFragment.TOTAL_DEBITED_KEY)

            pendingTemplate = TransferTemplate(
                amount = amount,
                currency = currency.name,
                recipient = recipient,
                isPhone = phone.isNotBlank()
            )

            model.setLastSuccessOperation(
                SuccessOperationModel(
                    amount = amount,
                    currency = currency,
                    operationTitle = bundle.getString(TransferConfirmationFragment.OPERATION_TITLE_KEY)
                        ?: getString(R.string.transfer),
                    paidFromAccount = bundle.getString(TransferConfirmationFragment.PAID_FROM_KEY).orEmpty(),
                    recipient = recipient,
                    receiptNumber = "",
                    fee = fee,
                    creditedAmount = amount,
                    totalDebitedAmount = totalDebited
                )
            )
            model.transferToUser(amount, phone, address, currency)
        }
    }

    private fun renderTemplates() {
        val templates = recentTemplateStore.getTransferTemplates()
        binding.templatesSection.isVisible = templates.isNotEmpty()
        binding.templatesContainer.removeAllViews()

        templates.forEach { template ->
            val currency = CurrencyEnum.fromNameOrNull(template.currency) ?: return@forEach
            val label = buildString {
                append(template.recipient)
                append('\n')
                append(formatTemplateAmount(template.amount))
                append(' ')
                append(getCurrencyName(currency))
            }
            binding.templatesContainer.addView(
                createTemplateView(label) { applyTemplate(template) }
            )
        }
    }

    private fun createTemplateView(label: String, onClick: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            minWidth = dp(132)
            maxWidth = dp(190)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            setBackgroundResource(R.drawable.recent_template_background)
            text = label
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#1D1D1B"))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setOnClickListener { onClick() }
        }
    }

    private fun applyTemplate(template: TransferTemplate) {
        val currency = CurrencyEnum.fromNameOrNull(template.currency) ?: return
        currentFromCurrency = currency
        isToPhoneNumber = template.isPhone

        updateCurrencyIcon(currency)
        updateWalletBalances()
        setupChangeButton()
        setContactHint()
        if (isToPhoneNumber) {
            applyPhoneMask()
        } else {
            removePhoneMask()
        }

        binding.contact.setText(template.recipient)
        binding.contact.setSelection(binding.contact.text?.length ?: 0)
        binding.sumInput.setText(formatTemplateAmount(template.amount))
        binding.sumInput.setSelection(binding.sumInput.text?.length ?: 0)
        updateCommissionAndTotal(binding.sumInput.text.toString())
    }

    private fun formatTemplateAmount(amount: Double): String =
        BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun setupChangeButton() {
        val canSwitchRecipientMode = currentFromCurrency in listOf(
            CurrencyEnum.USDT_TRC20,
            CurrencyEnum.ESOM
        )
        binding.changeLayout.isVisible = canSwitchRecipientMode
        binding.changeLayout.setOnClickListener {
            if (currentFromCurrency in listOf(CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM)) {
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
        val payload = AppQrCode.parsePayload(rawContent)
        payload?.currency?.takeIf { it != currentFromCurrency }?.let { targetCurrency ->
            toggleCurrency(targetCurrency)
        }

        val contact = payload?.contact ?: normalizeQrContact(rawContent)
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
        AppQrCode.parseContact(value)?.let { return it }
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
            QrShareUtils.decodeQrFromBitmap(bitmap)
        }.getOrNull().also {
            bitmap.recycle()
        }
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return QrShareUtils.decodeQrFromBitmap(bitmap)
    }

    private fun firstNotBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }.orEmpty()

    private fun updateWalletBalances() {
        val wallets = (model.myData.value as? UiState.Success)?.data?.wallets ?: return
        val phone = (model.myData.value as? UiState.Success)?.data?.phone

        val currentWallet = wallets.find { it.currency == currentFromCurrency }
        binding.sum.text = currentWallet?.balance?.formatBalanceNew() ?: "0"
        binding.currencyTitle.text = getCurrencyName(currentFromCurrency)
        binding.currency.text = when (currentFromCurrency) {
            CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
            else -> currentWallet?.address?.takeLast(3)?.let { "*$it" } ?: ""
        }

        binding.peopleTitle.text = getCurrencyName(currentFromCurrency)
        binding.peopleIcon.setImageResource(
            when (currentFromCurrency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )

        currencyPanelOptions = when (currentFromCurrency) {
            CurrencyEnum.USDT_TRC20 -> listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM)
            CurrencyEnum.SOM -> listOf(CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM)
            CurrencyEnum.ESOM -> listOf(CurrencyEnum.USDT_TRC20, CurrencyEnum.SOM)
        }

        binding.firstUsdtBtn.isVisible = CurrencyEnum.USDT_TRC20 in currencyPanelOptions
        binding.firstDigitalBtn.isVisible = CurrencyEnum.ESOM in currencyPanelOptions
        binding.firstSomBtn.isVisible = CurrencyEnum.SOM in currencyPanelOptions

        val usdtWallet = wallets.find { it.currency == CurrencyEnum.USDT_TRC20 }
        val esomWallet = wallets.find { it.currency == CurrencyEnum.ESOM }
        val somWallet = wallets.find { it.currency == CurrencyEnum.SOM }

        bindCurrencyRow(
            icon = binding.firstUsdtIcon,
            title = binding.firstUsdtTitle,
            suffix = binding.firstUsdt,
            balance = binding.firstSum1,
            divider = binding.firstUsdtView,
            currency = CurrencyEnum.USDT_TRC20.takeIf { it in currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindCurrencyRow(
            icon = binding.firstDigitalIcon,
            title = binding.firstDigitalTitle,
            suffix = binding.firstDigital,
            balance = binding.firstSum2,
            divider = binding.firstDigitalView,
            currency = CurrencyEnum.ESOM.takeIf { it in currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
        bindCurrencyRow(
            icon = binding.firstSomIcon,
            title = binding.firstSomTitle,
            suffix = binding.firstSom,
            balance = binding.firstSum3,
            divider = binding.firstSomView,
            currency = CurrencyEnum.SOM.takeIf { it in currencyPanelOptions },
            walletUSDT = usdtWallet,
            walletESOM = esomWallet,
            walletSOM = somWallet,
            phone = phone
        )
    }

    private fun bindCurrencyRow(
        icon: android.widget.ImageView,
        title: android.widget.TextView,
        suffix: android.widget.TextView,
        balance: android.widget.TextView,
        divider: android.view.View,
        currency: CurrencyEnum?,
        walletUSDT: WalletModel?,
        walletESOM: WalletModel?,
        walletSOM: WalletModel?,
        phone: String?
    ) {
        if (currency == null) {
            icon.isVisible = false
            title.isVisible = false
            suffix.isVisible = false
            balance.isVisible = false
            divider.isVisible = false
            return
        }

        val wallet = when (currency) {
            CurrencyEnum.USDT_TRC20 -> walletUSDT
            CurrencyEnum.ESOM -> walletESOM
            CurrencyEnum.SOM -> walletSOM
        }

        icon.setImageResource(
            when (currency) {
                CurrencyEnum.SOM -> R.drawable.som_icon
                CurrencyEnum.ESOM -> R.drawable.salam_icon
                CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            }
        )
        icon.isVisible = true
        title.text = getCurrencyName(currency)
        title.isVisible = true
        suffix.text = when (currency) {
            CurrencyEnum.SOM -> phone?.takeLast(3)?.let { "*$it" } ?: ""
            else -> wallet?.address?.takeLast(3)?.let { "*$it" } ?: ""
        }
        suffix.isVisible = true
        balance.text = wallet?.balance?.formatBalanceNew() ?: "0"
        balance.isVisible = true
        divider.isVisible = true
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

        val totalAmount = amount + commission

        binding.comissionValue.text = formatTransferAmount(commission, currentFromCurrency)
        binding.total.text = formatTransferAmount(totalAmount.coerceAtLeast(0.0), currentFromCurrency)
    }

    private fun calculateTransferCommission(amount: Double, currency: CurrencyEnum): Double {
        if (amount <= 0.0) return 0.0
        return model.calculateTransferFee(amount, currency)
    }

    private fun formatTransferAmount(amount: Double, currency: CurrencyEnum): String {
        val scale = when (currency) {
            CurrencyEnum.USDT_TRC20 -> 6
            CurrencyEnum.SOM, CurrencyEnum.ESOM -> 2
        }
        return BigDecimal.valueOf(amount)
            .setScale(scale, RoundingMode.DOWN)
            .toPlainString()
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
        val fee = calculateTransferCommission(sum, currentFromCurrency)
        if (sum + fee > walletBalance) {
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
                TransferConfirmationFragment.CREDITED_AMOUNT_KEY to amount,
                TransferConfirmationFragment.FEE_KEY to calculateTransferCommission(
                    amount,
                    currentFromCurrency
                ),
                TransferConfirmationFragment.TOTAL_DEBITED_KEY to (
                    amount + calculateTransferCommission(amount, currentFromCurrency)
                ),
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
