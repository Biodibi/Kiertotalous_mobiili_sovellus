package com.oamk.kiertotalous


class Const {
    companion object {
        const val MIN_WEIGHT = 1F // kg
        const val MAX_WEIGHT = 500.0F // kg
        const val MIN_PARCELS_COUNT = 1
        const val MAX_PARCELS_COUNT = 5
        const val MIN_DISTANCE = 1.0F // km
        const val MAX_DISTANCE = 200F // km
        const val MIN_BIOGAS_CONSUMPTION = 0.1F // kg
        const val MAX_BIOGAS_CONSUMPTION = 10F // kg
        const val MIN_GASOLINE_CONSUMPTION = 0.1F // l
        const val MAX_GASOLINE_CONSUMPTION = 20F // l
        const val MIN_DIESEL_CONSUMPTION = 0.1F // l
        const val MAX_DIESEL_CONSUMPTION = 20F // l
        const val MIN_ELECTRIC_CONSUMPTION = 0.1F // kWh
        const val MAX_ELECTRIC_CONSUMPTION = 60F // kWh
        const val MIN_HYDROGEN_CONSUMPTION = 0.1F // kg
        const val MAX_HYDROGEN_CONSUMPTION = 10F // kg

        const val CHANNEL_ID = "kiertotalous"
        const val CHANNEL_NAME = "kiertotalous_channel"
        const val CHANNEL_DESCRIPTION = "Kiertotalous push notifications"
        const val TOPIC_DELIVERIES = "deliveries"
    }

}