package com.esom.bank.screens.wallet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.ItemTransactionBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.isUserTransfer
import com.esom.bank.screens.main.enums.CurrencyEnum

class TransactionAdapter(
    private val context: Context,
    private val onTransactionClick: ((TransactionModel) -> Unit)? = null
) : ListAdapter<TransactionModel, TransactionAdapter.TransactionViewHolder>(
    TransactionDiffCallback()
) {
    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: TransactionModel) {
            binding.icon.setImageResource(currencyIcon(item.currencyEnum))

            val isTransfer = item.isUserTransfer()
            binding.title.text = transactionTitle(item, isTransfer)

            val counterpartyText = when {
                !isTransfer -> ""
                !item.recipientFullName.isNullOrBlank() ->
                    context.getString(R.string.history_recipient_name, item.recipientFullName)
                !item.senderFullName.isNullOrBlank() ->
                    context.getString(R.string.history_sender_name, item.senderFullName)
                else -> ""
            }
            binding.counterparty.text = counterpartyText
            binding.counterparty.visibility =
                if (counterpartyText.isBlank()) View.GONE else View.VISIBLE

            val (sign, color) = when (item.type) {
                TransactionEnum.INCOME, TransactionEnum.INFLOW ->
                    "+" to context.getColor(R.color.transaction_income)
                TransactionEnum.EXPENSE, TransactionEnum.TRANSFER ->
                    "-" to context.getColor(R.color.transaction_expense)
                else -> "" to context.getColor(R.color.transaction_expense)
            }
            binding.sum.setTextColor(color)
            binding.somIcon.setColorFilter(color)

            val formatted = "%.6f".format(item.amount ?: 0.0)
                .trimEnd('0')
                .trimEnd('.', ',')
                .ifEmpty { "0" }
            binding.sum.text = "$sign$formatted"
            binding.somIcon.visibility =
                if (item.currencyEnum == CurrencyEnum.SOM) View.VISIBLE else View.INVISIBLE
            binding.root.setOnClickListener { onTransactionClick?.invoke(item) }
        }

        private fun transactionTitle(item: TransactionModel, isTransfer: Boolean): String {
            if (item.type == TransactionEnum.CONVERSION && !isTransfer) {
                return context.getString(R.string.own_funds_conversion)
            }
            if (!item.recipientFullName.isNullOrBlank()) {
                return context.getString(R.string.transfer)
            }
            if (!item.senderFullName.isNullOrBlank()) {
                return context.getString(R.string.history_incoming_transfer)
            }

            val stringRes = when (item.type) {
                TransactionEnum.CONVERSION, TransactionEnum.TRANSFER -> transferTitle(item.currencyEnum)
                TransactionEnum.INCOME -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.income_som
                    CurrencyEnum.ESOM -> R.string.income_digital
                    CurrencyEnum.USDT_TRC20 -> R.string.income_usdt
                    null -> R.string.transfer
                }
                TransactionEnum.EXPENSE -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.expense_som
                    CurrencyEnum.ESOM -> R.string.expense_digital
                    CurrencyEnum.USDT_TRC20 -> R.string.expense_usdt
                    null -> R.string.transfer
                }
                TransactionEnum.INFLOW -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.inflow_som
                    CurrencyEnum.ESOM -> R.string.inflow_digital
                    CurrencyEnum.USDT_TRC20 -> R.string.inflow_usdt
                    null -> R.string.transfer
                }
                null -> R.string.transfer
            }
            return context.getString(stringRes)
        }

        private fun transferTitle(currency: CurrencyEnum?): Int = when (currency) {
            CurrencyEnum.SOM -> R.string.transfer_som
            CurrencyEnum.ESOM -> R.string.transfer_digital
            CurrencyEnum.USDT_TRC20 -> R.string.transfer_usdt
            null -> R.string.transfer
        }

        private fun currencyIcon(currency: CurrencyEnum?): Int = when (currency) {
            CurrencyEnum.SOM -> R.drawable.som_icon
            CurrencyEnum.ESOM -> R.drawable.salam_icon
            CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
            null -> R.drawable.som_icon
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        return TransactionViewHolder(
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem.transactionId == newItem.transactionId &&
            oldItem.createdAt == newItem.createdAt

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean =
        oldItem == newItem
}
