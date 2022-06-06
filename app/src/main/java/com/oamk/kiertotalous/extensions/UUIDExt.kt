package com.oamk.kiertotalous.extensions

import java.util.*

fun UUID.toUidString(): String {
    return toString().replace("-", "").substring(0, 20)
}
