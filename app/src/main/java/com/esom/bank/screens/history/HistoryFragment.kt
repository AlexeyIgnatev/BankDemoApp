package com.esom.bank.screens.history

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.paging.map
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.formatBalanceNew
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

    private val historyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadTransactions()
            model.monthTransactions()
        }
    }

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
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(historyReceiver, IntentFilter("ACTION_HISTORY"))

        val currentMonth = getCurrentMonthInPrepositional()
        binding.titleMonth.text = currentMonth

        adapter = HistoryAdapter(requireContext())
        binding.history.adapter = adapter
        loadTransactions()

        model.monthTransactions()
        model.month.observe(viewLifecycleOwner) { it ->
            when (it) {
                is UiState.Loading -> {}
                is UiState.Error -> binding.root.showErrorSnackbar(it.message)
                is UiState.Success -> {
                    Log.d("MONTH_STATS", "Received data: ${it.data.size} transactions")

                    it.data.forEachIndexed { index, tx ->
                        Log.d("MONTH_STATS", "Transaction $index: type=${tx?.type}, amount=${tx?.amount}, amountType=${tx?.amount?.javaClass}, createdAt=${tx?.createdAt}, date=${if (tx?.createdAt != null) Date(tx.createdAt) else "null"}")
                    }

                    val calendar = java.util.Calendar.getInstance()
                    val currentYear = calendar.get(java.util.Calendar.YEAR)
                    val currentMonth = calendar.get(java.util.Calendar.MONTH)

                    val calendarStart = java.util.Calendar.getInstance().apply {
                        set(currentYear, currentMonth, 1, 0, 0, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val calendarEnd = java.util.Calendar.getInstance().apply {
                        set(currentYear, currentMonth, getActualMaximum(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }

                    val fromTimeMonth = calendarStart.timeInMillis
                    val toTimeMonth = calendarEnd.timeInMillis

                    Log.d("MONTH_STATS", "Time range: $fromTimeMonth - $toTimeMonth")
                    Log.d("MONTH_STATS", "Calendar start: ${Date(fromTimeMonth)}")
                    Log.d("MONTH_STATS", "Calendar end: ${Date(toTimeMonth)}")

                    val monthFormat = java.text.SimpleDateFormat("MMMM", Locale.getDefault())
                    val monthText = monthFormat.format(Date(fromTimeMonth))

                    val monthInPrepositional = when (monthText.lowercase(Locale.getDefault())) {
                        "january" -> "Январь"
                        "february" -> "Февраль"
                        "march" -> "Март"
                        "april" -> "Апрель"
                        "may" -> "Май"
                        "june" -> "Июнь"
                        "july" -> "Июль"
                        "august" -> "Август"
                        "september" -> "Сентябрь"
                        "october" -> "Октябрь"
                        "november" -> "Ноябрь"
                        "december" -> "Декабрь"
                        else -> monthText
                    }

                    val monthTransactions = it.data.filter { tx ->
                        val inRange = tx!!.createdAt in fromTimeMonth..toTimeMonth
                        Log.d("MONTH_STATS", "Transaction filter: createdAt=${tx.createdAt}, date=${Date(tx.createdAt ?: 0L)}, inRange=$inRange, amount=${tx.amount}, amountType=${tx.amount?.javaClass}")
                        inRange
                    }

                    Log.d("MONTH_STATS", "Filtered month transactions: ${monthTransactions.size}")

                    val incomeTransactions = monthTransactions
                        .filterNotNull()
                        .filter {
                            val isIncome = it.type == TransactionEnum.INCOME || it.type == TransactionEnum.INFLOW
                            Log.d("MONTH_STATS", "Income check: type=${it.type}, isIncome=$isIncome, amount=${it.amount}, amountType=${it.amount?.javaClass}, amountNull=${it.amount == null}")
                            isIncome
                        }

                    val expenseTransactions = monthTransactions
                        .filterNotNull()
                        .filter {
                            val isExpense = it.type == TransactionEnum.EXPENSE || it.type == TransactionEnum.TRANSFER
                            Log.d("MONTH_STATS", "Expense check: type=${it.type}, isExpense=$isExpense, amount=${it.amount}, amountType=${it.amount?.javaClass}, amountNull=${it.amount == null}")
                            isExpense
                        }

                    Log.d("MONTH_STATS", "Income transactions: ${incomeTransactions.size}")
                    Log.d("MONTH_STATS", "Expense transactions: ${expenseTransactions.size}")

                    incomeTransactions.forEachIndexed { index, transaction ->
                        Log.d("MONTH_STATS", "Income transaction $index: amount=${transaction.amount}, amountType=${transaction.amount?.javaClass}, toDouble=${transaction.amount?.toDouble()}")
                    }

                    expenseTransactions.forEachIndexed { index, transaction ->
                        Log.d("MONTH_STATS", "Expense transaction $index: amount=${transaction.amount}, amountType=${transaction.amount?.javaClass}, toDouble=${transaction.amount?.toDouble()}")
                    }

                    val incomeSum = incomeTransactions.sumOf { amount ->
                        val amountValue = amount.amount
                        val doubleValue = amountValue?.toDouble() ?: 0.0
                        Log.d("MONTH_STATS", "Adding income: amount=$amountValue, toDouble=$doubleValue, amountType=${amountValue?.javaClass}")
                        doubleValue
                    }

                    val expenseSum = expenseTransactions.sumOf { amount ->
                        val amountValue = amount.amount
                        val doubleValue = amountValue?.toDouble() ?: 0.0
                        Log.d("MONTH_STATS", "Adding expense: amount=$amountValue, toDouble=$doubleValue, amountType=${amountValue?.javaClass}")
                        doubleValue
                    }

                    Log.d("MONTH_STATS", "Final income sum: $incomeSum (type: ${incomeSum.javaClass})")
                    Log.d("MONTH_STATS", "Final expense sum: $expenseSum (type: ${expenseSum.javaClass})")

                    binding.incomeTitle.text = "Доходы за $monthInPrepositional"
                    binding.expencesTitle.text = "Расходы за $monthInPrepositional"

                    binding.income.text = incomeSum.formatBalanceNew()
                    binding.expences.text = expenseSum.formatBalanceNew()

                    Log.d("MONTH_STATS", "UI updated - Income: ${binding.income.text}, Expense: ${binding.expences.text}")
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

    private fun getCurrentMonthInPrepositional(): String {
        val calendar = java.util.Calendar.getInstance()
        val russianLocale = Locale("ru", "RU")
        val monthFormat = java.text.SimpleDateFormat("MMMM", russianLocale)
        val monthText = monthFormat.format(calendar.time)

        return when (monthText.lowercase(russianLocale)) {
            "january" -> "Январь"
            "february" -> "Февраль"
            "march" -> "Март"
            "april" -> "Апрель"
            "may" -> "Май"
            "june" -> "Июнь"
            "july" -> "Июль"
            "august" -> "Август"
            "september" -> "Сентябрь"
            "october" -> "Октябрь"
            "november" -> "Ноябрь"
            "december" -> "Декабрь"
            else -> monthText
        }
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("HistoryFragment", "Starting to collect paging data")

                model.historyPaging(
                    currencyEnum = model.getCurrency(),
                    fromTime = model.getFromTime(),
                    toTime = model.getToTime()
                ).collectLatest { pagingData ->
                    adapter.submitData(PagingData.empty())

                    Log.d("HistoryFragment", "Received paging data, submitting to adapter")
                    adapter.submitData(pagingData)
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error loading transactions: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(historyReceiver)
    }
}