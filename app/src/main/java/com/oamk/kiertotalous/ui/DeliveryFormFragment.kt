package com.oamk.kiertotalous.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.DocumentReference
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.FragmentDeliveryFormBinding
import com.oamk.kiertotalous.extensions.*
import com.oamk.kiertotalous.model.*
import org.koin.android.ext.android.inject
import org.koin.androidx.navigation.koinNavGraphViewModel
import timber.log.Timber

class DeliveryFormFragment : Fragment() {
    private val viewModel: DeliveryFormViewModel by koinNavGraphViewModel(R.id.nav_graph)
    private val app: AppController by inject()

    private var _viewDataBinding: FragmentDeliveryFormBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    private lateinit var takePictureActivityResultLauncher: TakePictureActivityResultLauncher

    private val cameraPermissionResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
        if (permissionGranted) {
            requireActivity().createTempImageFile()?.let { fileInfo ->
                takePictureActivityResultLauncher.launch(fileInfo)
            }
        }
    }

    private val enableBluetoothResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            viewModel.startDeviceConnectionJob()
        }
    }

    private val bluetoothPermissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionsGranted ->
        if (permissionsGranted) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothResultLauncher.launch(intent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        _viewDataBinding = FragmentDeliveryFormBinding.inflate(inflater, container, false)

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = viewLifecycleOwner

        viewDataBinding.countInputLayout.editText?.setText("1")
        viewDataBinding.countInputLayout.addTextChangedListener()
        viewDataBinding.weightInputLayout.addTextChangedListener()

        viewDataBinding.parcelsRecyclerView.apply {
            ParcelsAdapter(viewModel.parcelItems).let { parcelsAdapter ->
                viewModel.parcelItems.subscribe(parcelsAdapter)
                adapter = parcelsAdapter
            }
        }

        viewDataBinding.attachmentsRecyclerView.apply {
            ImagesAdapter(viewModel.imageItems, false).let { imagesAdapter ->
                viewModel.imageItems.subscribe(imagesAdapter)
                adapter = imagesAdapter
            }
        }

        viewDataBinding.takePictureButton.setOnClickListener {
            takePicture()
        }

        viewDataBinding.submitFormButton.setOnClickListener {
            submitDeliveryForm()
        }

        viewDataBinding.addParcelButton.setOnClickListener {
            val weight = viewDataBinding.weightInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F
            val count = viewDataBinding.countInputLayout.editText?.text?.toString()?.toIntOrNull() ?: 0
            viewDataBinding.weightInputLayout.validate(Const.MIN_WEIGHT, Const.MAX_WEIGHT)
            viewDataBinding.countInputLayout.validate(Const.MIN_PARCELS_COUNT, Const.MAX_PARCELS_COUNT)

            if (viewDataBinding.countInputLayout.isValid() && viewDataBinding.weightInputLayout.isValid()) {
                viewDataBinding.weightInputLayout.editText?.text = null
                viewDataBinding.countInputLayout.editText?.setText("1")

                requireContext().resources.getStringArray(R.array.entries_parcel_keys)[viewDataBinding.typeSpinner.selectedItemPosition].let { type ->
                    val onDeleteClickListener = object : ParcelItemData.Listener {
                        override fun onDeleteClicked(item: ParcelItemData) {
                            viewModel.parcelItems.remove(item)
                        }
                    }

                    val parcel = Parcel(weight, count, type)
                    val parcelItem = ParcelItemData(parcel, viewDataBinding.typeSpinner.selectedItem.toString(), onDeleteClickListener)
                    viewModel.parcelItems.add(parcelItem)
                }
            }
        }

        viewModel.formSubmitResultLiveData.observe(viewLifecycleOwner) { result ->
            when (result) {
                is FirebaseResult.Success<DocumentReference> -> {
                    viewDataBinding.weightInputLayout.editText?.text = null
                    viewDataBinding.countInputLayout.editText?.setText("1")
                    viewDataBinding.descriptionInputLayout.editText?.text = null
                }
                is FirebaseResult.Error -> {
                    // Handled in view model
                }
            }
        }

        viewModel.deviceEventLiveData.observe(viewLifecycleOwner) { event ->
            val animatedImage = viewDataBinding.deviceStatusImageView
            when (event) {
                is DeviceEvent.Connecting -> {
                    animatedImage.setBackgroundResource(R.drawable.anim_bluetooth_connecting)
                    (animatedImage.background as AnimationDrawable).start()
                }
                is DeviceEvent.WeightResult,
                is DeviceEvent.Connected -> {
                    animatedImage.setBackgroundResource(R.drawable.ic_bluetooth_connected)
                }
                is DeviceEvent.Error -> {
                    when (event.errorEvent.errorEventType) {
                        ErrorEventType.BLUETOOTH_DISABLED,
                        ErrorEventType.DEVICE_NOT_PAIRED -> {
                            animatedImage.setBackgroundResource(R.drawable.ic_bluetooth_disconnected)
                            var errorMessage = R.string.error_bluetooth_disabled
                            if (event.errorEvent.errorEventType == ErrorEventType.DEVICE_NOT_PAIRED) {
                                errorMessage = R.string.error_bluetooth_pairing_missing
                            }
                            app.showAppNotification(AppNotification(message = errorMessage, isError = true))
                        }
                        else -> {
                            // TODO: Clear result?
                            if (event.errorEvent.errorEventType == ErrorEventType.DEVICE_DISCONNECTED) {
                                viewModel.weightResult.set("")
                            }
                            animatedImage.setBackgroundResource(R.drawable.anim_bluetooth_connecting)
                            (animatedImage.background as AnimationDrawable).start()
                            // Retry
                            viewModel.startDeviceConnectionJob(5000)
                        }
                    }
                }
            }
        }

        takePictureActivityResultLauncher = TakePictureActivityResultLauncher(requireActivity().activityResultRegistry)
        observeTakePictureActivityResult()
        lifecycle.addObserver(takePictureActivityResultLauncher)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _viewDataBinding = null
    }

    override fun onStart() {
        super.onStart()

        // BLUETOOTH_CONNECT permission must be requested for Android 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionResultLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothResultLauncher.launch(intent)
        }
        // Inform user if Bluetooth is turned off
        if (!viewModel.isBluetoothEnabled()) {
            viewDataBinding.deviceStatusImageView.setBackgroundResource(R.drawable.ic_bluetooth_disconnected)
            app.showAppNotification(AppNotification(message = R.string.error_bluetooth_disabled, isError = true))
        }
    }

    override fun onStop() {
        super.onStop()
        // Close device connection if app is inactive after 60s
        viewModel.scheduleCloseDeviceConnectionJob(60000)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_with_settings, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                viewModel.navigate(NavInfo(R.id.settingsFragment, SettingsFragmentDirections.actionGlobalSettingsFragment()))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun takePicture() {
        viewDataBinding.focusableView.launch(10) {
            viewDataBinding.focusableView.requestFocus()
            requireActivity().hideKeyboard()
        }
        cameraPermissionResult.launch(Manifest.permission.CAMERA)
    }

    private fun observeTakePictureActivityResult() {
        takePictureActivityResultLauncher.activityResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CustomActivityResult.OK -> {
                    val onDeleteClickListener = object : ImageItemData.Listener {
                        override fun onDeleteClicked(item: ImageItemData) {
                            viewModel.imageItems.remove(item)
                        }
                    }

                    val imageItem = ImageItemData(result.result, onDeleteClickListener)
                    viewModel.compressAndAddImage(imageItem)
                    viewDataBinding.attachmentsRecyclerView.launch(2000) {
                        viewDataBinding.attachmentsRecyclerView.smoothScrollToPosition(0)
                    }
                }
                is CustomActivityResult.Cancelled -> {
                    Timber.d("$result")
                }
                is CustomActivityResult.Error -> {
                    Timber.e("$result")
                }
            }
        }
    }

    private fun submitDeliveryForm() {
        val description = viewDataBinding.descriptionInputLayout.editText?.text?.toString() ?: ""

        if (viewModel.parcelItems.isEmpty()) {
            val count = viewDataBinding.countInputLayout.editText?.text?.toString()?.toIntOrNull() ?: 0
            var weight = viewDataBinding.weightInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F

            viewDataBinding.countInputLayout.validate(Const.MIN_PARCELS_COUNT, Const.MAX_PARCELS_COUNT)
            viewDataBinding.weightInputLayout.validate(Const.MIN_WEIGHT, Const.MAX_WEIGHT)

            if (viewDataBinding.countInputLayout.isValid() && viewDataBinding.weightInputLayout.isValid()) {
                requireContext().resources.getStringArray(R.array.entries_parcel_keys)[viewDataBinding.typeSpinner.selectedItemPosition]?.let { type ->
                    // Check potential duplicate
                    if (viewModel.isSameWeightAsPreviouslySubmittedWeight(weight, type)) {
                        showConfirmationDialog()
                    } else {
                        val parcel = Parcel(weight, count, type)
                        val parcelItem = ParcelItemData(parcel, viewDataBinding.typeSpinner.selectedItem.toString())
                        viewModel.parcelItems.add(parcelItem)
                        viewModel.submitDeliveryForm(description)
                    }
                }
            }
        } else {
            viewModel.submitDeliveryForm(description)
        }

        viewDataBinding.focusableView.launch(10) {
            viewDataBinding.focusableView.requestFocus()
            requireActivity().hideKeyboard()
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(getString(R.string.error_check_weight_potential_duplicate))
            setPositiveButton(R.string.yes) { _, _ ->
                viewModel.previouslySubmittedWeight = 0F
                viewModel.previouslySubmittedWeightUnixTimestamp = 0L
                submitDeliveryForm()
            }
            setNegativeButton(R.string.no) { _, _ -> }
        }.run {
            create().show()
        }
    }
}