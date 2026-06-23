package com.esom.bank.screens.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.ItemCurrencyBinding

class CurrencyAdapter(private val context: Context): ListAdapter<Currency, CurrencyAdapter.CurrencyViewHolder>(CurrencyDiffCallback()) {

    inner class CurrencyViewHolder(private val binding: ItemCurrencyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Currency) {
                when(item.type) {
                    TypeOfCurrency.FIAT -> {
                        binding.icon.setImageResource(R.drawable.som_icon)
                        binding.title.text = context.getString(R.string.kgs)
                        binding.currencyName.text = context.getString(R.string.fiat_som)
                    }
                    TypeOfCurrency.USDT -> {
                        binding.icon.setImageResource(R.drawable.usdt_icon)
                        binding.title.visibility = View.GONE
                        binding.currencyName.visibility = View.GONE
                        binding.currency.text = context.getString(R.string.usdt)
                    }
                    TypeOfCurrency.DIGITAL -> {
                        binding.icon.setImageResource(R.drawable.salam_icon)
                        binding.title.visibility = View.GONE
                        binding.currencyName.visibility = View.GONE
                        binding.currency.text = context.getString(R.string.digital)
                    }
                }

            binding.upCost.text = item.up
            binding.downCost.text = item.down
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(getItem(position))
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
    USDT
}
