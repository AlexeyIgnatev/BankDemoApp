package com.esom.bank.common.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

@Keep

@Parcelize

data class ErrorResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("statusCode")
    val statusCode: Int? = null
) : Parcelable {
    companion object {
        fun fromJson(raw: String?, fallbackStatusCode: Int? = null): ErrorResponse? {
            if (raw.isNullOrBlank()) return null

            return runCatching {
                val root = JsonParser.parseString(raw).asJsonObject
                val messageElement = root.get("message")
                val message = when {
                    messageElement == null || messageElement.isJsonNull -> null
                    messageElement.isJsonArray -> messageElement.asJsonArray
                        .mapNotNull { item ->
                            item.takeUnless { it.isJsonNull }?.asString?.trim()
                        }
                        .filter { it.isNotEmpty() }
                        .joinToString(". ")
                        .ifBlank { null }
                    else -> messageElement.asString.trim().ifBlank { null }
                }
                val statusCode = root.get("statusCode")
                    ?.takeUnless { it.isJsonNull }
                    ?.asInt
                    ?: fallbackStatusCode

                ErrorResponse(message = message, statusCode = statusCode)
            }.getOrNull()
        }
    }
}
