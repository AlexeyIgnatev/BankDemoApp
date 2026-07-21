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
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.databinding.CardPageBinding
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel

class CardAdapter(
    private val context: Context,
    private val phoneProvider: () -> String?,
    private val onSwapClick: (CurrencyEnum, CurrencyEnum) -> Unit,
    private val onReceiveClick: (CurrencyEnum) -> Unit,
    private val onTransferClick: (CurrencyEnum) -> Unit
) : ListAdapter<WalletModel, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    inner class CardViewHolder(private val binding: CardPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: WalletModel) {
            binding.convertBtn.setOnClickListener { onTransferClick(item.currency) }
            binding.acceptBtn.setOnClickListener { onReceiveClick(item.currency) }
            binding.newConvertBtn.setOnClickListener {
                val (fromCurrency, toCurrency) = getSwapCurrencies(item.currency)
                onSwapClick(fromCurrency, toCurrency)
            }
            binding.convertTitle.text = getConvertTitle(item.currency)

            when (item.currency) {
                CurrencyEnum.SOM -> {
                    binding.somIcon.setImageResource(R.drawable.som_icon)
                    binding.somTitle.text = context.getString(R.string.som)
                    binding.somCount.text = item.balance.formatBalanceNew()
                    binding.cardNumberIcon.setImageResource(R.drawable.icon_sum_som)
                    binding.somIconMonth.visibility = View.VISIBLE
                }

                CurrencyEnum.USDT_TRC20 -> {
                    binding.somIcon.setImageResource(R.drawable.usdt_icon)
                    binding.somTitle.text = context.getString(R.string.usdt)
                    binding.somCount.text = item.balance.formatBalanceNew()
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.GONE
                }

                CurrencyEnum.ESOM -> {
                    binding.somIcon.setImageResource(R.drawable.salam_icon)
                    binding.somTitle.text = context.getString(R.string.digital)
                    binding.somCount.text = item.balance.formatBalanceNew()
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.GONE
                }
            }

            binding.number.text = when (item.currency) {
                CurrencyEnum.SOM -> phoneProvider()
                    ?.takeLast(3)
                    ?.let { "*$it" }
                    ?: "*${item.address.takeLast(3)}"
                else -> "*${item.address.takeLast(3)}"
            }
        }

        private fun getSwapCurrencies(currency: CurrencyEnum): Pair<CurrencyEnum, CurrencyEnum> =
            when (currency) {
                CurrencyEnum.SOM -> CurrencyEnum.SOM to CurrencyEnum.ESOM
                CurrencyEnum.ESOM -> CurrencyEnum.ESOM to CurrencyEnum.SOM
                CurrencyEnum.USDT_TRC20 -> CurrencyEnum.ESOM to CurrencyEnum.USDT_TRC20
            }

        private fun getConvertTitle(currency: CurrencyEnum): String =
            when (currency) {
                CurrencyEnum.SOM -> context.getString(R.string.convert_from_som_to_salam)
                CurrencyEnum.ESOM -> context.getString(R.string.convert_from_salam_to_som)
                CurrencyEnum.USDT_TRC20 -> context.getString(R.string.convert_from_salam_to_usdt)
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val layoutInflater =
            CardPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(layoutInflater)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CardDiffCallback : DiffUtil.ItemCallback<WalletModel>() {
        override fun areItemsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean {
            return oldItem.currency == newItem.currency && oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: WalletModel, newItem: WalletModel): Boolean {
            return oldItem == newItem
        }
    }
}
