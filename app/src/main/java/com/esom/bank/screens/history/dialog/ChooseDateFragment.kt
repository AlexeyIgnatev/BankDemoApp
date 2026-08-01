package com.esom.bank.screens.history.dialog

import android.content.Intent
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
            setSelectionMode(isStartMode = true)
            updateCalendarSelection()
        }

        binding.endPeriodLayout.setOnClickListener {
            setSelectionMode(isStartMode = false)
            updateCalendarSelection()
        }
        binding.chooseBtn.setOnClickListener {
            val dateString = binding.startDate.text.toString()
            val endDateString = binding.endDate.text.toString()
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val date: Long = format.parse(dateString)?.time ?: 0L

            val endDateParsed = format.parse(endDateString)
            val endDate = if (endDateParsed != null) {
                val calendar = Calendar.getInstance()
                calendar.time = endDateParsed
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            } else {
                0L
            }

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

        binding.startDate.text = dateFormat.format(fromTime)
        binding.endDate.text = dateFormat.format(toTime)
        calendar.timeInMillis = fromTime
        setSelectionMode(isStartMode = true)
    }

    private fun updateMonthAndCalendar() {
        updateMonthTitle()

        val daysInMonth = generateDaysForMonth(calendar)

        val adapter = CalendarAdapter(
            selectedStartDateProvider = {
                displayDateToIso(binding.startDate.text?.toString()).ifEmpty { null }
            },
            selectedEndDateProvider = {
                displayDateToIso(binding.endDate.text?.toString()).ifEmpty { null }
            },
            selectionModeProvider = { currentSelectionMode() }
        ) { fullDate ->
            if (fullDate.isNotEmpty()) {
                val formattedDate = formatDateToDDMMYYYY(fullDate)
                if (isStartMode()) {
                    binding.startDate.text = formattedDate
                    val currentEnd = parseDisplayDate(binding.endDate.text?.toString())
                    val selectedStart = parseDisplayDate(formattedDate)
                    if (selectedStart != null && (currentEnd == null || currentEnd.before(selectedStart))) {
                        binding.endDate.text = formattedDate
                    }
                    setSelectionMode(isStartMode = false)
                } else {
                    binding.endDate.text = formattedDate
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
        binding.calendar.adapter?.notifyDataSetChanged()
    }

    private fun isStartMode(): Boolean = binding.startPeriodLayout.isSelected

    private fun currentSelectionMode(): Int = if (isStartMode()) 0 else 1

    private fun setSelectionMode(isStartMode: Boolean) {
        binding.startPeriodLayout.isSelected = isStartMode
        binding.endPeriodLayout.isSelected = !isStartMode

        if (isStartMode) {
            binding.startPeriodIcon.setImageResource(R.drawable.white_period_icon)
            binding.startTitle.setTextColor(getColor(requireContext(), R.color.white))
            binding.startDate.setTextColor(getColor(requireContext(), R.color.white))
            binding.startPeriodLayout.setBackgroundResource(R.drawable.start_period_background)
            binding.startPeriodLayout.elevation = 0F

            binding.endPeriodIcon.setImageResource(R.drawable.gray_period_icon)
            binding.endDate.setTextColor(getColor(requireContext(), R.color.period_inactive_text))
            binding.endTitle.setTextColor(getColor(requireContext(), R.color.period_inactive_text))
            binding.endPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.endPeriodLayout.elevation = 8F
        } else {
            binding.endPeriodIcon.setImageResource(R.drawable.white_period_icon)
            binding.endTitle.setTextColor(getColor(requireContext(), R.color.white))
            binding.endDate.setTextColor(getColor(requireContext(), R.color.white))
            binding.endPeriodLayout.setBackgroundResource(R.drawable.start_period_background)
            binding.endPeriodLayout.elevation = 0F

            binding.startPeriodIcon.setImageResource(R.drawable.gray_period_icon)
            binding.startDate.setTextColor(getColor(requireContext(), R.color.period_inactive_text))
            binding.startTitle.setTextColor(getColor(requireContext(), R.color.period_inactive_text))
            binding.startPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.startPeriodLayout.elevation = 8F
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

    private fun displayDateToIso(value: String?): String {
        val date = parseDisplayDate(value) ?: return ""
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }

    private fun parseDisplayDate(value: String?) = runCatching {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(value.orEmpty())
    }.getOrNull()

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
