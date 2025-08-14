package com.esom.bank.screens.transfer.dialog

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.esom.bank.R
import com.esom.bank.databinding.FragmentFailTransferBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FailTransferFragment : DialogFragment() {
    private lateinit var binding: FragmentFailTransferBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFailTransferBinding.inflate(inflater, container, false)
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
    }
}