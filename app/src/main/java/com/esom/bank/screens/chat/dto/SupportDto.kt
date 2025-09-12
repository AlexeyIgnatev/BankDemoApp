package com.esom.bank.screens.chat.dto

import com.esom.bank.screens.chat.enums.SupportRole
import com.google.gson.annotations.SerializedName

data class SupportDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("text")
    val text: String,
    @SerializedName("role")
    val role: SupportRole,
    @SerializedName("created_at")
    val createdAt: Long
)