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
    private val currentDay: String,
    private val isCurrentMonth: Boolean,
    private val onDayClicked: (String) -> Unit
) : ListAdapter<String, CalendarAdapter.DayViewHolder>(DayDiffCallback()) {

    companion object {
        var selectedPosition: Int = RecyclerView.NO_POSITION
    }

    inner class DayViewHolder(
        private val binding: CalendarItemBinding, private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            day: String, position: Int, isSelected: Boolean, onItemClick: (Int) -> Unit
        ) {
            binding.day.text = day

            if(day.isEmpty()) {
                binding.day.text = ""
                binding.day.setTextColor(Color.TRANSPARENT)
            }

            binding.root.background = if (isSelected) {
                ContextCompat.getDrawable(context, R.drawable.background_calendar_item)
            } else {
                null
            }
            binding.day.setTextColor( if(isSelected) Color.parseColor("#FFFFFF")
            else Color.parseColor("#1D1D1B"))

            binding.root.setOnClickListener {
                onItemClick(position)
                onDayClicked(day)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding =
            CalendarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = getItem(position)
        val isCurrentDay = isCurrentMonth && day == currentDay

        if (isCurrentDay && selectedPosition == RecyclerView.NO_POSITION) {
            selectedPosition = position
            if (day.isNotEmpty()) {
                onDayClicked(day)
            }
        }

        holder.bind(
            day = day,
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

    override fun getItemCount() = currentList.size
}

class DayDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}