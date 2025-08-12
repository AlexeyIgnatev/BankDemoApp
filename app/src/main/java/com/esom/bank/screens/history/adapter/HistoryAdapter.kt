package com.esom.bank.screens.history.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemHistoryBinding
import com.esom.bank.databinding.ItemHistoryDateBinding
import com.esom.bank.screens.wallet.adapter.Transaction
import com.esom.bank.screens.wallet.adapter.TransactionAdapter

class HistoryAdapter(private val context: Context): ListAdapter<HistoryAdapter.HistoryItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {
    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_HISTORY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is HistoryItem.HistoryDate -> TYPE_DATE
            is HistoryItem.History -> TYPE_HISTORY
        }
    }
    inner class HistoryDateViewHolder(private val binding: ItemHistoryDateBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String, sum: Long) {
            binding.date.text = date
            binding.sum.text = sum.toString()
        }
    }
    inner class HistoryViewHolder(private val binding: ItemHistoryBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: List<Transaction>) {
            val adapter = TransactionAdapter(context)
            binding.transactions.adapter = adapter
            adapter.submitList(item)
        }
    }

    sealed class HistoryItem {
        data class HistoryDate(val date: String, val sum: Long): HistoryItem()
        data class History(val transaction: List<Transaction>): HistoryItem()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_DATE -> {
                val binding = ItemHistoryDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryDateViewHolder(binding)
            }
            TYPE_HISTORY -> {
                val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is HistoryDateViewHolder -> holder.bind((getItem(position) as HistoryItem.HistoryDate).date, (getItem(position) as HistoryItem.HistoryDate).sum)
            is HistoryViewHolder -> holder.bind((getItem(position) as HistoryItem.History).transaction)
        }
    }
}

class HistoryDiffCallback: DiffUtil.ItemCallback<HistoryAdapter.HistoryItem>() {
    override fun areItemsTheSame(oldItem: HistoryAdapter.HistoryItem, newItem: HistoryAdapter.HistoryItem
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: HistoryAdapter.HistoryItem,
        newItem: HistoryAdapter.HistoryItem
    ): Boolean {
        return oldItem == newItem
    }

}

