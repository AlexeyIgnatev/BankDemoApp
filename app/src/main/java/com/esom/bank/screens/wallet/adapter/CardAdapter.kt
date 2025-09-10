package com.esom.bank.screens.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.CardPageBinding
import com.esom.bank.screens.main.enums.CurrencyEnum

class CardAdapter(private val context: Context,
                  private val onSwapClick: (CurrencyEnum) -> Unit,
                  private val onReceiveClick: () -> Unit,
                  private val onTransferClick: () -> Unit) :
    ListAdapter<Card, CardAdapter.CardViewHolder>(CardDiffCallback()) {
    inner class CardViewHolder(private val binding: CardPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Card) {
            binding.convertBtn.setOnClickListener {
                val currencyType = when (item.type) {
                    TypeOfCard.CARD -> CurrencyEnum.SOM
                    TypeOfCard.DIGITAL -> CurrencyEnum.ESOM
                    TypeOfCard.USDT -> CurrencyEnum.USDT_TRC20
                    TypeOfCard.BITCOIN -> CurrencyEnum.BTC
                    TypeOfCard.ETH -> CurrencyEnum.ETH
                }
                onSwapClick(currencyType)
            }
            binding.acceptBtn.setOnClickListener {
                onReceiveClick()
            }
            binding.transferBtn.setOnClickListener {
                onTransferClick()
            }
            when (item.type) {
                TypeOfCard.CARD -> {
                    binding.somIcon.setImageResource(R.drawable.som_icon)
                    binding.somTitle.text = context.getString(R.string.som)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.icon_sum_som)
                    binding.somIconMonth.visibility = View.VISIBLE
                    binding.newConvertLayout.visibility = View.VISIBLE
                }

                TypeOfCard.USDT -> {
                    binding.somIcon.setImageResource(R.drawable.usdt_icon)
                    binding.somTitle.text = context.getString(R.string.usdt)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.GONE
                }

                TypeOfCard.BITCOIN -> {
                    binding.somIcon.setImageResource(R.drawable.bitcoin_icon)
                    binding.somTitle.text = context.getString(R.string.bitcoin)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.GONE
                }

                TypeOfCard.ETH -> {
                    binding.somIcon.setImageResource(R.drawable.eth_icon)
                    binding.somTitle.text = context.getString(R.string.ethereum)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.GONE
                }

                TypeOfCard.DIGITAL -> {
                    binding.somIcon.setImageResource(R.drawable.salam_icon)
                    binding.somTitle.text = context.getString(R.string.digital)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                    binding.somIconMonth.visibility = View.VISIBLE
                    binding.newConvertLayout.visibility = View.VISIBLE
                    binding.convertTitle.text = context.getString(R.string.convert_to_salam)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val layoutInflater =
            CardPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(layoutInflater)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val realPosition = position % currentList.size
        holder.bind(getItem(realPosition))
    }

}

class CardDiffCallback : DiffUtil.ItemCallback<Card>() {
    override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem.number == newItem.number
    }

    override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
        return oldItem == newItem
    }

}

data class Card(
    val type: TypeOfCard,
    val sum: String,
    val number: String
)

enum class TypeOfCard() {
    CARD(),
    USDT(),
    BITCOIN(),
    ETH(),
    DIGITAL()
}