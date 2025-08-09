package com.esom.bank.screens.notification.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Notification(
    val title: String,
    val opinion: String?,
    val isImportant: Boolean
): Parcelable
