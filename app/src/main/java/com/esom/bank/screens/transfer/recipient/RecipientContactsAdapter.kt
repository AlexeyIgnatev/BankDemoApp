package com.esom.bank.screens.transfer.recipient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemTransferRecipientBinding
import com.esom.bank.screens.actions.model.PaymentContact

class RecipientContactsAdapter(
    private val onClick: (PaymentContact) -> Unit
) : ListAdapter<PaymentContact, RecipientContactsAdapter.ContactViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
            ItemTransferRecipientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = getItem(position)
        val section = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        val previousSection = currentList.getOrNull(position - 1)
            ?.name
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString()
        holder.bind(contact, section, section != previousSection)
    }

    inner class ContactViewHolder(
        private val binding: ItemTransferRecipientBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: PaymentContact, section: String, showSection: Boolean) {
            binding.section.isVisible = showSection
            binding.section.text = section
            binding.avatar.text = contact.initials
            binding.name.text = contact.name
            binding.phone.text = contact.phone
            binding.contactRow.setOnClickListener { onClick(contact) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<PaymentContact>() {
        override fun areItemsTheSame(oldItem: PaymentContact, newItem: PaymentContact) =
            oldItem.phone == newItem.phone

        override fun areContentsTheSame(oldItem: PaymentContact, newItem: PaymentContact) =
            oldItem == newItem
    }
}
