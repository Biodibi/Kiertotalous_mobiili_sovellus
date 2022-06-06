package com.oamk.kiertotalous.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentReference
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.FragmentDeliveriesBinding
import com.oamk.kiertotalous.extensions.*
import com.oamk.kiertotalous.model.FirebaseResult
import com.oamk.kiertotalous.model.NavInfo
import org.koin.androidx.navigation.koinNavGraphViewModel

class DeliveriesFragment : Fragment(), OnStartDragListener {
    private val viewModel: DeliveriesViewModel by koinNavGraphViewModel(R.id.nav_graph)

    private var _viewDataBinding: FragmentDeliveriesBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        _viewDataBinding = FragmentDeliveriesBinding.inflate(inflater, container, false)

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = viewLifecycleOwner

        viewDataBinding.distanceInputLayout.addTextChangedListener()
        viewDataBinding.fuelConsumptionInputLayout.addTextChangedListener()

        viewModel.isButtonEnabled.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                requireActivity().invalidateOptionsMenu()
            }
        })

        viewDataBinding.deliveriesRecyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            DeliveriesAdapter(viewModel.deliveryItems, this@DeliveriesFragment).let { deliveriesAdapter ->
                viewModel.deliveryItems.subscribe(deliveriesAdapter)
                adapter = deliveriesAdapter
            }

            val itemMoveCallback = ItemMoveCallback(adapter as DeliveriesAdapter)
            touchHelper = ItemTouchHelper(itemMoveCallback)
            touchHelper.attachToRecyclerView(this)
        }

        viewDataBinding.formButton.setOnClickListener {
            viewDataBinding.focusableView.launch(10) {
                view.requestFocus()
                requireActivity().hideKeyboard()
            }

            if (viewModel.isInputFormVisible.get()) {
                submitSummary()
            } else {
                viewModel.showInputForm()
            }
        }

        viewModel.previousSummary?.let { previousSummary ->
            requireContext().resources.getStringArray(R.array.entries_fuel_keys).forEachIndexed { index, type ->
                    if (type == previousSummary.fuelConsumptionInfo.type) {
                        viewDataBinding.fuelSpinner.setSelection(index)
                    }
                }

            if (previousSummary.distance > 0) {
                viewDataBinding.distanceInputLayout.editText?.setText(previousSummary.distance.toString())
            }

            if (previousSummary.fuelConsumptionInfo.value > 0) {
                viewDataBinding.fuelConsumptionInputLayout.editText?.setText(previousSummary.fuelConsumptionInfo.value.toString())
            }
        }

        viewModel.summarySubmitResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is FirebaseResult.Success<DocumentReference> -> {
                    viewDataBinding.deliveriesRecyclerView.adapter?.notifyDataSetChanged()
                }
                is FirebaseResult.Error -> {
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _viewDataBinding = null
    }


    override fun onStart() {
        super.onStart()

        viewModel.subscribeToDeliveries()
    }

    override fun onStop() {
        super.onStop()

        viewModel.unsubscribe()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (viewModel.isDeliveryItemChecked()) {
            inflater.inflate(R.menu.menu_with_settings_and_directions, menu)
        } else {
            inflater.inflate(R.menu.menu_with_settings, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                viewModel.navigate(NavInfo(R.id.settingsFragment, SettingsFragmentDirections.actionGlobalSettingsFragment()))
            }
            R.id.menu_directions -> {
                showDirections()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun submitSummary() {
        (viewDataBinding.deliveriesRecyclerView.adapter as? DeliveriesAdapter)?.items?.let { list ->
            viewModel.updateSites(list)
        }

        val distance = viewDataBinding.distanceInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F
        val fuelConsumption = viewDataBinding.fuelConsumptionInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F
        // Validate distance
        viewDataBinding.distanceInputLayout.validate(Const.MIN_DISTANCE, Const.MAX_DISTANCE)
        // Validate fuel consumption
        requireContext().resources.getStringArray(R.array.entries_fuel_keys)[viewDataBinding.fuelSpinner.selectedItemPosition]?.let { item ->
            viewDataBinding.fuelConsumptionInputLayout.run {
                when (item) {
                    "biogas" -> {
                        validate(Const.MIN_BIOGAS_CONSUMPTION, Const.MAX_BIOGAS_CONSUMPTION)
                    }
                    "gasoline" -> {
                        validate(Const.MIN_GASOLINE_CONSUMPTION, Const.MAX_GASOLINE_CONSUMPTION)
                    }
                    "diesel" -> {
                        validate(Const.MIN_DIESEL_CONSUMPTION, Const.MAX_DIESEL_CONSUMPTION)
                    }
                    "electric" -> {
                        validate(Const.MIN_ELECTRIC_CONSUMPTION, Const.MAX_ELECTRIC_CONSUMPTION)
                    }
                    "hydrogen" -> {
                        validate(Const.MIN_HYDROGEN_CONSUMPTION, Const.MAX_HYDROGEN_CONSUMPTION)
                    }
                }
            }
        }

        if (viewDataBinding.distanceInputLayout.isValid() && viewDataBinding.fuelConsumptionInputLayout.isValid()) {
            viewDataBinding.focusableView.launch(10) {
                viewDataBinding.focusableView.requestFocus()
                requireActivity().hideKeyboard()
            }

            requireContext().resources.getStringArray(R.array.entries_fuel_keys)[viewDataBinding.fuelSpinner.selectedItemPosition]?.let { item ->
                viewModel.submitSummary(distance, fuelConsumption, item)
            }
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        touchHelper.startDrag(viewHolder)
    }

    override fun onOrderChanged() {
        (viewDataBinding.deliveriesRecyclerView.adapter as? DeliveriesAdapter)?.items?.let { list ->
            viewModel.updateSites(list)
        }
    }

    private fun showDirections() {
        val intentUri = Uri.Builder().apply {
            scheme("https")
            authority("www.google.com")
            appendPath("maps")
            appendPath("dir")
            appendPath("")
            appendQueryParameter("api", "1")

            val sites = viewModel.sites.toMutableList()
            sites.lastOrNull()?.let { site ->
                appendQueryParameter("destination", "${site.location.latitude},${site.location.longitude}")
                sites.remove(site)
            }

            val wayPoints = StringBuilder()
            var first = true
            viewModel.sites.forEach { site ->
                if (first) {
                    wayPoints.append("${site.location.latitude},${site.location.longitude}")
                    first = false
                } else {
                    wayPoints.append("|${site.location.latitude},${site.location.longitude}")
                }
            }
            appendQueryParameter("waypoints", wayPoints.toString())
            appendQueryParameter("travelmode", "driving")
        }.build()

        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = intentUri
        })
    }
}