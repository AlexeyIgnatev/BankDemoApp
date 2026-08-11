package com.esom.bank.screens.notification.model

import androidx.annotation.Keep

@Keep
sealed class NotificationListItem {
    data class DateItem(val date: String, val timestamp: Long) : NotificationListItem()

    data class NotificationItem(val notification: NotificationModel) : NotificationListItem()
}
