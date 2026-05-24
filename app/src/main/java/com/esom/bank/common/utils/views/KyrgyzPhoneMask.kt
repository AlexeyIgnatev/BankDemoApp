package com.esom.bank.common.utils.views

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText

const val KYRGYZ_PHONE_PREFIX = "+996 "
private const val KYRGYZ_PHONE_PREFIX_DIGITS = "996"
private const val KYRGYZ_LOCAL_PHONE_DIGITS = 9
private const val PHONE_MAX_LENGTH = 25
private const val MIN_PHONE_DIGITS = 10

fun EditText.applyKyrgyzPhoneMask(
    onPhoneChanged: ((isComplete: Boolean) -> Unit)? = null
): TextWatcher {
    inputType = InputType.TYPE_CLASS_PHONE
    filters = arrayOf(InputFilter.LengthFilter(PHONE_MAX_LENGTH))

    val watcher = object : TextWatcher {
        private var isFormatting = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(s: Editable?) {
            if (isFormatting) return

            val current = s?.toString().orEmpty()
            if (shouldApplyKyrgyzFormat(current)) {
                val formatted = current.formatAsKyrgyzPhone()
                isFormatting = true
                if (current != formatted) {
                    setText(formatted)
                }
                setSelection(text?.length ?: 0)
                isFormatting = false
            }

            onPhoneChanged?.invoke(text.isValidPhoneInput())
        }
    }

    addTextChangedListener(watcher)
    if (text.isNullOrBlank()) {
        setText(KYRGYZ_PHONE_PREFIX.trim())
    }
    setSelection(text?.length ?: 0)
    onPhoneChanged?.invoke(text.isValidPhoneInput())
    return watcher
}

fun CharSequence?.kyrgyzPhoneDigits(): String {
    return this.phoneForApi()
}

fun CharSequence?.isCompleteKyrgyzPhone(): Boolean {
    return this.isValidPhoneInput()
}

fun CharSequence?.isValidPhoneInput(): Boolean {
    val raw = this?.toString()?.trim().orEmpty()
    val digits = raw.filter { it.isDigit() }
    return digits.length >= MIN_PHONE_DIGITS && (raw.startsWith("+") || digits.isNotEmpty())
}

fun CharSequence?.phoneForApi(): String {
    val raw = this?.toString()?.trim().orEmpty()
    val digits = raw.filter { it.isDigit() }
    return if (raw.startsWith("+")) "+$digits" else digits
}

private fun shouldApplyKyrgyzFormat(value: String): Boolean {
    if (value.isBlank()) return false
    val digits = value.filter { it.isDigit() }
    return value.startsWith("+996") || digits.startsWith(KYRGYZ_PHONE_PREFIX_DIGITS)
}

private fun CharSequence?.formatAsKyrgyzPhone(): String {
    val digits = kyrgyzLocalPhoneDigits()
    return buildString {
        append(KYRGYZ_PHONE_PREFIX.trim())
        if (digits.isNotEmpty()) append(' ')
        when {
            digits.length <= 3 -> append(digits)
            else -> {
                append("(")
                append(digits.substring(0, 3))
                append(") ")
                append(digits.substring(3, minOf(6, digits.length)))
                if (digits.length > 6) {
                    append("-")
                    append(digits.substring(6))
                }
            }
        }
    }
}

private fun CharSequence?.kyrgyzLocalPhoneDigits(): String {
    var digits = this?.toString()?.filter { it.isDigit() }.orEmpty()
    if (digits.startsWith(KYRGYZ_PHONE_PREFIX_DIGITS)) {
        digits = digits.removePrefix(KYRGYZ_PHONE_PREFIX_DIGITS)
    }
    return digits.take(KYRGYZ_LOCAL_PHONE_DIGITS)
}
