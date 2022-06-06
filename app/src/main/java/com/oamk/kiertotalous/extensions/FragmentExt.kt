package com.oamk.kiertotalous.extensions

import com.google.android.material.textfield.TextInputLayout
import com.oamk.kiertotalous.R

fun TextInputLayout.validate(minValue: Number, maxValue: Number) {
    val inputValue = editText?.text?.toString()?.toFloatOrNull() ?: -1F
    if (inputValue < minValue.toFloat() || inputValue > maxValue.toFloat()) {
        error = context.getString(R.string.error_check_input_value, minValue.toString(), maxValue.toString())
    } else {
        error = null
    }
}

fun TextInputLayout.isValid() = error == null