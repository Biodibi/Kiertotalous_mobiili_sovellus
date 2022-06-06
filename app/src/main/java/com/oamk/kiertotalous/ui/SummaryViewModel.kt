package com.oamk.kiertotalous.ui

import android.content.Context
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.model.Delivery
import kotlinx.coroutines.launch

class SummaryViewModel(private val remoteDataRepository: RemoteDataRepository, private val context : Context) : ViewModel() {
    val totalWeight = ObservableField<String>()
    val parcelsInfo = ObservableField<String>()
    val description = ObservableField<String>()
    val imageItems = ObservableArrayList<ImageItemData>()

    fun update(delivery: Delivery) {
        viewModelScope.launch {
            imageItems.clear()

            var totalWeightValue = 0F
            val parcelsInfoBuilder = StringBuilder()

            delivery.parcels.forEachIndexed { index, parcel ->
                totalWeightValue += parcel.weight
                var parcelType = ""
                context.resources.getStringArray(R.array.entries_parcel_keys).forEachIndexed { parcelKeyIndex, type ->
                    if (type == parcel.type) {
                        parcelType = context.resources.getStringArray(R.array.entries_parcel_values)[parcelKeyIndex] ?: ""
                    }
                }
                if (index == delivery.parcels.count() - 1) {
                    parcelsInfoBuilder.append("${parcel.weight} kg (${parcel.count} $parcelType)")
                } else {
                    parcelsInfoBuilder.appendLine("${parcel.weight} kg (${parcel.count} $parcelType)")
                }
            }

            if (delivery.parcels.count() > 1) {
                totalWeight.set("$totalWeightValue kg")
                parcelsInfo.set(parcelsInfoBuilder.toString())
            } else {
                totalWeight.set(parcelsInfoBuilder.toString())
                parcelsInfo.set("")
            }

            description.set(delivery.description)
            delivery.fileReferences.forEach { fileReference ->
                val imageRef = remoteDataRepository.getStorageReference(fileReference)
                val localFile = ImageItemData(imageRef)
                imageItems.add(localFile)
            }
        }
    }
}