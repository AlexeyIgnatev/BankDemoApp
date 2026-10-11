package com.esom.bank.common.utils.views

import android.text.InputFilter
import android.text.TextWatcher
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.widget.EditText
import java.math.BigDecimal

fun EditText.setupDecimalAmountInput(decimalPlaces: Int = 2) {
    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    keyListener = DigitsKeyListener.getInstance("0123456789.,")
    val decimalFilters = filters
        .filterNot { it is DecimalAmountInputFilter }
        .plus(DecimalAmountInputFilter(decimalPlaces))
        .toTypedArray()
    filters = decimalFilters

    addTextChangedListener(object : TextWatcher {
        private var normalizing = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(editable: android.text.Editable?) {
            if (normalizing || editable == null) return
            val original = editable.toString()
            val normalized = normalizeDecimalAmountInput(original)
            if (original == normalized) return

            val oldSelection = selectionStart
            normalizing = true
            try {
                editable.replace(0, editable.length, normalized)
                if (oldSelection >= 0) {
                    setSelection((oldSelection - (original.length - normalized.length))
                        .coerceIn(0, normalized.length))
                }
            } finally {
                normalizing = false
            }
        }
    })
}

fun normalizeDecimalAmountInput(value: String): String {
    if (value.isEmpty()) return value
    val separatorIndex = value.indexOfFirst { it == '.' || it == ',' }
    val integerEnd = if (separatorIndex < 0) value.length else separatorIndex
    val integer = value.substring(0, integerEnd)
    val normalizedInteger = integer.dropWhile { it == '0' }
        .ifEmpty { if (integer.isEmpty()) "" else "0" }
    return normalizedInteger + value.substring(integerEnd)
}

fun String.toDecimalAmountOrNull(): BigDecimal? =
    trim().replace(',', '.').toBigDecimalOrNull()

private class DecimalAmountInputFilter(
    decimalPlaces: Int
) : InputFilter {
    private val acceptedValue = Regex("\\d*(?:[.,]\\d{0,$decimalPlaces})?")

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val result = buildString {
            append(dest.substring(0, dstart))
            append(source.subSequence(start, end))
            append(dest.substring(dend))
        }
        return if (acceptedValue.matches(result)) null else ""
    }
}
