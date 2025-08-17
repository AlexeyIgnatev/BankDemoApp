package com.esom.bank.screens.history.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.esom.bank.R
import com.esom.bank.databinding.FragmentChooseDateBinding
import com.esom.bank.screens.history.adapter.CalendarAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class ChooseDateFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseDateBinding

    private val calendar = Calendar.getInstance()
    private lateinit var adapter: CalendarAdapter
    private val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    private var startDate = ""
    private var endDate = ""
    private var selectedDate = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChooseDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            it.setBackgroundResource(R.drawable.start_period_background)
            it.elevation = 0F
            binding.endPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.endPeriodLayout.elevation = 8F
            selectedDate = 0
        }

        binding.endPeriodLayout.setOnClickListener {
            it.setBackgroundResource(R.drawable.start_period_background)
            it.elevation = 0F
            binding.startPeriodLayout.setBackgroundResource(R.drawable.currency_background)
            binding.startPeriodLayout.elevation = 8F
            selectedDate = 0
            selectedDate = 1
        }
        binding.chooseBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun setCurrentDate() {
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        calendar.set(Calendar.DAY_OF_MONTH, currentDay.toInt())
    }

    private fun updateMonthAndCalendar() {
        updateMonthTitle()

        val daysInMonth = generateDaysForMonth(calendar)
        val isCurrentMonth = calendar.get(Calendar.MONTH) == currentMonth &&
                calendar.get(Calendar.YEAR) == currentYear

        adapter = CalendarAdapter(
            requireContext(),
            currentDay, // День без ведущего нуля (например, "5")
            isCurrentMonth = isCurrentMonth,
        ) { fullDate -> // fullDate в формате "yyyy-MM-dd"
            if (fullDate.isNotEmpty()) {
                val formattedDate = formatDateToDDMMYYYY(fullDate) // Преобразуем в "dd.MM.yyyy"
                if(selectedDate == 0) {
                    binding.startDate.text = formattedDate
                    startDate = formattedDate
                } else {
                    binding.endDate.text = formattedDate
                    endDate = formattedDate
                }
            }
        }

        binding.calendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.calendar.adapter = adapter
        adapter.submitList(daysInMonth)
    }

    private fun formatDateToDDMMYYYY(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) // Изменили на гггг
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString // Возвращаем оригинальную строку в случае ошибки
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
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH начинается с 0

        val firstDayOfMonth = calendar.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Пустые строки для выравнивания
        repeat(firstDayOfWeek) { days.add("") }

        // Формируем полные даты в формате "yyyy-MM-dd"
        for (day in 1..totalDaysInMonth) {
            val formattedDay = day.toString().padStart(2, '0')
            val formattedMonth = month.toString().padStart(2, '0')
            days.add("$year-$formattedMonth-$formattedDay")
        }

        return days
    }

}