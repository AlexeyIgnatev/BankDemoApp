package com.esom.bank.common.utils

import android.os.Build

object PhoneInfo {

    fun getFormattedPhoneInfo(): String {
        val manufacturer = Build.MANUFACTURER.trim()
        val model = Build.MODEL.trim()

        val cleanModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.substring(manufacturer.length).trim()
        } else {
            model
        }

        return "$manufacturer $cleanModel".trim()
    }
}