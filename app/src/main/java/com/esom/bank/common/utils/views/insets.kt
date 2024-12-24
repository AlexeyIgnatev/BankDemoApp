package com.esom.bank.common.utils.views

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.internal.ViewUtils.requestApplyInsetsWhenAttached

private const val TAG = "insets"

@SuppressLint("RestrictedApi")
fun View.doOnApplyWindowInsets(block: (View, WindowInsetsCompat, Rect) -> WindowInsetsCompat) {

    val initialPadding = recordInitialPaddingForView(this)

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        block(v, insets, initialPadding)
    }
    val instantInsets = ViewCompat.getRootWindowInsets(this)
    instantInsets?.let {
        block(this, it, initialPadding)
    }

    requestApplyInsetsWhenAttached(this)
}

private fun recordInitialPaddingForView(view: View) =
    Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

private fun recordInitialMarginForView(view: View) =
    Rect(view.marginLeft, view.marginTop, view.marginRight, view.marginBottom)

fun View.addTopAndBottomPaddings() {
    doOnApplyWindowInsets { view, insets, rect ->
        view.updatePadding(
            top = rect.top + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
            bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        )
        insets
    }
}

fun View.addBottomNavigationMargin(
    targetView: View = this,
    excludeInsets: Boolean = true
) {
    doOnApplyWindowInsets { _, insets, _ ->
        targetView.isVisible = !insets.isVisible(WindowInsetsCompat.Type.ime())

        targetView.post {
            targetView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            }
        }

        if (excludeInsets) {
            insets.exclude(WindowInsetsCompat.Type.navigationBars())
        } else {
            insets
        }
    }
}

fun WindowInsetsCompat.exclude(flags: Int) =
    WindowInsetsCompat.Builder(this)
        .setInsets(flags, Insets.of(Rect())).build()

fun View.isKeyboardVisible(): Boolean {
    val insets = ViewCompat.getRootWindowInsets(this) ?: return false
    return insets.isVisible(WindowInsetsCompat.Type.ime())
}