package com.esom.bank.screens.history.adapter

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
    private val selectedStartDateProvider: () -> String?,
    private val selectedEndDateProvider: () -> String?,
    private val selectionModeProvider: () -> Int,
    private val onDayClicked: (String) -> Unit
) : ListAdapter<String, CalendarAdapter.DayViewHolder>(DayDiffCallback()) {

    inner class DayViewHolder(
        private val binding: CalendarItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            fullDate: String,
            onItemClick: (String) -> Unit
        ) {
            val dayToShow = if (fullDate.isEmpty()) ""
            else fullDate.substring(fullDate.lastIndexOf("-") + 1)

            binding.day.text = dayToShow

            if (fullDate.isEmpty()) {
                binding.day.setTextColor(Color.TRANSPARENT)
                binding.root.background = null
            } else {
                val isStartSelected = fullDate == selectedStartDateProvider()
                val isEndSelected = fullDate == selectedEndDateProvider()
                val isSelected = when (selectionModeProvider()) {
                    0 -> isStartSelected
                    1 -> isEndSelected
                    else -> isStartSelected || isEndSelected
                }

                binding.root.background = if (isSelected) {
                    ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.background_calendar_item
                    )
                } else {
                    null
                }

                binding.day.setTextColor(
                    if (isSelected) ContextCompat.getColor(binding.root.context, R.color.white)
                    else ContextCompat.getColor(binding.root.context, R.color.title)
                )
            }

            binding.root.setOnClickListener {
                if (fullDate.isNotEmpty()) {
                    onItemClick(fullDate)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding =
            CalendarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val fullDate = getItem(position)
        holder.bind(fullDate) { selectedDate ->
            onDayClicked(selectedDate)
        }
    }
}

class DayDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}
