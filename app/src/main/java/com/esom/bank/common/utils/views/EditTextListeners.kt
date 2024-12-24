package com.esom.bank.common.utils.views

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.CheckResult
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart


fun View.hideKeyboard() {
    val imm =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun TextInputLayout.getText() = editText!!.text.toString()

fun EditText.addFocusListener(onFocusChanged: (Boolean) -> Unit = {}) {
    setOnFocusChangeListener { _, hasFocus ->
        onFocusChanged(hasFocus)
        if (!isFocused) {
            hideKeyboard()
        }
    }
}

fun List<EditText>.addFocusListeners(onFocusAcquired: () -> Unit = {}) {
    for (editText in this) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onFocusAcquired()
            }
            if (!any { it.isFocused }) {
                editText.hideKeyboard()
            }
        }
    }
}

fun Activity.showKeyboard() {
    val view = currentFocus
    val methodManager =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    methodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun View.showKeyboard() {
    val methodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    methodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.onActionDone(func: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->

        if (actionId == EditorInfo.IME_ACTION_DONE) {
            func()
        }
        actionId == EditorInfo.IME_ACTION_DONE
    }
}

fun EditText.onActionNext(func: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->

        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            func()
        }
        actionId == EditorInfo.IME_ACTION_NEXT
    }
}

@ExperimentalCoroutinesApi
@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow {
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                trySend(s)
            }
        }
        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }
}

// Define a unique key for the tag
private const val PROGRAMMATIC_CHANGE_KEY = -1

// Extension function to add a custom text change listener for user-initiated changes
fun EditText.setOnUserTextChangeListener(onTextChanged: (String) -> Unit) {
    this.setTag(PROGRAMMATIC_CHANGE_KEY, false)

    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val isProgrammaticChange = this@setOnUserTextChangeListener.getTag(PROGRAMMATIC_CHANGE_KEY) as? Boolean ?: false
            if (!isProgrammaticChange) {
                onTextChanged(s.toString())
            }
        }
    })
}

// Extension function to set text programmatically
fun EditText.setTextProgrammatically(text: String) {
    this.setTag(PROGRAMMATIC_CHANGE_KEY, true)
    this.setText(text)
    this.setTag(PROGRAMMATIC_CHANGE_KEY, false)
}