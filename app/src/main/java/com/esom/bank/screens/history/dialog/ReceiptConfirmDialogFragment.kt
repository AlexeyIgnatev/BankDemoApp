package com.esom.bank.screens.history.dialog

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.esom.bank.R
import com.esom.bank.databinding.FragmentReceiptConfirmBinding

class ReceiptConfirmDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentReceiptConfirmBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReceiptConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog?.window

        if (window != null) {
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val paddingInPixels = (16 * displayMetrics.density).toInt()
            val heightInPixels = requireContext().resources.getDimensionPixelSize(R.dimen._250dp)

            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                width = screenWidth - (2 * paddingInPixels)
                height = heightInPixels
            }
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.attributes = lp
        }

        binding.closeBtn.setOnClickListener { dismiss() }
        binding.confirmBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(CONFIRMED_KEY to true)
            )
            dismiss()
        }
    }

    companion object {
        const val REQUEST_KEY = "receipt_confirm_request"
        const val CONFIRMED_KEY = "receipt_confirmed"
    }
}
