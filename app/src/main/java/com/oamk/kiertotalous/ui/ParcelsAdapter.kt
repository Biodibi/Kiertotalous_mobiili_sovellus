package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableList
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.ui.base.BaseAdapter

class ParcelsAdapter(items: ObservableList<ParcelItemData>) : BaseAdapter() {
    private val items: ObservableList<ParcelItemData> = items

    override fun getLayoutIdForPosition(position: Int): Int {
        return R.layout.list_item_parcel
    }

    override fun getDataForPosition(position: Int): Any {
        return items[position]
    }

    override fun getItemCount(): Int {
        return items.count()
    }
}
