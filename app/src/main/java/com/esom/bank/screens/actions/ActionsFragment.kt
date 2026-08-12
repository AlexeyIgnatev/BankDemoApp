package com.esom.bank.screens.actions

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentActionsBinding
import com.esom.bank.screens.actions.model.PaymentContact
import com.esom.bank.screens.actions.adapter.PaymentContactsAdapter
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.swap.model.SwapTemplate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ActionsFragment : Fragment() {
    private lateinit var binding: FragmentActionsBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: ActionsUiStateViewModel by viewModels()
    private val contactsAdapter = PaymentContactsAdapter { contact ->
        openTransfer(contact.phone, contact.name)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scrollView.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        setupCurrencyChips()
        binding.contactsList.adapter = contactsAdapter
        binding.sendCard.setOnClickListener { openRecipientPicker() }
        binding.convertCard.setOnClickListener { openConversion() }
        binding.templatesHeader.setOnClickListener {
            uiModel.toggleTemplates()
            renderSections()
        }
        binding.contactsHeader.setOnClickListener {
            uiModel.toggleContacts()
            renderSections()
        }
        renderSections()
        if (model.myData.value !is UiState.Success) model.updateUserData()
    }

    override fun onResume() {
        super.onResume()
        renderTemplates()
        loadContacts()
    }

    private fun renderSections() {
        val state = uiModel.uiState.value
        binding.templatesContainer.isVisible = state.templatesExpanded
        binding.templatesEmpty.isVisible = state.templatesExpanded &&
            model.getTransferTemplates().isEmpty() && model.getSwapTemplates().isEmpty()
        binding.templatesArrow.rotation = if (state.templatesExpanded) 180f else 0f
        binding.contactsList.isVisible = state.contactsExpanded && contactsAdapter.currentList.isNotEmpty()
        binding.contactsEmpty.isVisible = state.contactsExpanded && contactsAdapter.currentList.isEmpty()
        binding.contactsArrow.rotation = if (state.contactsExpanded) 180f else 0f
    }

    private fun renderTemplates() {
        binding.templatesContainer.removeAllViews()
        model.getTransferTemplates().forEach { template ->
            binding.templatesContainer.addView(templateRow(templateTitle(template)) {
                showTransferTemplate(template)
            })
        }
        model.getSwapTemplates().forEach { template ->
            binding.templatesContainer.addView(templateRow(templateTitle(template)) {
                showSwapTemplate(template)
            })
        }
        renderSections()
    }

    private fun templateRow(title: String, clicked: () -> Unit) = TextView(requireContext()).apply {
        text = title
        textSize = 15f
        setTextColor(context.getColor(R.color.title))
        setBackgroundResource(R.drawable.payment_primary_action_background)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
        setOnClickListener { clicked() }
    }

    private fun showTransferTemplate(template: TransferTemplate) {
        val currency = CurrencyEnum.fromNameOrNull(template.currency) ?: return
        val details = "Перевод ${currencyTitle(currency)} получателю ${template.recipient} на сумму ${template.amount} ${currencyTitle(currency)}"
        showTemplateDialog(
            title = templateTitle(template),
            details = details,
            rename = { renameTransferTemplate(template) },
            apply = {
                findParentNavController().navigate(
                    NavGraphDirections.startTransferFragment(
                        template.currency,
                        template.recipient,
                        "",
                        template.amount.toFloat()
                    )
                )
            }
        )
    }

    private fun showSwapTemplate(template: SwapTemplate) {
        val from = CurrencyEnum.fromNameOrNull(template.fromCurrency) ?: return
        val to = CurrencyEnum.fromNameOrNull(template.toCurrency) ?: return
        val details = "Конвертация ${template.amount} ${currencyTitle(from)} из ${currencyTitle(from)} в ${currencyTitle(to)}"
        showTemplateDialog(
            title = templateTitle(template),
            details = details,
            rename = { renameSwapTemplate(template) },
            apply = {
                findParentNavController().navigate(
                    NavGraphDirections.startSwapFragment(from.name, to.name, template.amount.toFloat())
                )
            }
        )
    }

    private fun showTemplateDialog(
        title: String,
        details: String,
        rename: () -> Unit,
        apply: () -> Unit
    ) {
        val titleView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(8), dp(4))
        }
        titleView.addView(TextView(requireContext()).apply {
            text = title
            textSize = 20f
            setTextColor(context.getColor(R.color.title))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val closeButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(context.getColor(R.color.title))
            background = context.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
            ).let { attributes ->
                attributes.getDrawable(0).also { attributes.recycle() }
            }
            contentDescription = "Закрыть"
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        titleView.addView(closeButton, LinearLayout.LayoutParams(dp(48), dp(48)))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setCustomTitle(titleView)
            .setMessage(details)
            .setNegativeButton("Изменить название") { _, _ -> rename() }
            .setPositiveButton("Применить шаблон") { _, _ -> apply() }
            .create()
        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun renameTransferTemplate(template: TransferTemplate) = showRenameDialog(templateTitle(template)) { name ->
        model.renameTransferTemplate(template, name)
        renderTemplates()
    }

    private fun renameSwapTemplate(template: SwapTemplate) = showRenameDialog(templateTitle(template)) { name ->
        model.renameSwapTemplate(template, name)
        renderTemplates()
    }

    private fun showRenameDialog(current: String, renamed: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            setText(current)
            setSelection(text.length)
            hint = "Название шаблона"
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Название шаблона")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    renamed(name)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun templateTitle(template: TransferTemplate): String =
        template.name?.takeIf(String::isNotBlank) ?: "Перевод ${currencyTitle(CurrencyEnum.fromNameOrNull(template.currency) ?: CurrencyEnum.SOM)}"

    private fun templateTitle(template: SwapTemplate): String {
        val from = CurrencyEnum.fromNameOrNull(template.fromCurrency) ?: CurrencyEnum.SOM
        val to = CurrencyEnum.fromNameOrNull(template.toCurrency) ?: CurrencyEnum.ESOM
        return template.name?.takeIf(String::isNotBlank) ?: "${currencyTitle(from)} → ${currencyTitle(to)}"
    }

    private fun currencyTitle(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "Сом"
        CurrencyEnum.ESOM -> "Салам"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun setupCurrencyChips() {
        binding.currencySomChip.setOnClickListener { selectCurrency(CurrencyEnum.SOM) }
        binding.currencySalamChip.setOnClickListener { selectCurrency(CurrencyEnum.ESOM) }
        binding.currencyUsdtChip.setOnClickListener { selectCurrency(CurrencyEnum.USDT_TRC20) }
        selectCurrency(uiModel.uiState.value.selectedCurrency)
    }

    private fun selectCurrency(currency: CurrencyEnum) {
        uiModel.selectCurrency(currency)
        binding.currencySomChip.isSelected = currency == CurrencyEnum.SOM
        binding.currencySalamChip.isSelected = currency == CurrencyEnum.ESOM
        binding.currencyUsdtChip.isSelected = currency == CurrencyEnum.USDT_TRC20
        val currencyName = when (currency) {
            CurrencyEnum.SOM -> getString(R.string.som)
            CurrencyEnum.ESOM -> getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }
        binding.selectedWalletLabel.text = getString(R.string.payment_transfer_details, currencyName)
        binding.conversionWalletLabel.text = getString(R.string.payment_conversion_details, currencyName)
    }

    private fun openTransfer(contact: String = "", recipientName: String = "") {
        findParentNavController().navigate(
            NavGraphDirections.startTransferFragment(uiModel.uiState.value.selectedCurrency.name, contact, recipientName)
        )
    }

    private fun openRecipientPicker() {
        findParentNavController().navigate(
            NavGraphDirections.startTransferRecipientFragment(uiModel.uiState.value.selectedCurrency.name)
        )
    }

    private fun openConversion() {
        val (from, to) = when (uiModel.uiState.value.selectedCurrency) {
            CurrencyEnum.SOM -> CurrencyEnum.SOM to CurrencyEnum.ESOM
            CurrencyEnum.ESOM -> CurrencyEnum.ESOM to CurrencyEnum.SOM
            CurrencyEnum.USDT_TRC20 -> CurrencyEnum.ESOM to CurrencyEnum.USDT_TRC20
        }
        findParentNavController().navigate(
            NavGraphDirections.startSwapFragment(from.name, to.name)
        )
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showContacts(emptyList(), permissionMissing = true)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val contacts = withContext(Dispatchers.IO) { readContacts() }
            showContacts(contacts, permissionMissing = false)
        }
    }

    private fun readContacts(): List<PaymentContact> {
        val contacts = mutableListOf<PaymentContact>()
        val seenPhones = hashSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            requireContext().contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " COLLATE NOCASE ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(projection[0])
                val phoneIndex = cursor.getColumnIndexOrThrow(projection[1])
                while (cursor.moveToNext() && contacts.size < MAX_CONTACTS) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val phone = cursor.getString(phoneIndex)?.trim().orEmpty()
                    val phoneKey = phone.filter(Char::isDigit)
                    if (name.isNotBlank() && phoneKey.length >= MIN_PHONE_DIGITS && seenPhones.add(phoneKey)) {
                        contacts += PaymentContact(name, phone)
                    }
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return contacts
    }

    private fun showContacts(contacts: List<PaymentContact>, permissionMissing: Boolean) {
        if (!isAdded) return
        contactsAdapter.submitList(contacts)
        renderSections()
        if (!permissionMissing && contacts.isEmpty()) {
            binding.contactsEmpty.text = getString(R.string.payment_contacts_not_found)
        } else {
            binding.contactsEmpty.text = getString(R.string.payment_contacts_empty)
        }
    }

    companion object {
        private const val MAX_CONTACTS = 10
        private const val MIN_PHONE_DIGITS = 7
    }
}
