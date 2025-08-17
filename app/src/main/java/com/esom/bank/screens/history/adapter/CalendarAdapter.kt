package com.esom.bank.screens.history.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esom.bank.R
import com.esom.bank.databinding.CalendarItemBinding
class CalendarAdapter(
    private val context: Context,
    private val currentDay: String, // День в формате "d" (без ведущего нуля)
    private val isCurrentMonth: Boolean,
    private val onDayClicked: (String) -> Unit // Теперь принимает полную дату "yyyy-MM-dd"
) : ListAdapter<String, CalendarAdapter.DayViewHolder>(DayDiffCallback()) {

    companion object {
        var selectedPosition: Int = RecyclerView.NO_POSITION
    }

    inner class DayViewHolder(
        private val binding: CalendarItemBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            fullDate: String, // Полная дата в формате "yyyy-MM-dd" или пустая строка
            position: Int,
            isSelected: Boolean,
            onItemClick: (Int) -> Unit
        ) {
            // Отображаем только день (последнюю часть даты)
            val dayToShow = if (fullDate.isEmpty()) ""
            else fullDate.substring(fullDate.lastIndexOf("-") + 1)

            binding.day.text = dayToShow

            if(fullDate.isEmpty()) {
                binding.day.setTextColor(Color.TRANSPARENT)
            }

            binding.root.background = if (isSelected) {
                ContextCompat.getDrawable(context, R.drawable.background_calendar_item)
            } else {
                null
            }

            binding.day.setTextColor(if(isSelected) Color.parseColor("#FFFFFF")
            else Color.parseColor("#1D1D1B"))

            binding.root.setOnClickListener {
                if (fullDate.isNotEmpty()) { // Игнорируем клик по пустым ячейкам
                    onItemClick(position)
                    onDayClicked(fullDate) // Передаем полную дату
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = CalendarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val fullDate = getItem(position)
        // Проверяем, является ли это текущим днем (сравниваем только день)
        val isCurrentDay = isCurrentMonth &&
                fullDate.isNotEmpty() &&
                fullDate.substring(fullDate.lastIndexOf("-") + 1) == currentDay

        if (isCurrentDay && selectedPosition == RecyclerView.NO_POSITION) {
            selectedPosition = position
            onDayClicked(fullDate)
        }

        holder.bind(
            fullDate = fullDate,
            position = position,
            isSelected = selectedPosition == position,
        ) { newPosition ->
            val previousSelected = selectedPosition
            selectedPosition = newPosition

            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(newPosition)
        }
    }
}

class DayDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}