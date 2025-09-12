package com.esom.bank.screens.history

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.paging.map
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentHistoryBinding
import com.esom.bank.screens.history.adapter.HistoryAdapter
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val model: MainViewModel by activityViewModels()

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }
        adapter = HistoryAdapter(requireContext())
        binding.history.adapter = adapter
        loadTransactions()

        model.monthTransactions()
        model.month.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}
                is UiState.Error -> binding.root.showErrorSnackbar(it.message)
                is UiState.Success -> {
                    val calendarStart = java.util.Calendar.getInstance().apply {
                        set(2025, java.util.Calendar.SEPTEMBER, 1, 0, 0, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val calendarEnd = java.util.Calendar.getInstance().apply {
                        set(2025, java.util.Calendar.SEPTEMBER, 30, 23, 59, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }

                    val fromTimeMonth = calendarStart.timeInMillis
                    val toTimeMonth = calendarEnd.timeInMillis
                    val monthFormat = java.text.SimpleDateFormat("MMMM", Locale.getDefault())
                    val monthText = monthFormat.format(Date(fromTimeMonth))

                    val monthInPrepositional = when (monthText.lowercase(Locale.getDefault())) {
                        "января" -> "январь"
                        "февраля" -> "февраль"
                        "марта" -> "март"
                        "апреля" -> "апрель"
                        "мая" -> "май"
                        "июня" -> "июнь"
                        "июля" -> "июль"
                        "августа" -> "август"
                        "сентября" -> "сентябрь"
                        "октября" -> "октябрь"
                        "ноября" -> "ноябрь"
                        "декабря" -> "декабрь"
                        else -> monthText
                    }

                    val monthTransactions = it.data.filter { tx ->
                        tx.createdAt in fromTimeMonth..toTimeMonth
                    }

                    val incomeSum = monthTransactions
                        .filter { it.type == TransactionEnum.INCOME || it.type == TransactionEnum.INFLOW }
                        .sumOf { it.amount }

                    val expenseSum = monthTransactions
                        .filter { it.type == TransactionEnum.EXPENSE || it.type == TransactionEnum.TRANSFER }
                        .sumOf { it.amount }

                    binding.incomeTitle.text = "Доходы за $monthInPrepositional"
                    binding.expencesTitle.text = "Расходы за $monthInPrepositional"

                    binding.income.text = incomeSum.toInt().toString()
                    binding.expences.text = expenseSum.toInt().toString()
                }

            }
        }
        binding.dataPeriodBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChooseDateFragment())
        }
        binding.periodBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChoosePeriodFragment())
        }
        binding.activeBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChooseActiveFragment())
        }
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            model.historyPaging(
                currencyEnum = model.getCurrency(),
                fromTime = model.getFromTime(),
                toTime = model.getToTime()
            ).collectLatest { pagingData ->
                adapter.submitData(pagingData)
                adapter.refresh()
            }
        }
    }
}