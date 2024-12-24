package com.esom.bank.common.utils.views

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.esom.bank.databinding.LayoutSnackbarErrorBinding
import com.esom.bank.databinding.LayoutSnackbarSuccessBinding
import com.google.android.material.snackbar.Snackbar


fun View.showErrorSnackbar(text: String, anchorView: View? = null, action: () -> Unit = {}) {
    val snackbar = Snackbar.make(this, "", Snackbar.LENGTH_LONG)
    val layoutInflater = LayoutInflater.from(context)

    val errorViewBinding = LayoutSnackbarErrorBinding.inflate(layoutInflater)
    errorViewBinding.text.text = text
    errorViewBinding.icClose.setOnClickListener {
        action()
        snackbar.dismiss()
    }

    snackbar.view.setBackgroundColor(Color.TRANSPARENT)

    val snackbarLayout = snackbar.view as FrameLayout
    snackbarLayout.setPadding(0, 0, 0, 0)

    snackbarLayout.addView(errorViewBinding.root, 0)

    anchorView?.let {
        snackbar.setAnchorView(it)
    }

    snackbar.show()
}

fun View.showSuccessSnackbar(text: String, anchorView: View? = null, action: () -> Unit = {}) {
    val snackbar = Snackbar.make(this, "", Snackbar.LENGTH_LONG)
    val layoutInflater = LayoutInflater.from(context)

    val errorViewBinding = LayoutSnackbarSuccessBinding.inflate(layoutInflater)
    errorViewBinding.text.text = text
    errorViewBinding.icClose.setOnClickListener {
        action()
        snackbar.dismiss()
    }

    snackbar.view.setBackgroundColor(Color.TRANSPARENT)

    val snackbarLayout = snackbar.view as FrameLayout
    snackbarLayout.setPadding(0, 0, 0, 0)

    snackbarLayout.addView(errorViewBinding.root, 0)

    anchorView?.let {
        snackbar.setAnchorView(it)
    }

    snackbar.show()
}