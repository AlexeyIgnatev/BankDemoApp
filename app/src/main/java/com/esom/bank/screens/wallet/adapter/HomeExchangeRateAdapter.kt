package com.esom.bank.screens.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.databinding.ItemHomeExchangeRateBinding
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel

class HomeExchangeRateAdapter(
    private val context: Context
) : ListAdapter<WalletModel, HomeExchangeRateAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(
        private val binding: ItemHomeExchangeRateBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WalletModel) = with(binding) {
            rateIcon.setImageResource(icon(item.currency))
            rateName.text = name(item.currency)
            rateCode.text = code(item.currency)
            buyValue.text = context.getString(R.string.home_rate_value, item.buyRate.formatBalanceNew())
            sellValue.text = context.getString(R.string.home_rate_value, item.sellRate.formatBalanceNew())
            rateCard.setCardBackgroundColor(context.getColor(backgroundColor(item.currency)))
        }

        private fun icon(currency: CurrencyEnum): Int = when (currency) {
            CurrencyEnum.SOM -> R.drawable.som_icon
            CurrencyEnum.ESOM -> R.drawable.salam_icon
            CurrencyEnum.USDT_TRC20 -> R.drawable.usdt_icon
        }

        private fun name(currency: CurrencyEnum): String = when (currency) {
            CurrencyEnum.SOM -> context.getString(R.string.som_wallet)
            CurrencyEnum.ESOM -> context.getString(R.string.digital)
            CurrencyEnum.USDT_TRC20 -> context.getString(R.string.usdt)
        }

        private fun code(currency: CurrencyEnum): String = when (currency) {
            CurrencyEnum.SOM -> context.getString(R.string.kgs)
            CurrencyEnum.ESOM -> "SALAM"
            CurrencyEnum.USDT_TRC20 -> "USDT"
        }

        private fun backgroundColor(currency: CurrencyEnum): Int = when (currency) {
            CurrencyEnum.SOM -> R.color.home_rate_som
            CurrencyEnum.ESOM -> R.color.home_rate_salam
            CurrencyEnum.USDT_TRC20 -> R.color.home_rate_usdt
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemHomeExchangeRateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<WalletModel>() {
        override fun areItemsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean =
            oldItem.currency == newItem.currency

        override fun areContentsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean =
            oldItem == newItem
    }
}
