package com.esom.bank.screens.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.databinding.ItemMessageDateBinding
import com.esom.bank.databinding.ItemMessageReceiverBinding
import com.esom.bank.databinding.ItemMessageSenderBinding

class ChatAdapter() : ListAdapter<ChatAdapter.MessageItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {
    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_SENDER = 1
        private const val TYPE_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MessageItem.Date -> TYPE_DATE
            is MessageItem.ReceiverMessage -> TYPE_RECEIVER
            is MessageItem.SenderMessage -> TYPE_SENDER
        }
    }

    inner class SenderViewHolder(private val binding: ItemMessageSenderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.time.text = message.time
            binding.message.text = message.message
        }
    }

    inner class ReceiverViewHolder(private val binding: ItemMessageReceiverBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.name.text = message.name
            binding.time.text = message.time
            binding.message.text = message.message
        }
    }

    inner class DateViewHolder(private val binding: ItemMessageDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.date.text = date
        }
    }

    sealed class MessageItem {
        data class SenderMessage(val message: Message) : MessageItem()
        data class ReceiverMessage(val message: Message) : MessageItem()
        data class Date(val date: String) : MessageItem()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemMessageDateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateViewHolder(binding)
            }

            TYPE_SENDER -> {
                val binding = ItemMessageSenderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SenderViewHolder(binding)
            }

            TYPE_RECEIVER -> {
                val binding = ItemMessageReceiverBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceiverViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateViewHolder -> {
                holder.bind((getItem(position) as MessageItem.Date).date)
            }

            is ReceiverViewHolder -> {
                holder.bind((getItem(position) as MessageItem.ReceiverMessage).message)
            }

            is SenderViewHolder -> {
                holder.bind((getItem(position) as MessageItem.SenderMessage).message)
            }
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatAdapter.MessageItem>() {
    override fun areItemsTheSame(
        oldItem: ChatAdapter.MessageItem,
        newItem: ChatAdapter.MessageItem
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: ChatAdapter.MessageItem,
        newItem: ChatAdapter.MessageItem
    ): Boolean {
        return oldItem == newItem
    }

}

data class Message(
    val message: String,
    val time: String,
    val name: String = ""
)
