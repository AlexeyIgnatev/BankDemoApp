package com.esom.bank.screens.wallet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
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
import com.esom.bank.screens.main.enums.CurrencyEnum

class TransactionAdapter(
    private val context: Context,
    private val onTransactionClick: ((TransactionModel) -> Unit)? = null
) :
    ListAdapter<TransactionModel, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: TransactionModel) {
            when (item.currencyEnum) {
                CurrencyEnum.SOM -> binding.icon.setImageResource(R.drawable.som_icon)
                CurrencyEnum.ESOM -> binding.icon.setImageResource(R.drawable.salam_icon)
                CurrencyEnum.USDT_TRC20 -> binding.icon.setImageResource(R.drawable.usdt_icon)
                CurrencyEnum.BTC -> binding.icon.setImageResource(R.drawable.bitcoin_icon)
                CurrencyEnum.ETH -> binding.icon.setImageResource(R.drawable.eth_icon)
                null -> Log.e("error", "error - null")
            }

            val stringResId = when (item.type) {
                TransactionEnum.CONVERSION -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.convert_som
                    CurrencyEnum.ESOM -> R.string.convert_digital
                    CurrencyEnum.ETH -> R.string.convert_eth
                    CurrencyEnum.BTC -> R.string.convert_bitcoin
                    CurrencyEnum.USDT_TRC20 -> R.string.convert_usdt
                    else -> R.string.transfer_usdt
                }
                TransactionEnum.INCOME -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.income_som
                    CurrencyEnum.ESOM -> R.string.income_digital
                    CurrencyEnum.ETH -> R.string.income_eth
                    CurrencyEnum.BTC -> R.string.income_bitcoin
                    CurrencyEnum.USDT_TRC20 -> R.string.income_usdt
                    else -> R.string.transfer_usdt
                }
                TransactionEnum.EXPENSE -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.expense_som
                    CurrencyEnum.ESOM -> R.string.expense_digital
                    CurrencyEnum.ETH -> R.string.expense_eth
                    CurrencyEnum.BTC -> R.string.expense_bitcoin
                    CurrencyEnum.USDT_TRC20 -> R.string.expense_usdt
                    else -> R.string.transfer_usdt
                }
                TransactionEnum.INFLOW -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.inflow_som
                    CurrencyEnum.ESOM -> R.string.inflow_digital
                    CurrencyEnum.ETH -> R.string.inflow_eth
                    CurrencyEnum.BTC -> R.string.inflow_bitcoin
                    CurrencyEnum.USDT_TRC20 -> R.string.inflow_usdt
                    else -> R.string.transfer_usdt
                }
                TransactionEnum.TRANSFER -> when (item.currencyEnum) {
                    CurrencyEnum.SOM -> R.string.transfer_som
                    CurrencyEnum.ESOM -> R.string.transfer_digital
                    CurrencyEnum.ETH -> R.string.transfer_eth
                    CurrencyEnum.BTC -> R.string.transfer_bitcoin
                    CurrencyEnum.USDT_TRC20 -> R.string.transfer_usdt
                    else -> R.string.transfer_usdt
                }

                null -> R.string.transfer_usdt
            }

            binding.title.text = context.getString(stringResId)

            val (sign, color) = when (item.type) {
                TransactionEnum.INCOME, TransactionEnum.INFLOW -> "+" to "#38C72E"
                TransactionEnum.EXPENSE, TransactionEnum.TRANSFER -> "-" to "#1D1D1B"
                TransactionEnum.CONVERSION -> "" to "#1D1D1B"
                else -> "" to "#1D1D1B"
            }

            binding.sum.setTextColor(Color.parseColor(color))
            binding.somIcon.setColorFilter(Color.parseColor(color))
            val formatted = "%.6f".format(item.amount)
                .trimEnd('0')
                .trimEnd('.', ',')
                .ifEmpty { "0" }

            binding.sum.text = "$sign$formatted"
            binding.somIcon.visibility =
                if (item.currencyEnum == CurrencyEnum.SOM) View.VISIBLE else View.INVISIBLE

            binding.root.setOnClickListener {
                onTransactionClick?.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding =
            ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionModel>() {
    override fun areItemsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean {
        return oldItem.transactionId == newItem.transactionId &&
            oldItem.createdAt == newItem.createdAt
    }

    override fun areContentsTheSame(oldItem: TransactionModel, newItem: TransactionModel): Boolean {
        return oldItem == newItem
    }
}
