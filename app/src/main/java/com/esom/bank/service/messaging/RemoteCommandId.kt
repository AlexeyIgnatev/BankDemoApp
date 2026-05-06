package com.esom.bank.service.messaging

import androidx.annotation.Keep

@Keep
enum class RemoteCommandId(val value: String) {
    SHOW_NOTIFICATION("showNotification"),
    SIGMA_FARM_REWARD("sigmaFarmReward"),
    BSB_OFFER_TAKEN("bsbOfferTaken");

    companion object {
        fun fromValue(value: String): RemoteCommandId? = entries.firstOrNull { it.value == value }
    }
}
