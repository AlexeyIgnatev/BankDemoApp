package com.esom.bank.common.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

import com.google.gson.annotations.SerializedName

@Keep

@Parcelize

data class ErrorResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("statusCode")
    val statusCode: Int? = null
) : Parcelable