package com.oamk.kiertotalous.model

import kotlinx.serialization.Serializable

@Serializable
data class Location (
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Serializable
data class Site(
    val uid: String = "",
    val name: String = "",
    val address: String = "",
    val zipCode: String = "",
    val city: String = "",
    val phone: String = "",
    val location: Location = Location(latitude = 0.0, longitude = 0.0),
    val created: String = "",
    val modified: String = ""
)