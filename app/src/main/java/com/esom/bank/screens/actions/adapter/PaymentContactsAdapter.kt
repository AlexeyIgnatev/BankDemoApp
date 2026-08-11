package com.esom.bank.screens.actions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemPaymentContactBinding
import com.esom.bank.screens.actions.model.PaymentContact

class PaymentContactsAdapter(
    private val onClick: (PaymentContact) -> Unit
) : ListAdapter<PaymentContact, PaymentContactsAdapter.ContactViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
            ItemPaymentContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(
        private val binding: ItemPaymentContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: PaymentContact) {
            binding.avatar.text = contact.initials
            binding.name.text = contact.name
            binding.root.setOnClickListener { onClick(contact) }
        }
    }

    private class Diff : DiffUtil.ItemCallback<PaymentContact>() {
        override fun areItemsTheSame(oldItem: PaymentContact, newItem: PaymentContact) =
            oldItem.phone == newItem.phone

        override fun areContentsTheSame(oldItem: PaymentContact, newItem: PaymentContact) =
            oldItem == newItem
    }
}
