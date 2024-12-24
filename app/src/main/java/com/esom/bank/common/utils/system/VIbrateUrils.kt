package com.esom.bank.common.utils.system

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View

val ONE_TIME_VIBRATION_LONG = when (Build.BRAND.lowercase()) {
    "google" -> 8L
    "samsung" -> 2L
    else -> 50L
}

fun Context.vibrate(long: Long) {
    vibrateInternal(long)
}

private fun Context.vibrateInternal(long: Long) {
    Log.d("VibrateUtils", "vibrate: $long")
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(long, VibrationEffect.DEFAULT_AMPLITUDE),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                VibrationEffect.createOneShot(long, VibrationEffect.DEFAULT_AMPLITUDE),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(long)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun View.setVibrateClickListener(body: (() -> Unit)?) {
    if (body == null) {
        setOnClickListener(null)
    } else {
        setOnClickListener {
            context.vibrateInternal(ONE_TIME_VIBRATION_LONG)
            body()
        }
    }
}