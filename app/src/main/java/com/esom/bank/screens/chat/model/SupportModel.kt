package com.esom.bank.screens.chat.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.esom.bank.screens.chat.dto.SupportDto
import com.esom.bank.screens.chat.enums.SupportRole
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SupportModel(
    val id: Int,
    val text: String,
    val role: SupportRole,
    val createdAt: Long
) : Parcelable

fun SupportDto.toModel(): SupportModel = SupportModel(
    id = id,
    text = text,
    role = SupportRole.fromRaw(role),
    createdAt = createdAt
)

fun List<SupportDto>.toModel(): List<SupportModel> {
    return this.map { it.toModel() }
}
