package com.esom.bank.screens.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.esom.bank.MainNavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bottomNavigationView.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.ic_wallet -> findMainNavController().navigate(MainNavGraphDirections.startWalletFragment())
                R.id.ic_activity -> findMainNavController().navigate(MainNavGraphDirections.startActionsFragment())
                R.id.ic_settings -> findMainNavController().navigate(MainNavGraphDirections.startSettingsFragment())
            }
            true
        }
    }

    private fun findMainNavController(): NavController {
        return Navigation.findNavController(requireView().findViewById(R.id.main_nav_host_fragment))
    }

    companion object {
        fun Fragment.findParentNavController() =
            (parentFragment!!.parentFragment as Fragment).findNavController()
    }
}