package com.esom.bank.screens.settigns

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.activities.MainActivity
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.common.utils.views.showSuccessSnackbar
import com.esom.bank.databinding.FragmentSettingsBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot
import javax.inject.Inject


@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding

    private val model: MainViewModel by activityViewModels()

    private var ignorePushSwitchChanges = false
    private var ignoreBioSwitchChanges = false

    @Inject
    lateinit var pinLocalDataSource: PinLocalDataSource

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        model.setPushNotificationsEnabled(granted)
        setPushSwitchChecked(granted)
        if (granted) refreshFcmToken()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        setupPushSwitch()
        setupBioSwitch()

        binding.financeBtn.setOnClickListener {
            model.sendFinancialReport()
        }

        binding.changePassBtn.setOnClickListener {
            findParentNavController().navigate(
                R.id.startPinCreateFragment,
                bundleOf("fromSettings" to true)
            )
        }

        binding.logInBtn.setOnClickListener {
            model.clearAllDataAndNavigate()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        model.myData.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}
                is UiState.Error -> {
                    binding.root.showErrorSnackbar(it.message)
                }

                is UiState.Success -> {
                    binding.fio.text = "${it.data.firstName} ${it.data.middleName} ${it.data.lastName}"
                    binding.phone.text = "${it.data.phone.formatPhone()}"
                    binding.mail.text = "${it.data.email}"
                }
            }
        }

        model.financialReport.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> Unit
                is UiState.Error -> binding.root.showErrorSnackbar(it.message)
                is UiState.Success -> binding.root.showSuccessSnackbar("Выгрузка прошла успешно")
            }
        }

        binding.helpBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChatFragment())
        }

        binding.callBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$PERSONAL_MANAGER_PHONE")
            }
            startActivity(intent)
        }
    }

    private fun setupPushSwitch() {
        binding.pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ignorePushSwitchChanges) return@setOnCheckedChangeListener
            handlePushSwitchChanged(isChecked)
        }

        model.pushNotificationsEnabled.observe(viewLifecycleOwner) { enabled ->
            setPushSwitchChecked(enabled && hasNotificationPermission())
        }
        model.loadPushNotificationsEnabled()
    }

    private fun setupBioSwitch() {
        val bioEnabled = pinLocalDataSource.isBio() && isBiometricAvailable()
        if (pinLocalDataSource.isBio() && !bioEnabled) {
            pinLocalDataSource.setBio(false)
        }
        setBioSwitchChecked(bioEnabled)

        binding.bioSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreBioSwitchChanges) return@setOnCheckedChangeListener
            handleBioSwitchChanged(isChecked)
        }
    }

    private fun handleBioSwitchChanged(enabled: Boolean) {
        if (!enabled) {
            pinLocalDataSource.setBio(false)
            return
        }

        if (!isBiometricAvailable()) {
            pinLocalDataSource.setBio(false)
            setBioSwitchChecked(false)
            binding.root.showErrorSnackbar(getString(R.string.biometric_unavailable))
            return
        }

        pinLocalDataSource.setBio(true)
    }

    private fun handlePushSwitchChanged(enabled: Boolean) {
        if (!enabled) {
            model.setPushNotificationsEnabled(false)
            return
        }

        if (!hasNotificationPermission()) {
            requestNotificationPermission()
            return
        }

        model.setPushNotificationsEnabled(true)
        refreshFcmToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setPushSwitchChecked(checked: Boolean) {
        ignorePushSwitchChanges = true
        binding.pushSwitch.isChecked = checked
        ignorePushSwitchChanges = false
    }

    private fun setBioSwitchChecked(checked: Boolean) {
        ignoreBioSwitchChanges = true
        binding.bioSwitch.isChecked = checked
        ignoreBioSwitchChanges = false
    }

    private fun isBiometricAvailable(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return BiometricManager.from(requireContext()).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun refreshFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                model.sendFcmToken(token)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Unable to get FCM token", error)
            }
    }

    companion object {
        private const val TAG = "SettingsFragment"
        private const val PERSONAL_MANAGER_PHONE = "+996555123456"

        fun String.formatPhone(): String {
            val mask: Mask = MaskImpl(PHONE_NUMBER, true)
            mask.insertFront(replace(" ", "").replace("+996", ""))
            return mask.toString()
        }

        val PHONE_NUMBER: Array<Slot> = arrayOf(
            PredefinedSlots.hardcodedSlot('+'),
            PredefinedSlots.hardcodedSlot('9'),
            PredefinedSlots.hardcodedSlot('9'),
            PredefinedSlots.hardcodedSlot('6'),
            PredefinedSlots.hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            PredefinedSlots.hardcodedSlot('(').withTags(Slot.TAG_DECORATION),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
            PredefinedSlots.hardcodedSlot(')').withTags(Slot.TAG_DECORATION),
            PredefinedSlots.hardcodedSlot(' ').withTags(Slot.TAG_DECORATION),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
            PredefinedSlots.hardcodedSlot('-').withTags(Slot.TAG_DECORATION),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
            PredefinedSlots.digit(),
        )
    }

}
