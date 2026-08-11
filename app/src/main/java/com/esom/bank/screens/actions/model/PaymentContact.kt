package com.esom.bank.screens.actions.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import java.util.Locale

@Keep
@Parcelize
data class PaymentContact(
    val name: String,
    val phone: String
) : Parcelable {
    val initials: String
        get() = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase(Locale.getDefault())
            .ifBlank { "?" }
}
