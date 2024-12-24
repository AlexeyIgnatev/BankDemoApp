package com.esom.bank.common.utils.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.esom.bank.activities.MainActivity
import com.esom.bank.R


fun Context.reloadApp() {
    val intent = Intent(this, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    startActivity(intent)
}

fun Context.showToast(message: String?) {
    android.os.Handler(Looper.getMainLooper()).post {
        Toast.makeText(
            this,
            message ?: getString(R.string.something_went_wrong),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun Activity.syncStatusBarWithTheme() {
    if (isDarkMode()) {
        clearLightStatusBar()
    } else {
        setLightStatusBar()
    }
}

fun Activity.isDarkMode(): Boolean {
    val nightModeFlags: Int = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
    return when (nightModeFlags) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> false
    }
}

fun Activity.setLightStatusBar() {
    var flags = window.decorView.systemUiVisibility
    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    window.decorView.systemUiVisibility = flags
}

fun Activity.clearLightStatusBar() {
    var flags = window.decorView.systemUiVisibility
    flags =
        flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    window.decorView.systemUiVisibility = flags
}