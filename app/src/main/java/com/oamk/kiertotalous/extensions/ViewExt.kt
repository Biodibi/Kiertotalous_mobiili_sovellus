package com.oamk.kiertotalous.extensions

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*

fun View.launch(durationInMillis: Long, dispatcher: CoroutineDispatcher = Dispatchers.Main, block: () -> Unit
) : Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
    lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
        delay(durationInMillis)
        block()
    }
}

fun TextInputLayout.addTextChangedListener() {
    editText?.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            this@addTextChangedListener.error = null
        }
    })
}