package com.esom.bank.screens.sms

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showKeyboard
import com.esom.bank.databinding.FragmentSmsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmsFragment : Fragment() {
    private lateinit var binding: FragmentSmsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            val imeBar = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if(imeBar == 0) insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom else imeBar
            )
            insets
        }
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        setupSmsInputs()
        setupAutoSubmit()

        binding.root.post {
            binding.first.requestFocus()
            binding.first.showKeyboard()
        }
    }

    private var shouldHandleAutoSubmit = true
    override fun onResume() {
        super.onResume()
        shouldHandleAutoSubmit = true

        if (findNavController().currentDestination?.id == R.id.smsFragment) {
            with(binding) {
                listOf(first, second, third, fourth, fifth).forEach {
                    it.text?.clear()
                }
                first.requestFocus()
            }
        }
    }

    private fun setupSmsInputs() {
        with(binding) {
            val inputFields = listOf(
                first to oneLine,
                second to twoLine,
                third to threeLine,
                fourth to fourLine,
                fifth to fiveLine
            )

            inputFields.forEachIndexed { index, (editText, underline) ->
                editText.addTextChangedListener { text ->
                    underline.setBackgroundResource(
                        if (editText.text.isNullOrEmpty()) R.drawable.sms_line_gray_background
                        else R.drawable.sms_line_background
                    )

                    if (!text.isNullOrEmpty() && index < inputFields.lastIndex) {
                        inputFields[index + 1].first.requestFocus()
                    }
                }

                editText.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        editText.text.isNullOrEmpty() &&
                        index > 0) {
                        inputFields[index - 1].first.requestFocus()
                        inputFields[index - 1].first.text?.clear()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
    private fun setupAutoSubmit() {
        val allFields = listOf(binding.first, binding.second, binding.third, binding.fourth, binding.fifth)

        allFields.forEach { field ->
            field.addTextChangedListener {
                if (allFields.all { !it.text.isNullOrEmpty() }) {
                    findNavController().navigate(NavGraphDirections.startPinCreateFragment())
                }
            }
        }
    }

    private fun getFullCode(): String {
        return with(binding) {
            "${first.text}${second.text}${third.text}${fourth.text}${fifth.text}"
        }
    }
}