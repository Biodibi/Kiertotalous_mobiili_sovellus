package com.oamk.kiertotalous.ui.base

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.oamk.kiertotalous.BR

open class BaseViewHolder(binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    val binding: ViewDataBinding? = binding

    fun bind(obj: Any?) {
        binding?.setVariable(BR.data, obj)
        binding?.executePendingBindings()
    }
}