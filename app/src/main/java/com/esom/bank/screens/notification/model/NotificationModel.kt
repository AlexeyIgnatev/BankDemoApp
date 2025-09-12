package com.esom.bank.screens.notification.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.notification.dto.NotificationDto
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class NotificationModel(
    val id: Int,
    val title: String,
    val text: String,
    val createdAt: Long
) : Parcelable

fun NotificationDto.toModel() = NotificationModel(
    id = id,
    title = title,
    text = text,
    createdAt = createdAt
)

fun List<NotificationDto>.toModel() : List<NotificationModel> {
    return this.map { it.toModel() }
}