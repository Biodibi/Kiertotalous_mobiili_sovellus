package com.oamk.kiertotalous.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.FragmentSettingsBinding
import com.oamk.kiertotalous.extensions.hideKeyboard
import com.oamk.kiertotalous.extensions.launch
import org.koin.androidx.navigation.koinNavGraphViewModel

class SettingsFragment : Fragment() {
    private val viewModel: SettingsViewModel by koinNavGraphViewModel(R.id.nav_graph)

    private var _viewDataBinding: FragmentSettingsBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewDataBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = viewLifecycleOwner

        viewDataBinding.saveTareWeightsButton.setOnClickListener {
            saveTareWeights()
        }

        viewModel.loadSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _viewDataBinding = null
    }

    private fun saveTareWeights() {
        val trolleyTareWeight = viewDataBinding.trolleyInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F
        val palletTareWeight = viewDataBinding.palletInputLayout.editText?.text?.toString()?.toFloatOrNull() ?: 0F
        viewModel.saveTareWeights(palletTareWeight, trolleyTareWeight)

        viewDataBinding.focusableView.launch(10) {
            viewDataBinding.focusableView.requestFocus()
            requireActivity().hideKeyboard()
        }
    }
}