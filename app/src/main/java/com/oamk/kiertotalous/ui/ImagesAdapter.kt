package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableList
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.ui.base.BaseAdapter

class ImagesAdapter(imageItems: ObservableList<ImageItemData>, largeImage: Boolean) : BaseAdapter() {
    private val items: ObservableList<ImageItemData> = imageItems

    override fun getLayoutIdForPosition(position: Int): Int {
        return R.layout.list_item_image
    }

    override fun getDataForPosition(position: Int): Any {
        return items[position]
    }

    override fun getItemCount(): Int {
        return items.count()
    }
}
