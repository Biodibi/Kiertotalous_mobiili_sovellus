package com.oamk.kiertotalous.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class DeliveryStatus(val status: String) {
    OPEN("open"),
    COMPLETED("completed");
}

@Parcelize
data class Delivery(
    val uid: String = "",
    val userId: String = "",
    val siteId: String = "",
    val status: String = "",
    val description: String = "",
    val parcels: List<Parcel> = emptyList(),
    val fileReferences: List<String> = emptyList(),
    val created: String = "",
    val modified: String = ""
) : Parcelable