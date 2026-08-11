package com.esom.bank.screens.wallets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.databinding.ItemWalletAccountBinding
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel

class WalletListAdapter(
    private val context: Context,
    private val phoneProvider: () -> String?,
    private val balancesVisibleProvider: () -> Boolean,
    private val onWalletClick: (WalletModel) -> Unit
) : ListAdapter<WalletModel, WalletListAdapter.ViewHolder>(Diff()) {
    fun refreshBalanceVisibility() = notifyItemRangeChanged(0, itemCount)

    inner class ViewHolder(private val binding: ItemWalletAccountBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WalletModel, isLast: Boolean) = with(binding) {
            walletIcon.setImageResource(icon(item.currency))
            val balancesVisible = balancesVisibleProvider()
            balance.setBalance(item.balance.formatBalanceNew(), balancesVisible)
            currency.text = currencyCode(item.currency)
            somSign.visibility = if (item.currency == CurrencyEnum.SOM) View.VISIBLE else View.GONE
            somSign.animate().alpha(if (balancesVisible) 1f else 0f).setDuration(220L).start()
            currency.animate().alpha(if (balancesVisible) 1f else 0f).setDuration(220L).start()
            account.text = when (item.currency) {
                CurrencyEnum.SOM -> phoneProvider()?.takeLast(4)?.let { "Счёт •• $it" }
                    ?: "Счёт •• ${item.address.takeLast(4)}"
                else -> "Кошелёк •• ${item.address.takeLast(4)}"
            }
            walletType.text = title(item.currency)
            divider.visibility = if (isLast) View.GONE else View.VISIBLE
            root.setOnClickListener { onWalletClick(item) }
        }

        private fun title(currency: CurrencyEnum): String = when (currency) {
            CurrencyEnum.SOM -> context.getString(R.string.som_wallet)
            CurrencyEnum.ESOM -> context.getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> "USDT TRC20"
        }

        private fun currencyCode(currency: CurrencyEnum): String = when (currency) {
            CurrencyEnum.SOM -> ""
            CurrencyEnum.ESOM -> "SALAM"
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }

        private fun icon(currency: CurrencyEnum): Int = when (currency) {
            CurrencyEnum.SOM -> R.drawable.som_icon
            CurrencyEnum.ESOM -> R.drawable.salam_icon
            CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemWalletAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    class Diff : DiffUtil.ItemCallback<WalletModel>() {
        override fun areItemsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean =
            oldItem.currency == newItem.currency && oldItem.address == newItem.address
        override fun areContentsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean = oldItem == newItem
    }
}
