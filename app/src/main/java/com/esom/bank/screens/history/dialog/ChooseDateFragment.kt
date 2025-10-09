package com.esom.bank.screens.history.dialog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.esom.bank.R
import com.esom.bank.databinding.FragmentChooseDateBinding
import com.esom.bank.screens.history.adapter.CalendarAdapter
import com.esom.bank.screens.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class ChooseDateFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseDateBinding
    private val model: MainViewModel by activityViewModels()
    private val calendar = Calendar.getInstance()
    private lateinit var adapter: CalendarAdapter
    private val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    private var startDate = ""
    private var endDate = ""
    private var selectedDateMode = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChooseDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setInitialDates()
        setCurrentDate()
        updateMonthAndCalendar()
        binding.previousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthAndCalendar()
        }

        binding.nextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthAndCalendar()
        }
        binding.startPeriodLayout.setOnClickListener {
            binding.startPeriodIcon.setImageResource(R.drawable.white_period_icon)
            binding.startTitle.setTextColor(getColor(requireContext(), R.color.white))
            binding.startDate.setTextColor(getColor(requireContext(), R.color.white))
            binding.endPeriodIcon.setImageResource(R.drawable.gray_period_icon)
            binding.endDate.setTextColor(Color.parseColor("#535353"))
            binding.endTitle.setTextColor(Color.parseColor("#535353"))

            it.setBackgroundResource(R.drawable.start_period_background)
            it.elevation = 0F
            binding.endPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.endPeriodLayout.elevation = 8F
            selectedDateMode = 0
            updateCalendarSelection()
        }

        binding.endPeriodLayout.setOnClickListener {
            binding.endPeriodIcon.setImageResource(R.drawable.white_period_icon)
            binding.endTitle.setTextColor(getColor(requireContext(), R.color.white))
            binding.endDate.setTextColor(getColor(requireContext(), R.color.white))
            binding.startPeriodIcon.setImageResource(R.drawable.gray_period_icon)
            binding.startDate.setTextColor(Color.parseColor("#535353"))
            binding.startTitle.setTextColor(Color.parseColor("#535353"))

            it.setBackgroundResource(R.drawable.start_period_background)
            it.elevation = 0F
            binding.startPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.startPeriodLayout.elevation = 8F
            selectedDateMode = 1
            updateCalendarSelection()
        }
        binding.chooseBtn.setOnClickListener {
            val dateString = binding.startDate.text.toString()
            val endDateString = binding.endDate.text.toString()
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date: Long = format.parse(dateString)?.time ?: 0L
            val endDate = format.parse(endDateString)?.time ?: 0L
            model.setFromTime(date)
            model.setToTime(endDate)
            val intent = Intent("ACTION_HISTORY")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

            dismiss()
        }
    }

    private fun setInitialDates() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val fromTime = model.getFromTime()
        val toTime = model.getToTime()

        startDate = formatDateToYYYYMMDD(dateFormat.format(fromTime))
        endDate = formatDateToYYYYMMDD(dateFormat.format(toTime))
        binding.startDate.text = dateFormat.format(fromTime)
        binding.endDate.text = dateFormat.format(toTime)
    }

    private fun setCurrentDate() {
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        calendar.set(Calendar.DAY_OF_MONTH, currentDay.toInt())
    }

    private fun updateMonthAndCalendar() {
        updateMonthTitle()

        val daysInMonth = generateDaysForMonth(calendar)

        adapter = CalendarAdapter(
            requireContext(),
        ) { fullDate ->
            if (fullDate.isNotEmpty()) {
                val formattedDate = formatDateToDDMMYYYY(fullDate)
                if (selectedDateMode == 0) {
                    binding.startDate.text = formattedDate
                    startDate = fullDate
                } else {
                    binding.endDate.text = formattedDate
                    endDate = fullDate
                }
                updateCalendarSelection()
            }
        }

        binding.calendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.calendar.adapter = adapter
        adapter.submitList(daysInMonth)
        updateCalendarSelection()
    }

    private fun updateCalendarSelection() {
        val startDateForAdapter = startDate.ifEmpty { null }
        val endDateForAdapter = endDate.ifEmpty { null }
        adapter.setSelectedDates(startDateForAdapter, endDateForAdapter, selectedDateMode)
    }

    private fun formatDateToYYYYMMDD(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun formatDateToDDMMYYYY(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun updateMonthTitle() {
        val monthName = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
            .format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.monthYear.text = monthName
    }

    private fun generateDaysForMonth(calendar: Calendar): List<String> {
        val days = mutableListOf<String>()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(firstDayOfWeek) { days.add("") }

        for (day in 1..totalDaysInMonth) {
            val formattedDay = day.toString().padStart(2, '0')
            val formattedMonth = month.toString().padStart(2, '0')
            days.add("$year-$formattedMonth-$formattedDay")
        }

        return days
    }
}