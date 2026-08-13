package com.esom.bank.common.utils.views

import android.text.InputFilter
import android.text.Spanned
import android.text.method.DigitsKeyListener
import android.widget.EditText

fun EditText.setupDecimalAmountInput(decimalPlaces: Int = 2) {
    inputType = android.text.InputType.TYPE_CLASS_NUMBER or
        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    keyListener = DigitsKeyListener.getInstance("0123456789.,")
    val decimalFilters = filters
        .filterNot { it is DecimalAmountInputFilter }
        .plus(DecimalAmountInputFilter(decimalPlaces))
        .toTypedArray()
    filters = decimalFilters
}

fun String.toDecimalAmountOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

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
