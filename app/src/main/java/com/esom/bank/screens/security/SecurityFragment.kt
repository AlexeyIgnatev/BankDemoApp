package com.esom.bank.screens.security

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.findNavController as findActivityNavController
import androidx.navigation.navOptions
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentSecurityBinding
import com.esom.bank.screens.main.MainViewModel
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SecurityFragment : Fragment() {
    private lateinit var binding: FragmentSecurityBinding
    private val model: MainViewModel by activityViewModels()

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        model.setPushNotificationsEnabled(granted)
        setPushChecked(granted)
        if (granted) refreshFcmToken()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scrollView.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        binding.changePinBtn.setOnClickListener {
            requireActivity().findActivityNavController(R.id.nav_host_fragment).navigate(
                R.id.startPinCreateFragment,
                bundleOf("fromSettings" to true)
            )
        }
        binding.backBtn.setOnClickListener { returnToMain() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            returnToMain()
        }
        setupBiometrics()
        setupNotifications()
        binding.bioCard.setOnClickListener { binding.bioSwitch.toggle() }
        binding.pushCard.setOnClickListener { binding.pushSwitch.toggle() }
    }

    private fun setupBiometrics() {
        val enabled = model.isBiometricEnabled() && isBiometricAvailable()
        setBioChecked(enabled)
    }

    private fun setupNotifications() {
        binding.pushSwitch.setOnCheckedChangeListener { _, checked ->
            handlePushChanged(checked)
        }
        model.pushNotificationsEnabled.observe(viewLifecycleOwner) {
            setPushChecked(it && hasNotificationPermission())
        }
        model.loadPushNotificationsEnabled()
    }

    private fun setPushChecked(checked: Boolean) {
        binding.pushSwitch.setOnCheckedChangeListener(null)
        binding.pushSwitch.isChecked = checked
        binding.pushSwitch.setOnCheckedChangeListener { _, enabled ->
            handlePushChanged(enabled)
        }
    }

    private fun setBioChecked(checked: Boolean) {
        binding.bioSwitch.setOnCheckedChangeListener(null)
        binding.bioSwitch.isChecked = checked
        binding.bioSwitch.setOnCheckedChangeListener { _, enabled ->
            handleBioChanged(enabled)
        }
    }

    private fun handleBioChanged(enabled: Boolean) {
        if (enabled && !isBiometricAvailable()) {
            model.setBiometricEnabled(false)
            setBioChecked(false)
            binding.root.showErrorSnackbar(getString(R.string.biometric_unavailable))
        } else {
            model.setBiometricEnabled(enabled)
        }
    }

    private fun handlePushChanged(enabled: Boolean) {
        if (enabled && !hasNotificationPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            model.setPushNotificationsEnabled(enabled)
            if (enabled) refreshFcmToken()
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val types = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return BiometricManager.from(requireContext()).canAuthenticate(types) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(model::sendFcmToken)
    }

    private fun returnToMain() {
        findNavController().navigate(R.id.mainFragment, null, navOptions {
            launchSingleTop = true
            popUpTo(R.id.mainFragment) { inclusive = true }
        })
    }
}
