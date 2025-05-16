package com.esom.bank.screens.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentAuthBinding
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthFragment : Fragment() {
    private lateinit var binding: FragmentAuthBinding

    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
            )
            insets
        }

        binding.logInBtn.setOnClickListener {
            if (model.myData.value !is UiState.Loading) {
                model.authenticate(
                    binding.loginInput.editText!!.text.toString(),
                    binding.passwordInput.editText!!.text.toString(),
                )
            }
        }

        model.myData.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    binding.logInText.isVisible = false
                    binding.indicator.isVisible = true
                }

                is UiState.Error -> {
                    binding.root.showErrorSnackbar(it.message)
                    binding.logInText.isVisible = true
                    binding.indicator.isVisible = false
                }

                is UiState.Success -> {
                    binding.logInText.isVisible = true
                    binding.indicator.isVisible = false

                    findNavController().popBackStack()
                    findNavController().navigate(NavGraphDirections.startMainFragment())
                }
            }
        }

        if (model.isAuthenticated()) {
            findNavController().popBackStack()
            findNavController().navigate(NavGraphDirections.startMainFragment())
        }
    }
}