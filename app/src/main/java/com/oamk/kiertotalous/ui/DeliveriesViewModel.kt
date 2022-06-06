package com.oamk.kiertotalous.ui

import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentReference
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.data.LocalDataRepository
import com.oamk.kiertotalous.data.RemoteDataRepository
import com.oamk.kiertotalous.extensions.toUidString
import com.oamk.kiertotalous.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import timber.log.Timber
import java.time.Instant
import java.util.*

class DeliveriesViewModel(private val app: AppController,
                          private val remoteDataRepository: RemoteDataRepository,
                          private val localDataRepository: LocalDataRepository
) : ViewModel() {
    val totalWeight = ObservableField<String>()
    val totalWeightValue = ObservableField<Float>()

    var isInputFormVisible = ObservableBoolean(false)
    var isItemChecked = ObservableBoolean(false)
    var isButtonEnabled = ObservableBoolean(false)
    var isFetchDeliveriesProgressVisible = ObservableBoolean(true)
    var isFetchDeliveriesRetryButtonVisible = ObservableBoolean(false)
    var isSubmitSummaryInProgress = ObservableBoolean(false)

    var sites = listOf<Site>()
    val deliveryItems = ObservableArrayList<DeliveryItemData>()
    val previousSummary = localDataRepository.previousSummary

    private val _summarySubmitResult: MutableLiveData<FirebaseResult<DocumentReference>> = MutableLiveData()
    val summarySubmitResult: LiveData<FirebaseResult<DocumentReference>> = _summarySubmitResult

    private var subscribeToDeliveriesJob : Job? = null

    init {
        fetchSites()
    }

    override fun onCleared() {
        super.onCleared()

        deliveryItems.clear()
    }

    private fun fetchSites() {
        // Preload cached sites
        localDataRepository.sites?.let { cachedSites ->
            sites = cachedSites
        }

        viewModelScope.launch {
            remoteDataRepository.fetchSites().cancellable().collect { sitesResult ->
                when (sitesResult) {
                    is FirebaseResult.Success -> {
                        sites = sitesResult.data

                        localDataRepository.sites = sites
                        localDataRepository.orderedSiteIds?.reversed()?.forEach { site ->
                            sites = sites.sortedWith(compareBy { site == it.uid })
                        }
                    }
                    is FirebaseResult.Error -> {
                        Timber.e(sitesResult.error)
                    }
                }
            }
        }
    }

    fun subscribeToDeliveries() {
        subscribeToDeliveriesJob?.cancel()
        subscribeToDeliveriesJob = viewModelScope.launch {
            isFetchDeliveriesProgressVisible.set(true)
            isFetchDeliveriesRetryButtonVisible.set(false)

            remoteDataRepository.subscribeToDeliveries().collect { deliveriesResult ->
                when (deliveriesResult) {
                    is FirebaseResult.Success -> {
                        update(deliveriesResult.data)
                    }
                    is FirebaseResult.Error -> {
                        Timber.e(deliveriesResult.error)
                        app.showAppNotification(AppNotification(message = R.string.error_deliveries_subscription_failed, isError = true))
                        isFetchDeliveriesRetryButtonVisible.set(true)
                    }
                }
                isFetchDeliveriesProgressVisible.set(false)
            }
        }
    }

    fun unsubscribe() {
        remoteDataRepository.unsubscribe()
        subscribeToDeliveriesJob?.cancel()
    }

    private fun update(deliveries: List<Delivery>?) {
        // Sort by site and date
        var sortedDeliveries = deliveries?.sortedWith(compareByDescending { it.created })
        sites.reversed().forEach { site ->
            sortedDeliveries = sortedDeliveries?.sortedWith(compareBy { site.uid == it.siteId })
        }

        var totalWeight = 0F
        val items = mutableListOf<DeliveryItemData>()
        val sites = mutableListOf<Site>()
        val onItemListener = object : DeliveryItemData.Listener {
            override fun onItemClicked(delivery: Delivery) {
                app.navigate(NavInfo(R.id.summaryFragment, SummaryFragmentDirections.actionGlobalSummaryFragment(delivery)))
            }

            override fun onCheckBoxChecked(delivery: Delivery) {
                val isItemChecked = deliveryItems.firstOrNull { it.isChecked } != null
                if (!isItemChecked) {
                    this@DeliveriesViewModel.isItemChecked.set(false)
                }
                isInputFormVisible.set(this@DeliveriesViewModel.isItemChecked.get())
                isButtonEnabled.set(isItemChecked)
            }
        }

        sortedDeliveries?.forEach { delivery ->
            this.sites.firstOrNull { it.uid == delivery.siteId }?.let { site ->
                items.add(DeliveryItemData(delivery, site, onItemListener))

                delivery.parcels.forEach { parcel ->
                    totalWeight = totalWeight.plus(parcel.weight)
                }

                if (!sites.contains(site)) {
                    sites.add(site)
                }
            }
        }

        this.deliveryItems.clear()
        this.deliveryItems.addAll(items)
        this.totalWeightValue.set(totalWeight)
        this.totalWeight.set("$totalWeight kg")
    }

    fun updateSites(items : List<DeliveryItemData>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val orderedSites = mutableListOf<Site>()
                items.toMutableList().let { list ->
                    list.forEach { item ->
                        item.site?.let { site ->
                            if (!orderedSites.contains(site)) {
                                orderedSites.add(site)
                                sites = sites.sortedWith(compareBy { it.uid == site.uid })
                            }
                        }
                    }
                }

                val orderedSiteIds = mutableListOf<String>()
                sites.forEach { site ->
                    orderedSiteIds.add(site.uid)
                }
                localDataRepository.orderedSiteIds = orderedSiteIds
            }
        }
    }

    fun submitSummary(distance: Float, fuelConsumption: Float, type: String) {
        isSubmitSummaryInProgress.set(true)
        viewModelScope.launch {
            val deliveryIds = mutableListOf<String>()
            val siteIds = mutableListOf<String>()

            deliveryItems.forEach { item ->
                if (item.isChecked) {
                    item.delivery?.let { delivery ->
                        deliveryIds.add(delivery.uid)
                        siteIds.add(delivery.siteId)
                    }
                }
            }

            val uid = UUID.randomUUID().toUidString()
            val userId = localDataRepository.userAccount!!.userId
            val fuelConsumptionInfo = FuelConsumptionInfo(type, fuelConsumption)
            val created = Instant.now().toString()
            val summary = Summary(uid, userId, distance, fuelConsumptionInfo, deliveryIds, siteIds, created)

            remoteDataRepository.saveSummary(summary).cancellable().collect { result ->
                when (result) {
                    is FirebaseResult.Success -> {
                        isItemChecked.set(false)
                        isInputFormVisible.set(false)
                        isButtonEnabled.set(false)
                        _summarySubmitResult.value = result
                        localDataRepository.previousSummary = summary
                        app.showAppNotification(AppNotification(message = R.string.send_success))
                    }
                    is FirebaseResult.Error -> {
                        Timber.e(result.error)
                        _summarySubmitResult.value = result
                        app.showAppNotification(AppNotification(message = R.string.error_request_failed, isError = true))
                    }
                }
            }
            isSubmitSummaryInProgress.set(false)
        }
    }

    fun isDeliveryItemChecked(): Boolean {
        return deliveryItems.firstOrNull { it.isChecked } != null
    }

    fun showInputForm() {
        isItemChecked.set(true)

        if (!isInputFormVisible.get()) {
            isInputFormVisible.set(true)
        }
    }

    fun navigate(navInfo: NavInfo) {
        app.navigate(navInfo)
    }
}