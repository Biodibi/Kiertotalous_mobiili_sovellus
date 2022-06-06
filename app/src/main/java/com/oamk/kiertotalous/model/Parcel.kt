package com.oamk.kiertotalous.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Parcel(
    var weight: Float = 0F,
    val count: Int = 0,
    val type: String = "",
) : Parcelable