package com.esom.bank.screens.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemHistoryBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.isUserTransfer
import com.esom.bank.screens.history.model.HistoryAdapterUiState
import com.esom.bank.screens.history.model.HistoryGroup
import com.esom.bank.screens.history.model.HistoryTypeFilter
import com.esom.bank.screens.wallet.adapter.HomeTransactionAdapter
import com.esom.bank.screens.wallet.model.toHomeTransactionItems
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val stateProvider: () -> HistoryAdapterUiState,
    private val balancesVisibleProvider: () -> Boolean,
    private val onTransactionClick: ((TransactionModel) -> Unit)? = null
) : PagingDataAdapter<TransactionModel, HistoryAdapter.HistoryGroupViewHolder>(HistoryDiffCallback()) {
    private val groups = AsyncListDiffer(this, HistoryGroupDiffCallback())
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru", "RU")).apply {
        dateFormatSymbols = object : DateFormatSymbols(Locale("ru", "RU")) {
            override fun getMonths(): Array<String> = arrayOf(
                "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря"
            )
        }
    }

    init {
        addOnPagesUpdatedListener(::regroup)
    }

    fun refreshBalanceVisibility() {
        notifyItemRangeChanged(0, itemCount)
    }

    fun regroup() {
        val state = stateProvider()
        val filtered = snapshot().items
            .asSequence()
            .filter { !state.withoutTransfers || it.type == TransactionEnum.CONVERSION && !it.isUserTransfer() }
            .filter { transaction ->
                when (state.typeFilter) {
                    HistoryTypeFilter.ALL -> true
                    HistoryTypeFilter.TRANSFERS -> transaction.isUserTransfer()
                    HistoryTypeFilter.CONVERSIONS ->
                        transaction.type == TransactionEnum.CONVERSION && !transaction.isUserTransfer()
                    HistoryTypeFilter.INCOME ->
                        transaction.type == TransactionEnum.INCOME || transaction.type == TransactionEnum.INFLOW
                    HistoryTypeFilter.EXPENSES ->
                        transaction.type == TransactionEnum.EXPENSE || transaction.isUserTransfer()
                }
            }
            .filter { transaction ->
                val amount = transaction.amount ?: 0.0
                (state.minimumAmount == null || amount >= state.minimumAmount) &&
                    (state.maximumAmount == null || amount <= state.maximumAmount)
            }
            .filter { state.searchQuery.isBlank() || it.searchableText().contains(state.searchQuery) }
            .toList()

        val groups = filtered
            .groupBy { dateFormat.format(Date(it.createdAt ?: 0L)) }
            .map { (date, list) -> HistoryGroup(date, list.sortedByDescending { it.createdAt }) }
            .sortedByDescending { it.list.firstOrNull()?.createdAt ?: 0L }
        this.groups.submitList(groups)
    }

    private fun TransactionModel.searchableText(): String = buildString {
        append(type?.name.orEmpty())
        append(' ')
        append(currencyEnum?.name.orEmpty())
        append(' ')
        append(amount?.toString().orEmpty())
        append(' ')
        append(recipientFullName.orEmpty())
        append(' ')
        append(senderFullName.orEmpty())
    }.lowercase(Locale.getDefault())

    inner class HistoryGroupViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(group: HistoryGroup) {
            binding.date.text = group.date
            binding.transactions.adapter = HomeTransactionAdapter(
                context = binding.root.context,
                balancesVisibleProvider = balancesVisibleProvider,
                onTransactionClick = { item -> onTransactionClick?.invoke(item.transaction) }
            ).apply {
                submitList(group.list.toHomeTransactionItems(group.list.size))
            }
        }
    }

    override fun getItemCount(): Int = groups.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryGroupViewHolder =
        HistoryGroupViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: HistoryGroupViewHolder, position: Int) {
        groups.currentList.getOrNull(position)?.let(holder::bind)
    }
}

private class HistoryGroupDiffCallback : DiffUtil.ItemCallback<HistoryGroup>() {
    override fun areItemsTheSame(oldItem: HistoryGroup, newItem: HistoryGroup): Boolean =
        oldItem.date == newItem.date

    override fun areContentsTheSame(oldItem: HistoryGroup, newItem: HistoryGroup): Boolean =
        oldItem == newItem
}

class HistoryDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem.transactionId == newItem.transactionId && oldItem.createdAt == newItem.createdAt

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}
