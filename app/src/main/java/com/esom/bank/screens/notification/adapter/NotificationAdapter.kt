package com.esom.bank.screens.notification.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.ItemDataBinding
import com.esom.bank.databinding.ItemNotificationBinding
import com.esom.bank.screens.notification.model.Notification

class NotificationAdapter: ListAdapter<NotificationAdapter.NotificationItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_NOTIFICATION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is NotificationItem.Notifications -> TYPE_NOTIFICATION
            is NotificationItem.Date -> TYPE_DATE
        }
    }
    inner class DateViewHolder(private val binding: ItemDataBinding):
    RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.date.text = item
        }
    }
    inner class NotificationViewHolder(private val binding: ItemNotificationBinding):
    RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.title.text = notification.title
            if(notification.opinion != null) {
                binding.icon.visibility = View.VISIBLE
                binding.opinion.text = notification.opinion
            }

            if(notification.isImportant) {
                binding.title.setTextColor(Color.parseColor("#E62324"))
                binding.opinion.setTextColor(Color.parseColor("#E62324"))
                binding.icon.setColorFilter(Color.parseColor("#E62324"))
            }

            binding.mainLayout.setOnClickListener {
                if(notification.opinion != null) {
                    if(binding.opinionLayout.visibility == View.GONE) {
                        binding.icon.setImageResource(R.drawable.arrow_bottom)
                        binding.opinionLayout.visibility = View.VISIBLE
                    } else {
                        binding.icon.setImageResource(R.drawable.arrow_bottom_gray)
                        binding.opinionLayout.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_DATE -> {
                val inflater = ItemDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateViewHolder(inflater)
            }
            TYPE_NOTIFICATION -> {
                val inflater = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NotificationViewHolder(inflater)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is DateViewHolder -> holder.bind((getItem(position) as NotificationItem.Date).date)
            is NotificationViewHolder -> holder.bind((getItem(position) as NotificationItem.Notifications).notification)
        }
    }

    sealed class NotificationItem {
        data class Notifications(val notification: Notification): NotificationItem()
        data class Date(val date: String): NotificationItem()
    }
}

class NotificationDiffCallback: DiffUtil.ItemCallback<NotificationAdapter.NotificationItem>() {
    override fun areItemsTheSame(oldItem: NotificationAdapter.NotificationItem, newItem: NotificationAdapter.NotificationItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: NotificationAdapter.NotificationItem, newItem: NotificationAdapter.NotificationItem): Boolean {
        return oldItem == newItem
    }

}