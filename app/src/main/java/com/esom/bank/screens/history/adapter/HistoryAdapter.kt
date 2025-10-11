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
import java.util.*

class HistoryAdapter(private val context: Context) :
    PagingDataAdapter<TransactionModel, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_TRANSACTION = 1
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

    inner class HistoryDateViewHolder(private val binding: ItemHistoryDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.date.text = date
        }
    }

    inner class HistoryTransactionViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionModel) {
            val adapter = TransactionAdapter(context)
            binding.transactions.adapter = adapter
            adapter.submitList(listOf(transaction))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position) ?: return TYPE_TRANSACTION

        val shouldShowDate = shouldShowDate(position)

        return if (shouldShowDate) TYPE_DATE else TYPE_TRANSACTION
    }

    private fun shouldShowDate(position: Int): Boolean {
        val currentItem = getItem(position) ?: return false

        if (position == 0) {
            return true
        }

        val prevItem = getItem(position - 1) ?: return true
        val currentDate = dateFormat.format(Date(currentItem.createdAt ?: 0L))
        val prevDate = dateFormat.format(Date(prevItem.createdAt ?: 0L))

        return currentDate != prevDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemHistoryDateBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HistoryDateViewHolder(binding)
            }
            TYPE_TRANSACTION -> {
                val binding = ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HistoryTransactionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (holder) {
            is HistoryDateViewHolder -> {
                val date = dateFormat.format(Date(item.createdAt ?: 0L))
                holder.bind(date)
            }
            is HistoryTransactionViewHolder -> {
                holder.bind(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount()
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}