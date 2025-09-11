package com.esom.bank.screens.history.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemHistoryBinding
import com.esom.bank.databinding.ItemHistoryDateBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.wallet.adapter.TransactionAdapter
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val context: Context) :
    PagingDataAdapter<TransactionModel, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_TRANSACTION = 1
    }

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    inner class HistoryDateViewHolder(private val binding: ItemHistoryDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String, sum: Double) {
            val sumText = if (sum >= 0) "+${sum.toInt()}" else sum.toInt().toString()
            binding.date.text = date
            binding.sum.text = sumText
        }
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionModel) {
            val currentDate = dateFormat.format(Date(item.createdAt))
            val transactionsForDate = snapshot().items
                .filter { dateFormat.format(Date(it.createdAt)) == currentDate }

            val adapter = TransactionAdapter(context)
            binding.transactions.adapter = adapter
            adapter.submitList(transactionsForDate)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return TYPE_TRANSACTION
        val prevItem = if (position > 0) getItem(position - 1) else null

        val currentDate = dateFormat.format(Date(item.createdAt))
        val prevDate = prevItem?.let { dateFormat.format(Date(it.createdAt)) }

        return if (prevItem == null || currentDate != prevDate) TYPE_DATE else TYPE_TRANSACTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding =
                    ItemHistoryDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryDateViewHolder(binding)
            }

            TYPE_TRANSACTION -> {
                val binding =
                    ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        when (holder) {
            is HistoryDateViewHolder -> {
                val currentDate = dateFormat.format(Date(item.createdAt))
                val sumForDate = snapshot().items
                    .filter { dateFormat.format(Date(it.createdAt)) == currentDate }
                    .sumOf { tx ->
                        when (tx.type) {
                            TransactionEnum.INCOME, TransactionEnum.INFLOW -> tx.amount
                            TransactionEnum.EXPENSE, TransactionEnum.TRANSFER -> -tx.amount
                            TransactionEnum.CONVERSATION -> 0.0
                        }
                    }
                holder.bind(currentDate, sumForDate)
            }

            is HistoryViewHolder -> holder.bind(item)
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem.createdAt == newItem.createdAt && oldItem.amount == newItem.amount

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}
