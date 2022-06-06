package com.oamk.kiertotalous.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.oamk.kiertotalous.BuildConfig
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.databinding.FragmentLoginBinding
import com.oamk.kiertotalous.extensions.hideKeyboard
import com.oamk.kiertotalous.extensions.launch
import org.koin.androidx.navigation.koinNavGraphViewModel

class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by koinNavGraphViewModel(R.id.nav_graph)

    private var _viewDataBinding: FragmentLoginBinding? = null
    private val viewDataBinding get() = _viewDataBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewDataBinding = FragmentLoginBinding.inflate(inflater, container, false)
        return viewDataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewDataBinding.viewModel = viewModel
        viewDataBinding.lifecycleOwner = viewLifecycleOwner

        viewDataBinding.loginButton.setOnClickListener {
            onLoginButtonClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _viewDataBinding = null
    }

    private fun onLoginButtonClick() {
        var isValid = true
        val email = viewDataBinding.emailInputLayout.editText?.text?.toString() ?: ""
        val password = viewDataBinding.passwordInputLayout.editText?.text?.toString() ?: ""

        if (email.isNullOrEmpty()) {
            viewDataBinding.emailInputLayout.error = getString(R.string.error_check_input_text)
            isValid = false
        }

        if (password.isNullOrEmpty()) {
            viewDataBinding.passwordInputLayout.error = getString(R.string.error_check_input_text)
            isValid = false
        }

        if (isValid) {
            viewDataBinding.focusableView.launch(10) {
                viewDataBinding.focusableView.requestFocus()
                requireActivity().hideKeyboard()
            }
            viewModel.loginAndSubscribeToPushNotifications(email, password)
        }
    }
}