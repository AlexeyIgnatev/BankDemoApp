package com.esom.bank.screens.transfer.recipient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentTransferRecipientBinding
import com.esom.bank.screens.actions.model.PaymentContact
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

@AndroidEntryPoint
class TransferRecipientFragment : Fragment() {
    private lateinit var binding: FragmentTransferRecipientBinding
    private val args: TransferRecipientFragmentArgs by navArgs()
    private val uiModel: TransferRecipientUiStateViewModel by viewModels()
    private val adapter = RecipientContactsAdapter(::selectContact)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferRecipientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { target, insets, rect ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.updatePadding(
                top = rect.top + systemBars.top,
                bottom = rect.bottom + systemBars.bottom
            )
            insets
        }

        binding.contactsList.adapter = adapter
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.continueBtn.setOnClickListener { continueWithInput() }
        binding.qrBtn.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startQrFragment())
        }
        binding.recipientInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                continueWithInput()
                true
            } else {
                false
            }
        }
        binding.recipientInput.doAfterTextChanged { filterContacts(it?.toString().orEmpty()) }
        loadContacts()
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showContacts(emptyList(), getString(R.string.payment_contacts_empty))
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            uiModel.setContacts(withContext(Dispatchers.IO) { readContacts() })
            filterContacts(binding.recipientInput.text?.toString().orEmpty())
        }
    }

    private fun readContacts(): List<PaymentContact> {
        val result = mutableListOf<PaymentContact>()
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
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val phone = cursor.getString(phoneIndex)?.trim().orEmpty()
                    val phoneKey = phone.filter(Char::isDigit)
                    if (name.isNotBlank() && phoneKey.length >= MIN_PHONE_DIGITS && seenPhones.add(phoneKey)) {
                        result += PaymentContact(name, phone)
                    }
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return result
    }

    private fun filterContacts(query: String) {
        val normalizedQuery = query.trim()
        val searchableQuery = normalizedQuery.toSearchableText()
        val queryDigits = normalizedQuery.filter(Char::isDigit)
        val filtered = if (normalizedQuery.isBlank()) {
            uiModel.uiState.value.contacts
        } else {
            uiModel.uiState.value.contacts.filter { contact ->
                contact.name.toSearchableText().contains(searchableQuery) ||
                    (queryDigits.isNotBlank() && contact.phone.filter(Char::isDigit).contains(queryDigits))
            }
        }
        showContacts(filtered, getString(R.string.recipient_not_found))
    }

    private fun String.toSearchableText(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.getDefault())
            .replace('ё', 'е')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun showContacts(items: List<PaymentContact>, emptyText: String) {
        adapter.submitList(items)
        binding.contactsList.isVisible = items.isNotEmpty()
        binding.emptyView.isVisible = items.isEmpty()
        binding.emptyView.text = emptyText
    }

    private fun selectContact(contact: PaymentContact) {
        openTransfer(contact.phone, contact.name)
    }

    private fun continueWithInput() {
        val recipient = binding.recipientInput.text?.toString()?.trim().orEmpty()
        if (recipient.isBlank()) {
            binding.root.showErrorSnackbar(getString(R.string.enter_phone_or_address))
        } else {
            openTransfer(recipient)
        }
    }

    private fun openTransfer(contact: String, recipientName: String = "") {
        findNavController().navigate(
            NavGraphDirections.startTransferFragment(args.currency, contact, recipientName)
        )
    }

    companion object {
        private const val MIN_PHONE_DIGITS = 7
    }
}
