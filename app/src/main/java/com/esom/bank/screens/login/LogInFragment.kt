package com.esom.bank.screens.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentLogInBinding
import com.esom.bank.screens.pinCreate.data.PinLocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LogInFragment : Fragment() {
    private lateinit var binding: FragmentLogInBinding
    private var pinCode = ""
    private val maxPinLength = 4

    @Inject lateinit var localDataSource: PinLocalDataSource

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
        if(localDataSource.isBio()) binding.bioBtn.visibility = View.VISIBLE
        binding.authBtn.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startAuthFragment())
        }

        binding.bioBtn.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startSplashLogInFragment())
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
    }

    private fun onBackspaceClicked() {
        if (pinCode.isNotEmpty()) {
            pinCode = pinCode.substring(0, pinCode.length - 1)
            updatePinDots()
        }
    }

    private fun onDigitClicked(digit: String) {
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
            if(localDataSource.isBio())  {
                findNavController().navigate(NavGraphDirections.startSplashLogInFragment())
            } else {
                findNavController().navigate(NavGraphDirections.startBioFragment())
            }
        }
    }
}