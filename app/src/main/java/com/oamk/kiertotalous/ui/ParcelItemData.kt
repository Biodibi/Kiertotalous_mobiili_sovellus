package com.oamk.kiertotalous.ui

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.oamk.kiertotalous.model.Parcel

class ParcelItemData private constructor(): BaseObservable() {
    interface Listener {
        fun onDeleteClicked(item: ParcelItemData)
    }

    var parcel: Parcel? = null

    @get:Bindable
    var parcelInfo: String? = null

    @get:Bindable
    var onDeleteClickListener: Listener? = null

    fun onDeleteClicked() {
        onDeleteClickListener?.onDeleteClicked(this)
    }

    constructor(parcel: Parcel, parcelType: String, onDeleteClickListener: Listener? = null) : this() {
        this.parcel = parcel
        this.parcelInfo = "${parcel.weight} kg (${parcel.count} $parcelType)"
        this.onDeleteClickListener = onDeleteClickListener
    }
}