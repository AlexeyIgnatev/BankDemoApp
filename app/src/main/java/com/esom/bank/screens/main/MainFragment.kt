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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding: FragmentMainBinding
        get() = _binding ?: error("Binding accessed outside of the view lifecycle")
    private val uiModel: MainContainerUiStateViewModel by viewModels()
    private var destinationListener: NavController.OnDestinationChangedListener? = null
    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
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
                R.id.nav_qr -> R.id.mainQrFragment
                R.id.nav_services -> R.id.servicesFragment
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

        val navController = findMainNavController()
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            uiModel.setDestination(destination.id)
            renderBottomNavigation()
            val menuItem = when (destination.id) {
                R.id.walletFragment -> R.id.nav_home
                R.id.actionsFragment -> R.id.nav_payments
                R.id.mainQrFragment -> R.id.nav_qr
                R.id.servicesFragment -> R.id.nav_services
                else -> null
            }
            menuItem?.let { binding.bottomNavigationView.menu.findItem(it).isChecked = true }
        }
        destinationListener = listener
        navController.addOnDestinationChangedListener(listener)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val controller = findMainNavController()
            if (!controller.popBackStack()) requireActivity().finish()
        }
    }

    private fun findMainNavController(): NavController {
        val nestedHost = childFragmentManager.findFragmentById(R.id.main_nav_host_fragment)
            as? NavHostFragment
        return nestedHost?.navController
            ?: runCatching {
                requireView().findViewById<View>(R.id.main_nav_host_fragment).findNavController()
            }.getOrElse { findNavController() }
    }

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
            generateSequence(parentFragment) { it.parentFragment }
                .filterNot { it is NavHostFragment }
                .mapNotNull { fragment ->
                    runCatching { fragment.findNavController() }.getOrNull()
                }
                .firstOrNull()
                ?: findNavController()
    }

    override fun onDestroyView() {
        destinationListener?.let { listener ->
            runCatching { findMainNavController().removeOnDestinationChangedListener(listener) }
        }
        destinationListener = null
        _binding = null
        super.onDestroyView()
    }
}
