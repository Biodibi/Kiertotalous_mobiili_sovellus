package com.oamk.kiertotalous.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.FragmentSummaryBinding
import com.oamk.kiertotalous.model.Delivery
import org.koin.androidx.navigation.koinNavGraphViewModel

class SummaryFragment : Fragment() {
    private val viewModel: SummaryViewModel by koinNavGraphViewModel(R.id.nav_graph)

    private var _viewDataBinding: FragmentSummaryBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    private var delivery: Delivery? = null

    override fun onDestroy() {
        super.onDestroy()

        viewModel.imageItems.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewDataBinding = FragmentSummaryBinding.inflate(inflater, container, false)
        delivery = arguments?.getParcelable("delivery") as? Delivery

        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = viewLifecycleOwner
        viewDataBinding.attachmentsRecyclerView.apply {
            adapter = ImagesAdapter(viewModel.imageItems, true)
        }

        delivery?.let { delivery ->
            viewModel.update(delivery)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _viewDataBinding = null
    }
}