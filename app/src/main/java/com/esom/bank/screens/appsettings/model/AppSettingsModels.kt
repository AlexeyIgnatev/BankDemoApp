package com.esom.bank.screens.appsettings.model

import android.view.View
import com.google.android.material.switchmaterial.SwitchMaterial

class AppSettingsRowSpec(
    val title: String,
    val subtitle: String,
    val icon: Int,
    val keywords: String,
    val hasSwitch: Boolean = false,
    val destructive: Boolean = false,
    val action: () -> Unit
)

class AppSettingsCreatedRow(val view: View, val switch: SwitchMaterial?)

class AppSettingsSearchRow(
    val view: View,
    val keywords: String,
    val switch: SwitchMaterial?
)

class AppSettingsSection(val card: View, val rows: List<AppSettingsSearchRow>)
