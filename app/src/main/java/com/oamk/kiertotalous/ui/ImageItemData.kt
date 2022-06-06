package com.oamk.kiertotalous.ui

import android.view.ViewGroup
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import com.google.firebase.storage.StorageReference
import com.oamk.kiertotalous.model.FileInfo


class ImageItemData private constructor(): BaseObservable() {
    interface Listener {
        fun onDeleteClicked(item: ImageItemData)
    }

    @get:Bindable
    var reference: StorageReference? = null
        private set

    @get:Bindable
    var fileInfo: FileInfo? = null
        private set

    @get:Bindable
    var isThumbnail: Boolean = false
        private set

    @get:Bindable
    var onDeleteClickListener: Listener? = null

    fun onDeleteClicked() {
        onDeleteClickListener?.onDeleteClicked(this)
    }

    constructor(reference: StorageReference) : this() {
        this.reference = reference
    }

    constructor(fileInfo: FileInfo, onDeleteClickListener: Listener? = null) : this() {
        this.fileInfo = fileInfo
        this.isThumbnail = true
        this.onDeleteClickListener = onDeleteClickListener
    }
}