package com.esom.bank.screens.pinCreate

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.views.patternlock.PatternLockView
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentPinCreateBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.pinCreate.data.LockType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PinCreateFragment : Fragment() {
    private lateinit var binding: FragmentPinCreateBinding
    private val args: PinCreateFragmentArgs by navArgs()
    private val model: MainViewModel by activityViewModels()
    private val uiModel: PinCreateUiStateViewModel by viewModels()
    private val maxPinLength = 4

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinCreateBinding.inflate(inflater, container, false)
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

        binding.pinModeBtn.setOnClickListener { initMode(LockType.PIN) }
        binding.patternModeBtn.setOnClickListener { initMode(LockType.PATTERN) }
        binding.resetText.setOnClickListener {
            resetCurrentModeState()
        }

        binding.backBtn.isVisible = args.fromSettings
        if (args.fromSettings) {
            binding.backBtn.setOnClickListener { navigateBack() }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                navigateBack()
            }
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
        val initialMode = if (args.fromSettings) {
            model.getLockType() ?: LockType.PIN
        } else {
            LockType.PIN
        }
        initMode(initialMode)
    }

    private fun onBackspaceClicked() {
        if (uiModel.uiState.value.mode != LockType.PIN) return
        uiModel.removeLastDigit()
        updatePinDots()
    }

    private fun onDigitClicked(digit: String) {
        uiModel.appendDigit(digit, maxPinLength)
        updatePinDots()

        if (uiModel.uiState.value.currentPin.length == maxPinLength) {
            handlePinComplete()
        }
    }

    private fun updatePinDots() {
        val views = listOf(binding.one, binding.two, binding.three, binding.four)
        val pinChooseBg = ContextCompat.getDrawable(requireContext(), R.drawable.lock_pin_slot_filled)
        val pinNotChooseBg = ContextCompat.getDrawable(requireContext(), R.drawable.lock_pin_slot_empty)

        views.forEachIndexed { index, view ->
            view.background = if (index < uiModel.uiState.value.currentPin.length) pinChooseBg else pinNotChooseBg
        }
    }

    private fun handlePinComplete() {
        val state = uiModel.uiState.value
        if (state.firstPin == null) {
            uiModel.startPinConfirmation()
            updatePinDots()
            updateTexts()
            return
        }

        if (state.firstPin == state.currentPin) {
            model.savePin(state.currentPin)
            onLockSaved()
        } else {
            showPinError(getString(R.string.lock_pin_not_match))
        }
    }

    private fun setupPatternListener() {
        binding.patternLockView.setOnPatternListener(object : PatternLockView.OnPatternListener {
            override fun onComplete(ids: ArrayList<Int>): Boolean {
                val pattern = ids.toList()
                if (pattern.size < 4) {
                    showPatternError(getString(R.string.lock_pattern_min_points))
                    return false
                }

                val firstPattern = uiModel.uiState.value.firstPattern
                if (firstPattern == null) {
                    uiModel.startPatternConfirmation(pattern)
                    updateTexts()
                    return true
                }

                return if (firstPattern == pattern) {
                    model.savePattern(pattern)
                    onLockSaved()
                    true
                } else {
                    showPatternError(getString(R.string.lock_pattern_not_match))
                    false
                }
            }
        })
    }

    private fun initMode(mode: LockType) {
        uiModel.selectMode(mode)

        binding.pinSection.isVisible = mode == LockType.PIN
        binding.patternSection.isVisible = mode == LockType.PATTERN
        binding.pinModeBtn.isSelected = mode == LockType.PIN
        binding.patternModeBtn.isSelected = mode == LockType.PATTERN
        binding.errorCard.isVisible = false
        binding.patternErrorCard.isVisible = false
        binding.patternLockView.unFreeze()
        updatePinDots()
        updateTexts()
    }

    private fun resetCurrentModeState() {
        val mode = uiModel.uiState.value.mode
        uiModel.resetCurrentMode()
        if (mode == LockType.PIN) {
            binding.errorCard.isVisible = false
            updatePinDots()
        } else {
            binding.patternErrorCard.isVisible = false
            binding.patternLockView.unFreeze()
        }
        updateTexts()
    }

    private fun updateTexts() {
        val state = uiModel.uiState.value
        val isPin = state.mode == LockType.PIN
        val isConfirm = if (isPin) state.firstPin != null else state.firstPattern != null

        binding.lockTitle.text = when {
            isPin && args.fromSettings -> getString(R.string.lock_change_pin_title)
            isPin -> getString(R.string.lock_create_pin_title)
            !isPin && args.fromSettings -> getString(R.string.lock_change_pattern_title)
            else -> getString(R.string.lock_create_pattern_title)
        }

        binding.lockSubtitle.text = when {
            isPin && !isConfirm -> getString(R.string.lock_enter_pin)
            isPin -> getString(R.string.lock_confirm_pin)
            !isPin && !isConfirm -> getString(R.string.lock_enter_pattern)
            else -> getString(R.string.lock_confirm_pattern)
        }

        binding.resetText.isVisible = isConfirm
    }

    private fun showPinError(message: String) {
        binding.errorText.text = message
        binding.errorCard.isVisible = true
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        binding.pinLayout.startAnimation(shake)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(900L)
            binding.errorCard.isVisible = false
            uiModel.clearCurrentPin()
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

    private fun onLockSaved() {
        if (args.fromSettings) {
            navigateBack()
            return
        }

        if (model.isAuthenticated()) {
            findNavController().navigate(NavGraphDirections.startMainFragment())
        } else {
            findNavController().navigate(NavGraphDirections.startAuthFragment())
        }
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }
}
