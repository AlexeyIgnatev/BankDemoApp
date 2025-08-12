package com.esom.bank.screens.wallet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.ItemTransactionBinding

class TransactionAdapter(private val context: Context): ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding): RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Transaction) {
            when(item.type) {
                TypeOfTransaction.SOM -> {
                    binding.icon.setImageResource(R.drawable.som_icon)
                    binding.title.text = context.getString(R.string.convert_som)
                }
                TypeOfTransaction.DIGITAL -> {
                    binding.icon.setImageResource(R.drawable.digital_icon)
                    binding.title.text = context.getString(R.string.convert_digital)
                }
                TypeOfTransaction.USDT -> {
                    binding.icon.setImageResource(R.drawable.usdt_icon)
                    binding.title.text = context.getString(R.string.convert_usdt)
                }
                TypeOfTransaction.BITCOIN -> {
                    binding.icon.setImageResource(R.drawable.bitcoin_icon)
                    binding.title.text = context.getString(R.string.convert_bitcoin)
                }
                TypeOfTransaction.ETH -> {
                    binding.icon.setImageResource(R.drawable.eth_icon)
                    binding.title.text = context.getString(R.string.convert_eth)
                }
            }

            if(item.sum > 0) {
                binding.sum.setTextColor(Color.parseColor("#38C72E"))
                binding.sum.text = "+" + item.sum
            } else {
                binding.sum.setTextColor(Color.parseColor("#1D1D1B"))
                binding.sum.text = item.sum.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TransactionDiffCallback: DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }

}

data class Transaction(
    val type: TypeOfTransaction,
    val title: String,
    val sum: Long
)

enum class TypeOfTransaction {
    SOM,
    DIGITAL,
    USDT,
    BITCOIN,
    ETH
}