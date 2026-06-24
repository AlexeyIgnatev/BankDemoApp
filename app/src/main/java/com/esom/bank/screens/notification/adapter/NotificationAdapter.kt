package com.esom.bank.screens.notification.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.ItemDataBinding
import com.esom.bank.databinding.ItemNotificationBinding
import com.esom.bank.screens.notification.model.NotificationModel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter :
    ListAdapter<NotificationAdapter.NotificationListItem, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_NOTIFICATION = 1
    }

    private val dateFormat = SimpleDateFormat("dd MMMM", Locale("ru")).apply {
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        dateFormatSymbols = object : DateFormatSymbols(Locale("ru")) {
            override fun getMonths(): Array<String> {
                return months
            }
        }
    }
    sealed class NotificationListItem {
        data class DateItem(val date: String, val timestamp: Long) : NotificationListItem()
        data class NotificationItem(val notification: NotificationModel) : NotificationListItem()
    }

    fun submitNotifications(notifications: List<NotificationModel>) {
        val items = mutableListOf<NotificationListItem>()
        var lastDate = ""

        val sortedNotifications = notifications.sortedByDescending { it.createdAt }

        sortedNotifications.forEach { notification ->
            val currentDate = dateFormat.format(Date(notification.createdAt))

            if (currentDate != lastDate) {
                items.add(NotificationListItem.DateItem(currentDate, notification.createdAt))
                lastDate = currentDate
            }
            items.add(NotificationListItem.NotificationItem(notification))
        }

        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.DateItem -> TYPE_DATE
            is NotificationListItem.NotificationItem -> TYPE_NOTIFICATION
        }
    }

    class DateViewHolder(private val binding: ItemDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dateItem: NotificationListItem.DateItem) {
            binding.date.text = dateItem.date
        }
    }

    class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notificationItem: NotificationListItem.NotificationItem) {
            val notification = notificationItem.notification
            binding.title.text = notification.title
            binding.icon.visibility = View.VISIBLE
            binding.opinion.text = notification.text

            binding.mainLayout.setOnClickListener {
                if (binding.opinionLayout.visibility == View.GONE) {
                    binding.icon.setImageResource(R.drawable.arrow_bottom)
                    binding.opinionLayout.visibility = View.VISIBLE
                } else {
                    binding.icon.setImageResource(R.drawable.arrow_bottom_gray)
                    binding.opinionLayout.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateViewHolder(binding)
            }

            TYPE_NOTIFICATION -> {
                val binding = ItemNotificationBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                NotificationViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateViewHolder -> holder.bind(getItem(position) as NotificationListItem.DateItem)
            is NotificationViewHolder -> holder.bind(getItem(position) as NotificationListItem.NotificationItem)
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationAdapter.NotificationListItem>() {
    override fun areItemsTheSame(oldItem: NotificationAdapter.NotificationListItem, newItem: NotificationAdapter.NotificationListItem): Boolean {
        return when {
            oldItem is NotificationAdapter.NotificationListItem.DateItem && newItem is NotificationAdapter.NotificationListItem.DateItem ->
                oldItem.timestamp == newItem.timestamp
            oldItem is NotificationAdapter.NotificationListItem.NotificationItem && newItem is NotificationAdapter.NotificationListItem.NotificationItem ->
                oldItem.notification.id == newItem.notification.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: NotificationAdapter.NotificationListItem, newItem: NotificationAdapter.NotificationListItem): Boolean {
        return oldItem == newItem
    }
}