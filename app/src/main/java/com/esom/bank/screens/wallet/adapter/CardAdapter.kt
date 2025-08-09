package com.esom.bank.screens.wallet.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.CardPageBinding

class CardAdapter(private val context: Context) :
    ListAdapter<Card, CardAdapter.CardViewHolder>(CardDiffCallback()) {
    inner class CardViewHolder(private val binding: CardPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Card) {
            when (item.type) {
                TypeOfCard.CARD -> {
                    binding.somIcon.setImageResource(R.drawable.som_icon)
                    binding.somTitle.text = context.getString(R.string.som)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.icon_sum_som)
                }

                TypeOfCard.USDT -> {
                    binding.somIcon.setImageResource(R.drawable.usdt_icon)
                    binding.somTitle.text = context.getString(R.string.usdt)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                }

                TypeOfCard.BITCOIN -> {
                    binding.somIcon.setImageResource(R.drawable.bitcoin_icon)
                    binding.somTitle.text = context.getString(R.string.bitcoin)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                }

                TypeOfCard.ETH -> {
                    binding.somIcon.setImageResource(R.drawable.eth_icon)
                    binding.somTitle.text = context.getString(R.string.ethereum)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
                }

                TypeOfCard.DIGITAL -> {
                    binding.somIcon.setImageResource(R.drawable.digital_icon)
                    binding.somTitle.text = context.getString(R.string.digital)
                    binding.somCount.text = item.sum
                    binding.cardNumberIcon.setImageResource(R.drawable.wallet_icon)
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
        holder.bind(getItem(position))
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