package com.esom.bank.screens.history.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemHistoryBinding
import com.esom.bank.databinding.ItemHistoryDateBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.wallet.adapter.TransactionAdapter
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val context: Context,
    private var showTransfers: Boolean = true
) : PagingDataAdapter<TransactionModel, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_TRANSACTIONS = 1
        private const val TYPE_PLACEHOLDER = 2
    }

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru")).apply {
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        dateFormatSymbols = object : DateFormatSymbols(Locale("ru")) {
            override fun getMonths(): Array<String> {
                return months
            }
        }
    }

    fun updateFilter(showTransfers: Boolean) {
        this.showTransfers = showTransfers
        notifyDataSetChanged()
    }

    private fun filterTransactions(transactions: List<TransactionModel>): List<TransactionModel> {
        return if (!showTransfers) {
            transactions.filter { it.type != TransactionEnum.TRANSFER }
        } else {
            transactions
        }
    }

    inner class HistoryDateViewHolder(private val binding: ItemHistoryDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.date.text = date
        }
    }

    inner class HistoryTransactionsViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transactions: List<TransactionModel>) {
            val filteredTransactions = filterTransactions(transactions)
            val dates = filteredTransactions.map { dateFormat.format(Date(it.createdAt ?: 0L)) }
            Log.d("HistoryAdapter", "Даты в адаптере: $dates, показано транзакций: ${filteredTransactions.size}")

            val adapter = TransactionAdapter(context)
            binding.transactions.adapter = adapter
            adapter.submitList(filteredTransactions)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return TYPE_PLACEHOLDER

        if (!showTransfers && item.type == TransactionEnum.TRANSFER) {
            return TYPE_PLACEHOLDER
        }

        val prevItem = if (position > 0) getItem(position - 1) else null
        val nextItem = if (position + 1 < itemCount) getItem(position + 1) else null

        val currentDate = dateFormat.format(Date(item.createdAt ?: 0L))

        val prevDate = prevItem?.let {
            if (!showTransfers && it.type == TransactionEnum.TRANSFER) null
            else dateFormat.format(Date(it.createdAt ?: 0L))
        }

        val nextDate = nextItem?.let {
            if (!showTransfers && it.type == TransactionEnum.TRANSFER) null
            else dateFormat.format(Date(it.createdAt ?: 0L))
        }

        val type = when {
            prevItem == null || currentDate != prevDate -> TYPE_DATE
            nextItem == null || currentDate != nextDate -> TYPE_TRANSACTIONS
            else -> TYPE_PLACEHOLDER
        }

        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemHistoryDateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HistoryDateViewHolder(binding)
            }

            TYPE_TRANSACTIONS -> {
                val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryTransactionsViewHolder(binding)
            }

            TYPE_PLACEHOLDER -> {
                val placeholder = View(parent.context)
                placeholder.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                )
                object : RecyclerView.ViewHolder(placeholder) {}
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        if (!showTransfers && item.type == TransactionEnum.TRANSFER) {
            return
        }

        val currentDate = dateFormat.format(Date(item.createdAt ?: 0L))

        val transactionsForDate = snapshot().items
            .filter {
                val shouldInclude = if (!showTransfers) it.type != TransactionEnum.TRANSFER else true
                shouldInclude && dateFormat.format(Date(it.createdAt ?: 0L)) == currentDate
            }

        when (holder) {
            is HistoryDateViewHolder -> {
                holder.bind(currentDate)
            }

            is HistoryTransactionsViewHolder -> holder.bind(transactionsForDate)
            else -> return
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem.createdAt == newItem.createdAt && oldItem.amount == newItem.amount

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}