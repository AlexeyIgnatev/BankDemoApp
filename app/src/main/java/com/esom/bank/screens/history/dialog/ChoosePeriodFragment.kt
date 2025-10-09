package com.esom.bank.screens.history.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.esom.bank.databinding.FragmentChoosePeriodBinding
import com.esom.bank.screens.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class ChoosePeriodFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChoosePeriodBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChoosePeriodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.month3Btn.setOnClickListener { binding.last3monthCheck.isChecked = !binding.last3monthCheck.isChecked }
        binding.monthBtn.setOnClickListener { binding.lastMonthCheck.isChecked = !binding.lastMonthCheck.isChecked }
        binding.weekBtn.setOnClickListener { binding.lastWeekCheck.isChecked = !binding.lastWeekCheck.isChecked }

        binding.last3monthCheck.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                binding.lastMonthCheck.isChecked = false
                binding.lastWeekCheck.isChecked = false
            }
        }
        binding.lastMonthCheck.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                binding.last3monthCheck.isChecked = false
                binding.lastWeekCheck.isChecked = false
            }
        }
        binding.lastWeekCheck.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                binding.last3monthCheck.isChecked = false
                binding.lastMonthCheck.isChecked = false
            }
        }
        binding.chooseBtn.setOnClickListener {
            if(binding.last3monthCheck.isChecked) {
                val calendar = Calendar.getInstance()

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val toTime = calendar.timeInMillis

                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val fromTime = calendar.timeInMillis

                model.setFromTime(fromTime)
                model.setToTime(toTime)
            }

            if(binding.lastMonthCheck.isChecked) {
                val calendar = Calendar.getInstance()

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val toTime = calendar.timeInMillis

                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val fromTime = calendar.timeInMillis

                model.setFromTime(fromTime)
                model.setToTime(toTime)
            }

            if (binding.lastWeekCheck.isChecked) {
                val calendar = Calendar.getInstance()

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val toTime = calendar.timeInMillis

                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val fromTime = calendar.timeInMillis

                model.setFromTime(fromTime)
                model.setToTime(toTime)
            }

            val intent = Intent("ACTION_HISTORY")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

            dismiss()
        }
    }
}