package com.esom.bank.screens.registration

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationFragment : Fragment() {
    private lateinit var binding: FragmentRegistrationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false)
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

        binding.phoneInput.addTextChangedListener {
            if (it != null) {
                if(it.isEmpty()) {
                    binding.nextBtn.isClickable = false
                    binding.nextBtn.setCardBackgroundColor(Color.parseColor("#B2B2B2"))
                    binding.nextBtnShadow.visibility = View.GONE
                } else {
                    binding.nextBtn.isClickable = true
                    binding.nextBtn.setCardBackgroundColor(Color.parseColor("#E62324"))
                    binding.nextBtnShadow.visibility = View.VISIBLE
                }
            }
        }

        val fulltext = getString(R.string.privacy_policy_confirmed)
        val endText = getString(R.string.user_agreement)
        val startIndex = fulltext.indexOf(endText)
        if (startIndex != -1) {
            val endIndex = startIndex + endText.length

            val builder = SpannableStringBuilder(fulltext)
            builder.setSpan(
                ForegroundColorSpan(Color.parseColor("#E62324")),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            binding.privacyPolicy.text = builder
        } else {
            binding.privacyPolicy.text = fulltext
        }

        binding.nextBtn.setOnClickListener {
            findNavController().navigate(NavGraphDirections.startSmsFragment())
        }
    }
}