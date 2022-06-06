package com.oamk.kiertotalous.model

import kotlinx.serialization.Serializable

@Serializable
data class FuelConsumptionInfo (
    val type: String = "",
    val value: Float = 0F
)

@Serializable
data class Summary(
    val uid: String = "",
    val userId: String = "",
    val distance: Float = 0F,
    val fuelConsumptionInfo: FuelConsumptionInfo,
    val deliveries: List<String> = emptyList(),
    val sites: List<String> = emptyList(),
    val created: String = ""
)