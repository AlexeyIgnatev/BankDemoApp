package com.esom.bank.screens.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
    private lateinit var binding: FragmentMainBinding
    private val uiModel: MainContainerUiStateViewModel by viewModels()
    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestContactsPermissionIfNeeded()
        binding.bottomNavigationView.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(bottom = rect.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            insets
        }
        binding.root.doOnApplyWindowInsets { _, insets, _ ->
            uiModel.setKeyboardVisible(insets.isVisible(WindowInsetsCompat.Type.ime()))
            renderBottomNavigation()
            insets
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val controller = findMainNavController()
            val destination = when (item.itemId) {
                R.id.nav_home -> R.id.walletFragment
                R.id.nav_payments -> R.id.actionsFragment
                R.id.nav_support -> R.id.chatFragment
                R.id.nav_security -> R.id.securityFragment
                else -> return@setOnItemSelectedListener false
            }
            if (controller.currentDestination?.id != destination) {
                controller.navigate(destination, null, navOptions {
                    launchSingleTop = true
                    popUpTo(controller.graph.startDestinationId) {
                        inclusive = false
                    }
                })
            }
            true
        }

        findMainNavController().addOnDestinationChangedListener { _, destination, _ ->
            uiModel.setDestination(destination.id)
            renderBottomNavigation()
            val menuItem = when (destination.id) {
                R.id.walletFragment -> R.id.nav_home
                R.id.actionsFragment -> R.id.nav_payments
                R.id.chatFragment -> R.id.nav_support
                R.id.securityFragment -> R.id.nav_security
                else -> null
            }
            menuItem?.let { binding.bottomNavigationView.menu.findItem(it).isChecked = true }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val controller = findMainNavController()
            if (!controller.popBackStack()) requireActivity().finish()
        }
    }

    private fun findMainNavController(): NavController =
        requireView().findViewById<View>(R.id.main_nav_host_fragment).findNavController()

    private fun renderBottomNavigation() {
        val state = uiModel.uiState.value
        binding.bottomNavigationView.visibility = if (
            state.keyboardVisible || state.destinationId == R.id.appSettingsFragment ||
                state.destinationId == R.id.historyFragment ||
                state.destinationId == R.id.financialAnalysisFragment
        ) View.GONE else View.VISIBLE
    }

    private fun requestContactsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    companion object {
        fun Fragment.findParentNavController() =
            (parentFragment!!.parentFragment as Fragment).findNavController()
    }
}
