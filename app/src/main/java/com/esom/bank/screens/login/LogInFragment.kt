package com.esom.bank.screens.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.esom.bank.BuildConfig
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.views.patternlock.PatternLockView
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentLogInBinding
import com.esom.bank.screens.pinCreate.data.LockType
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LogInFragment : Fragment() {
    private lateinit var binding: FragmentLogInBinding
    private var pinCode = ""
    private val maxPinLength = 4
    private var currentMode: LockType = LockType.PIN
    private var biometricPromptShown = false

    @Inject
    lateinit var localDataSource: PinLocalDataSource

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogInBinding.inflate(inflater, container, false)
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
        binding.versionTitle.text = getString(R.string.version_title, BuildConfig.VERSION_NAME)

        binding.authBtn.setOnClickListener {
            findNavController().navigate(
                R.id.startPinCreateFragment,
                bundleOf("fromSettings" to true)
            )
        }

        binding.pincDig0Btn.setOnClickListener { onDigitClicked("0") }
        binding.pincDig1Btn.setOnClickListener { onDigitClicked("1") }
        binding.pincDig2Btn.setOnClickListener { onDigitClicked("2") }
        binding.pincDig3Btn.setOnClickListener { onDigitClicked("3") }
        binding.pincDig4Btn.setOnClickListener { onDigitClicked("4") }
        binding.pincDig5Btn.setOnClickListener { onDigitClicked("5") }
        binding.pincDig6Btn.setOnClickListener { onDigitClicked("6") }
        binding.pincDig7Btn.setOnClickListener { onDigitClicked("7") }
        binding.pincDig8Btn.setOnClickListener { onDigitClicked("8") }
        binding.pincDig9Btn.setOnClickListener { onDigitClicked("9") }

        binding.pincDigbackBtn.setOnClickListener { onBackspaceClicked() }

        setupPatternListener()
        initLockMode()
    }

    private fun onBackspaceClicked() {
        if (currentMode != LockType.PIN) return
        if (pinCode.isNotEmpty()) {
            pinCode = pinCode.substring(0, pinCode.length - 1)
            updatePinDots()
        }
    }

    private fun onDigitClicked(digit: String) {
        if (currentMode != LockType.PIN) return
        if (pinCode.length < maxPinLength) {
            pinCode += digit
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        val views = listOf(binding.one, binding.two, binding.three, binding.four)
        val pinChooseBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.pin_choose_background)
        val pinNotChooseBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.pin_not_choose_background)

        views.forEachIndexed { index, view ->
            view.background = if (index < pinCode.length) pinChooseBg else pinNotChooseBg
        }

        if (pinCode.length == maxPinLength) {
            if (localDataSource.verifyPin(pinCode)) {
                onUnlockSuccess()
            } else {
                showPinError(getString(R.string.lock_wrong_password))
            }
        }
    }

    private fun setupPatternListener() {
        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val input = ids.toList()
                if (input.size < 4 || !localDataSource.verifyPattern(input)) {
                    showPatternError(getString(R.string.lock_wrong_password))
                    return false
                }
                onUnlockSuccess()
                return true
            }
        })
    }

    private fun initLockMode() {
        if (!localDataSource.hasLock()) {
            findNavController().navigate(
                R.id.startPinCreateFragment,
                bundleOf("fromSettings" to false)
            )
            return
        }

        currentMode = localDataSource.getLockType() ?: LockType.PIN
        binding.pinSection.isVisible = currentMode == LockType.PIN
        binding.patternSection.isVisible = currentMode == LockType.PATTERN
        binding.lockTitle.text = if (currentMode == LockType.PIN) {
            getString(R.string.lock_enter_pin)
        } else {
            getString(R.string.lock_enter_pattern)
        }
        binding.errorCard.isVisible = false
        binding.patternErrorCard.isVisible = false
        pinCode = ""
        updatePinDots()
        showBiometricPromptIfEnabled()
    }

    private fun showBiometricPromptIfEnabled() {
        if (biometricPromptShown || !localDataSource.isBio()) return
        if (!isBiometricAvailable()) {
            localDataSource.setBio(false)
            return
        }

        biometricPromptShown = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onUnlockSuccess()
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.bio_log_in))
                .setSubtitle(getString(R.string.use_bio_for_log_in).replace("\n", " "))
                .setNegativeButtonText(getString(R.string.use_only_password))
                .setAllowedAuthenticators(biometricAuthenticators())
                .build()
        )
    }

    private fun isBiometricAvailable(): Boolean {
        return BiometricManager.from(requireContext()).canAuthenticate(biometricAuthenticators()) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun biometricAuthenticators(): Int {
        return BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
    }

    private fun showPinError(message: String) {
        binding.errorText.text = message
        binding.errorCard.isVisible = true
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        binding.pinLayout.startAnimation(shake)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(900L)
            binding.errorCard.isVisible = false
            pinCode = ""
            updatePinDots()
        }
    }

    private fun showPatternError(message: String) {
        binding.patternErrorText.text = message
        binding.patternErrorCard.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            delay(900L)
            binding.patternErrorCard.isVisible = false
        }
    }

    private fun onUnlockSuccess() {
        findNavController().navigate(NavGraphDirections.startMainFragment())
    }
}
