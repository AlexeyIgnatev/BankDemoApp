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
    private val onDayClicked: (String) -> Unit
) : ListAdapter<String, CalendarAdapter.DayViewHolder>(DayDiffCallback()) {

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var selectionMode: Int = 0

    fun setSelectedDates(startDate: String?, endDate: String?, mode: Int) {
        selectedStartDate = startDate
        selectedEndDate = endDate
        selectionMode = mode
        notifyDataSetChanged()
    }

    inner class DayViewHolder(
        private val binding: CalendarItemBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            fullDate: String,
            onItemClick: (String) -> Unit
        ) {
            val dayToShow = if (fullDate.isEmpty()) ""
            else fullDate.substring(fullDate.lastIndexOf("-") + 1)

            binding.day.text = dayToShow

            if(fullDate.isEmpty()) {
                binding.day.setTextColor(Color.TRANSPARENT)
                binding.root.background = null
            } else {
                val isStartSelected = fullDate == selectedStartDate
                val isEndSelected = fullDate == selectedEndDate

                binding.root.background = if (isStartSelected || isEndSelected) {
                    ContextCompat.getDrawable(context, R.drawable.background_calendar_item)
                } else {
                    null
                }

                binding.day.setTextColor(if(isStartSelected || isEndSelected) Color.parseColor("#FFFFFF")
                else Color.parseColor("#1D1D1B"))
            }

            binding.root.setOnClickListener {
                if (fullDate.isNotEmpty()) {
                    onItemClick(fullDate)
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
        holder.bind(fullDate) { selectedDate ->
            onDayClicked(selectedDate)
        }
    }
}

class DayDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}