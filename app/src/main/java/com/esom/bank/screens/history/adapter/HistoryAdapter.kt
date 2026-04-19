package com.esom.bank.screens.history.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemHistoryBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.wallet.adapter.TransactionAdapter
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val context: Context,
    private var showTransfers: Boolean = true,
    private val onTransactionClick: ((TransactionModel) -> Unit)? = null
) : PagingDataAdapter<TransactionModel, HistoryAdapter.HistoryGroupViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val TAG = "HistoryAdapter"
    }

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru")).apply {
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        dateFormatSymbols = object : DateFormatSymbols(Locale("ru")) {
            override fun getMonths(): Array<String> = months
        }
    }

    private var groupedItems: List<HistoryGroup> = emptyList()

    init {
        addOnPagesUpdatedListener {
            regroup()
        }
    }

    fun updateFilter(showTransfers: Boolean) {
        this.showTransfers = showTransfers
        regroup()
    }

    private fun regroup() {
        val allItems = snapshot().items
        val filtered = if (!showTransfers)
            allItems.filter { it.type != TransactionEnum.TRANSFER }
        else allItems

        groupedItems = filtered
            .groupBy { dateFormat.format(Date(it.createdAt ?: 0L)) }
            .map { (date, list) -> HistoryGroup(date, list.sortedByDescending { it.createdAt }) }
            .sortedByDescending { it.list.firstOrNull()?.createdAt ?: 0L }

        Log.d(TAG, "Regrouped items (${groupedItems.size} days):")
        groupedItems.forEach {
            Log.d(TAG, " - ${it.date}: ${it.list.map { t -> "${t.type}-${t.amount}" }}")
        }

        notifyDataSetChanged()
    }

    inner class HistoryGroupViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: HistoryGroup) {
            val date = group.date
            val transactions = group.list
            Log.d(TAG, "Binding group for date $date with ${transactions.size} transactions")

            binding.date.text = date

            val adapter = TransactionAdapter(context, onTransactionClick)
            binding.transactions.adapter = adapter
            adapter.submitList(transactions)
        }
    }

    override fun getItemCount(): Int = groupedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryGroupViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryGroupViewHolder, position: Int) {
        val group = groupedItems.getOrNull(position)
        if (group != null) holder.bind(group)
        else Log.w(TAG, "No group found for position $position")
    }
}

data class HistoryGroup(
    val date: String,
    val list: List<TransactionModel>
)

class HistoryDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem.transactionId == newItem.transactionId &&
            oldItem.createdAt == newItem.createdAt &&
            oldItem.amount == newItem.amount

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}
