package com.esom.bank.screens.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.esom.bank.R
import com.esom.bank.activities.MainActivity
import com.esom.bank.common.views.patternlock.PatternLockView
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentLogInBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.pinCreate.data.LockType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogInFragment : Fragment() {
    private lateinit var binding: FragmentLogInBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: LogInUiStateViewModel by viewModels()
    private val maxPinLength = 4

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
        binding.pinModeBtn.setOnClickListener { selectMode(LockType.PIN) }
        binding.patternModeBtn.setOnClickListener { selectMode(LockType.PATTERN) }
        binding.biometricBtn.setOnClickListener {
            uiModel.resetBiometricPrompt()
            showBiometricPrompt(requireEnabled = false)
        }

        setupPatternListener()
        initLockMode()
    }

    private fun onBackspaceClicked() {
        uiModel.removeLastDigit()
        updatePinDots()
    }

    private fun onDigitClicked(digit: String) {
        uiModel.appendDigit(digit, maxPinLength)
        updatePinDots()
    }

    private fun updatePinDots() {
        val views = listOf(binding.one, binding.two, binding.three, binding.four)
        val pinChooseBg = ContextCompat.getDrawable(requireContext(), R.drawable.lock_pin_slot_filled)
        val pinNotChooseBg = ContextCompat.getDrawable(requireContext(), R.drawable.lock_pin_slot_empty)

        views.forEachIndexed { index, view ->
            view.background = if (index < uiModel.uiState.value.pinCode.length) pinChooseBg else pinNotChooseBg
        }

        val pinCode = uiModel.uiState.value.pinCode
        if (pinCode.length == maxPinLength) {
            if (model.verifyPin(pinCode)) {
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
                if (input.size < 4 || !model.verifyPattern(input)) {
                    showPatternError(getString(R.string.lock_wrong_password))
                    return false
                }
                onUnlockSuccess()
                return true
            }
        })
    }

    private fun initLockMode() {
        if (!model.hasLock()) {
            findNavController().navigate(
                R.id.startPinCreateFragment,
                bundleOf("fromSettings" to false)
            )
            return
        }

        binding.pinModeBtn.isEnabled = model.hasPin()
        binding.patternModeBtn.isEnabled = model.hasPattern()
        binding.pinModeBtn.alpha = if (binding.pinModeBtn.isEnabled) 1f else 0.45f
        binding.patternModeBtn.alpha = if (binding.patternModeBtn.isEnabled) 1f else 0.45f
        val initialMode = model.getLockType()?.takeIf(::isModeAvailable)
            ?: if (model.hasPin()) LockType.PIN else LockType.PATTERN
        selectMode(initialMode)
        binding.biometricBtn.visibility = if (isBiometricAvailable()) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        showBiometricPrompt(requireEnabled = true)
    }

    private fun selectMode(mode: LockType) {
        if (!isModeAvailable(mode)) return
        uiModel.selectMode(mode)
        binding.pinSection.isVisible = mode == LockType.PIN
        binding.patternSection.isVisible = mode == LockType.PATTERN
        binding.pinModeBtn.isSelected = mode == LockType.PIN
        binding.patternModeBtn.isSelected = mode == LockType.PATTERN
        binding.lockTitle.text = if (mode == LockType.PIN) {
            getString(R.string.lock_enter_pin)
        } else {
            getString(R.string.lock_enter_pattern)
        }
        binding.errorCard.isVisible = false
        binding.patternErrorCard.isVisible = false
        updatePinDots()
    }

    private fun isModeAvailable(mode: LockType): Boolean = when (mode) {
        LockType.PIN -> model.hasPin()
        LockType.PATTERN -> model.hasPattern()
    }

    private fun showBiometricPrompt(requireEnabled: Boolean) {
        if (uiModel.uiState.value.biometricPromptShown || requireEnabled && !model.isBiometricEnabled()) return
        if (!isBiometricAvailable()) {
            return
        }

        uiModel.markBiometricPromptShown()
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
            uiModel.clearPin()
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
        (activity as? MainActivity)?.markUserAuthenticated()
        findNavController().navigate(
            R.id.mainFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
        )
    }
}
