package com.oamk.kiertotalous.ui

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.oamk.kiertotalous.model.Delivery
import com.oamk.kiertotalous.model.Site
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class DeliveryItemData private constructor(): BaseObservable() {
    interface Listener {
        fun onItemClicked(delivery: Delivery)
        fun onCheckBoxChecked(delivery: Delivery)
    }

    @get:Bindable
    var time: String? = null
        private set

    @get:Bindable
    var title: String? = null
        private set

    @get:Bindable
    var weight: String? = null
        private set

    @get:Bindable
    var isChecked: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    var delivery: Delivery? = null
    var site: Site? = null

    var onItemListener: Listener? = null
        private set

    constructor(delivery: Delivery, site: Site, onItemListener: Listener? = null) : this() {
        this.delivery = delivery
        this.site = site

        val date = Date.from(Instant.parse(delivery.created))
        this.time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        this.title = site.name

        var totalWeight = 0F
        delivery.parcels.forEach { parcel ->
            totalWeight = totalWeight.plus(parcel.weight)
        }

        this.weight = "$totalWeight kg"
        this.onItemListener = onItemListener
    }

    fun onItemClicked() {
        delivery?.let { delivery ->
            onItemListener?.onItemClicked(delivery)
        }
    }

    fun onCheckboxClicked() {
        isChecked = !isChecked
        delivery?.let { delivery ->
            onItemListener?.onCheckBoxChecked(delivery)
        }
    }
}