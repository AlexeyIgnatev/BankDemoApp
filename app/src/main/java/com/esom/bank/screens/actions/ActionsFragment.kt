package com.esom.bank.screens.actions

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        if (model.myData.value !is UiState.Success) model.updateUserData()
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

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
        binding.contactsList.isVisible = contacts.isNotEmpty()
        binding.contactsEmpty.isVisible = contacts.isEmpty()
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
