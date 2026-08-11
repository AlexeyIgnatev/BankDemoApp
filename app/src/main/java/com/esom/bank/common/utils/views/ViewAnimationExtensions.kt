package com.esom.bank.common.utils.views

import android.view.View

fun View.slideInFromTop(distancePx: Float, durationMs: Long = 180L) {
    alpha = 0f
    visibility = View.VISIBLE
    post {
        translationY = -distancePx
        animate().translationY(0f).alpha(1f).setDuration(durationMs).start()
    }
}

fun View.slideInFromBottom(durationMs: Long = 450L) {
    alpha = 0f
    visibility = View.VISIBLE
    post {
        translationY = height.toFloat()
        animate().translationY(0f).alpha(1f).setDuration(durationMs).start()
    }
}

fun View.slideOut(
    translationY: Float,
    durationMs: Long,
    onEnd: (() -> Unit)? = null
) {
    animate()
        .translationY(translationY)
        .alpha(0f)
        .setDuration(durationMs)
        .withEndAction {
            visibility = View.GONE
            this.translationY = 0f
            onEnd?.invoke()
        }
        .start()
}
