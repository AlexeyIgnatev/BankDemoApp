package com.esom.bank.screens.bio

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentBioBinding
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BioFragment : Fragment() {
    private lateinit var binding: FragmentBioBinding

    @Inject lateinit var localDataSource: PinLocalDataSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }
        binding.logInBtn.setOnClickListener {
            localDataSource.setBio(true)
            findNavController().navigate(NavGraphDirections.startLogInFragment())
        }
        binding.onlyPasswordTitle.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startSplashLogInFragment())
        }
    }
}