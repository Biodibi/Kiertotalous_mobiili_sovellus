package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableList
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.ui.base.BaseAdapter
import com.oamk.kiertotalous.ui.base.BaseViewHolder
import java.util.*

class DeliveriesAdapter(deliveryItems: ObservableList<DeliveryItemData>, private val startDragListener: OnStartDragListener) : BaseAdapter(), ItemMoveCallback.Listener {
    val items: ObservableList<DeliveryItemData> = deliveryItems

    override fun getLayoutIdForPosition(position: Int): Int {
        return R.layout.list_item_delivery
    }

    override fun getDataForPosition(position: Int): Any {
        return items[position]
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(itemViewHolder: BaseViewHolder) { }

    override fun onRowClear(itemViewHolder: BaseViewHolder) { }

    override fun onOrderChanged() {
        startDragListener.onOrderChanged()
    }
}
