package com.esom.bank.screens.settigns.model

class SettingsService(
    val title: String,
    val subtitle: String,
    val icon: Int,
    val action: () -> Unit
)
