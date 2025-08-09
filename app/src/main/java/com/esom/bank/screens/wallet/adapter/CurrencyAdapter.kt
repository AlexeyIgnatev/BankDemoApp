package com.esom.bank.screens.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemCurrencyBinding

class CurrencyAdapter: ListAdapter<Currency, CurrencyAdapter.CurrencyViewHolder>(CurrencyDiffCallback()) {

    inner class CurrencyViewHolder(private val binding: ItemCurrencyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Currency) {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        TODO("Not yet implemented")
    }
}

class  CurrencyDiffCallback: DiffUtil.ItemCallback<Currency>() {
    override fun areItemsTheSame(oldItem: Currency, newItem: Currency): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: Currency, newItem: Currency): Boolean {
        return oldItem == newItem
    }

}

data class Currency(
    val type: TypeOfCurrency,
    val up: String,
    val down: String
)

enum class TypeOfCurrency {
    FIAT,
    DIGITAL,
    USDT,
    BITCOIN,
    ETH
}