package com.esom.bank.screens.chat.dto

import com.google.gson.annotations.SerializedName

data class SupportDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("ticket_id")
    val ticketId: Int?,
    @SerializedName("text")
    val text: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("created_at")
    val createdAt: Long
)
