package com.esom.bank.common.dialogs

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow


open class BlurBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private val TAG = "BlurBottomSheetDialog"
    private lateinit var blurView: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireDialog().window!!.setDimAmount(0f)

        (requireDialog() as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        (requireDialog() as BottomSheetDialog).behavior.skipCollapsed = true
        (requireDialog() as BottomSheetDialog).behavior.peekHeight =
            resources.displayMetrics.heightPixels

        (requireDialog() as BottomSheetDialog).behavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Log.d(TAG, "onStateChanged: $newState")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (isAdded) {
                    if (slideOffset < 0) {
                        blurView.alpha = 1 - abs(slideOffset).pow(2)
                    } else {
                        blurView.alpha = 1f
                    }
                }
            }
        })

        blurView = ImageView(requireContext())
        (requireDialog().window!!.decorView as ViewGroup).addView(
            blurView, 0, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        viewLifecycleOwner.lifecycleScope.launch {
            delay(200L)
            Blurry.with(requireContext())
                .radius(20)
                .async()
                .capture(requireActivity().window!!.decorView.rootView)
                .into(blurView)

            animate(blurView)
        }
    }

    private fun animate(v: View) {
        val alpha = AlphaAnimation(0f, 1f)
        alpha.duration = 400L
        v.startAnimation(alpha)
    }
}