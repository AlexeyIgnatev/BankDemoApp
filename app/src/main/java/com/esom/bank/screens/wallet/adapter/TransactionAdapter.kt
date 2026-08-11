package com.esom.bank.screens.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.databinding.ItemTransactionBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.isUserTransfer
import com.esom.bank.screens.history.model.isDisplayedAsIncome
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.wallet.model.HomeTransactionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeTransactionAdapter(
    private val context: Context,
    private val balancesVisibleProvider: () -> Boolean = { true },
    private val onTransactionClick: ((HomeTransactionItem) -> Unit)? = null
) : ListAdapter<HomeTransactionItem, HomeTransactionAdapter.TransactionViewHolder>(DiffCallback()) {
    fun refreshBalanceVisibility() = notifyItemRangeChanged(0, itemCount)

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeTransactionItem) {
            val transaction = item.transaction
            binding.icon.setImageResource(currencyIcon(transaction.currencyEnum))
            binding.title.text = title(item)
            binding.counterparty.text = description(item)
            binding.counterparty.visibility =
                if (binding.counterparty.text.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.date.text = transaction.createdAt?.let {
                SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(it))
            }.orEmpty()

            val isIncome = transaction.isDisplayedAsIncome()
            val sign = if (isIncome) "+" else "-"
            val amount = "$sign${(transaction.amount ?: 0.0).formatBalanceNew()} ${currencyShort(transaction.currencyEnum)}"
            binding.sum.setBalance(amount, balancesVisibleProvider())
            binding.sum.setTextColor(
                context.getColor(if (isIncome) R.color.transaction_income else R.color.title)
            )
            binding.root.setOnClickListener { onTransactionClick?.invoke(item) }
        }

        private fun title(item: HomeTransactionItem): String {
            if (item.conversionFrom != null || item.conversionTo != null) {
                return context.getString(R.string.home_conversion)
            }
            if (item.transaction.isUserTransfer()) {
                return if (!item.transaction.senderFullName.isNullOrBlank() ||
                    item.transaction.type == TransactionEnum.INCOME ||
                    item.transaction.type == TransactionEnum.INFLOW
                ) {
                    context.getString(
                        R.string.home_received_currency,
                        currencyName(item.transaction.currencyEnum)
                    )
                } else {
                    context.getString(
                        R.string.home_transfer_currency,
                        currencyName(item.transaction.currencyEnum)
                    )
                }
            }
            return when (item.transaction.type) {
                TransactionEnum.INCOME, TransactionEnum.INFLOW -> context.getString(R.string.home_received)
                TransactionEnum.EXPENSE, TransactionEnum.TRANSFER -> context.getString(R.string.home_transfer)
                TransactionEnum.CONVERSION -> context.getString(R.string.home_conversion)
                null -> context.getString(R.string.transfer)
            }
        }

        private fun description(item: HomeTransactionItem): String {
            val transaction = item.transaction
            if (!transaction.recipientFullName.isNullOrBlank()) {
                return context.getString(R.string.home_to_person, transaction.recipientFullName)
            }
            if (!transaction.senderFullName.isNullOrBlank()) {
                return context.getString(R.string.home_from_person, transaction.senderFullName)
            }
            if (item.conversionFrom != null && item.conversionTo != null) {
                return context.getString(
                    R.string.home_conversion_description,
                    currencyName(item.conversionFrom.currencyEnum),
                    currencyName(item.conversionTo.currencyEnum)
                )
            }
            if (transaction.isUserTransfer() && !transaction.accountDetails.isNullOrBlank()) {
                val label = if (transaction.type == TransactionEnum.INCOME ||
                    transaction.type == TransactionEnum.INFLOW
                ) R.string.home_from_account else R.string.home_to_account
                return context.getString(label, transaction.accountDetails)
            }
            if (transaction.type == TransactionEnum.CONVERSION) {
                return context.getString(R.string.own_funds_conversion_single)
            }
            return currencyName(transaction.currencyEnum)
        }

        private fun currencyName(currency: CurrencyEnum?): String = when (currency) {
            CurrencyEnum.SOM -> context.getString(R.string.som)
            CurrencyEnum.ESOM -> context.getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> "USDT"
            null -> context.getString(R.string.wallet)
        }

        private fun currencyShort(currency: CurrencyEnum?): String = when (currency) {
            CurrencyEnum.SOM -> context.getString(R.string.kgs)
            CurrencyEnum.ESOM -> "SALAM"
            CurrencyEnum.USDT_TRC20 -> "USDT"
            null -> ""
        }

        private fun currencyIcon(currency: CurrencyEnum?): Int = when (currency) {
            CurrencyEnum.SOM -> R.drawable.som_icon
            CurrencyEnum.ESOM -> R.drawable.salam_icon
            CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            null -> R.drawable.fcb_icon
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder =
        TransactionViewHolder(
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<HomeTransactionItem>() {
        override fun areItemsTheSame(oldItem: HomeTransactionItem, newItem: HomeTransactionItem): Boolean =
            oldItem.transaction.transactionId == newItem.transaction.transactionId &&
                oldItem.transaction.createdAt == newItem.transaction.createdAt

        override fun areContentsTheSame(oldItem: HomeTransactionItem, newItem: HomeTransactionItem): Boolean =
            oldItem == newItem
    }
}
