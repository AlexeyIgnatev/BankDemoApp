package com.esom.bank.screens.history

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.paging.PagingData
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentHistoryBinding
import com.esom.bank.screens.history.adapter.HistoryAdapter
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.TransactionSuccessMapper
import com.esom.bank.screens.history.model.isUserTransfer
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val model: MainViewModel by activityViewModels()
    private lateinit var adapter: HistoryAdapter
    private var historyJob: Job? = null

    private val historyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { root, insets, rect ->
            root.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(historyReceiver, IntentFilter(ACTION_HISTORY))

        adapter = HistoryAdapter(
            withoutTransfers = model.getWithoutTransactions(),
            onTransactionClick = ::openSuccessTransfer
        )
        binding.history.adapter = adapter

        binding.searchInput.doAfterTextChanged { adapter.updateSearch(it?.toString().orEmpty()) }
        binding.swipeRefreshLayout.setOnRefreshListener(::refreshData)
        binding.nonTransactionBtn.setOnClickListener {
            val withoutTransfers = !model.getWithoutTransactions()
            model.setWithoutTransactions(withoutTransfers)
            adapter.updateFilter(withoutTransfers)
            updateFilterLabels()
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

        model.month.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(state.message)
                }
                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    updatePeriodStats(state.data.filterNotNull())
                }
            }
        }

        refreshData()
    }

    private fun refreshData() {
        updateFilterLabels()
        loadTransactions()
        model.monthTransactions()
    }

    private fun loadTransactions() {
        historyJob?.cancel()
        historyJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                model.historyPaging(
                    currencyEnum = model.getCurrency(),
                    fromTime = model.getFromTime(),
                    toTime = model.getToTime()
                ).collectLatest { pagingData ->
                    adapter.submitData(PagingData.empty())
                    adapter.submitData(pagingData)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(
                        error.message ?: getString(R.string.something_went_wrong)
                    )
                }
            }
        }
    }

    private fun updatePeriodStats(transactions: List<TransactionModel>) {
        val visibleTransactions = if (model.getWithoutTransactions()) {
            transactions.filterNot { it.isUserTransfer() }
        } else {
            transactions
        }
        val income = visibleTransactions
            .filter { it.type == TransactionEnum.INCOME || it.type == TransactionEnum.INFLOW }
            .sumOf { it.amount ?: 0.0 }
        val expenses = visibleTransactions
            .filter {
                it.type == TransactionEnum.EXPENSE ||
                    it.type == TransactionEnum.TRANSFER ||
                    !it.recipientFullName.isNullOrBlank()
            }
            .sumOf { it.amount ?: 0.0 }
        val label = statsPeriodLabel()

        binding.incomeTitle.text = "Доходы за $label"
        binding.expencesTitle.text = "Расходы за $label"
        binding.income.text = income.formatBalanceNew()
        binding.expences.text = expenses.formatBalanceNew()
    }

    private fun updateFilterLabels() {
        val withoutTransfers = model.getWithoutTransactions()
        binding.nonTransactionLayout.setBackgroundResource(
            if (withoutTransfers) R.drawable.data_period_background
            else R.drawable.gray_period_background
        )
        binding.nonTransactionTitle.setTextColor(
            requireContext().getColor(if (withoutTransfers) R.color.white else R.color.title)
        )

        val from = model.getFromTime()
        val to = model.getToTime()
        binding.titleMonth.text =
            SimpleDateFormat("LLLL", RUSSIAN_LOCALE).format(Date(to))

        val currencies = model.getCurrency()
        binding.active.text = if (currencies.size == CurrencyEnum.supportedValues.size) {
            getString(R.string.actives)
        } else {
            "Активы: ${currencies.size}"
        }

        val days = TimeUnit.MILLISECONDS.toDays((to - from).coerceAtLeast(0L))
        binding.periodTitle.text = when {
            days <= 8 -> "Неделя"
            days <= 32 -> "1 месяц"
            days <= 95 -> "3 месяца"
            else -> "Период"
        }
    }

    private fun statsPeriodLabel(): String =
        if (isSameMonth(model.getFromTime(), model.getToTime())) {
            SimpleDateFormat("LLLL", RUSSIAN_LOCALE).format(Date(model.getFromTime()))
        } else {
            "выбранный период"
        }

    private fun isSameMonth(from: Long, to: Long): Boolean {
        val start = Calendar.getInstance().apply { timeInMillis = from }
        val end = Calendar.getInstance().apply { timeInMillis = to }
        return start.get(Calendar.YEAR) == end.get(Calendar.YEAR) &&
            start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
    }

    private fun openSuccessTransfer(transaction: TransactionModel) {
        val user = (model.myData.value as? UiState.Success)?.data
        model.setLastSuccessOperation(
            TransactionSuccessMapper.toSuccessOperation(
                context = requireContext(),
                transaction = transaction,
                user = user
            )
        )
        findParentNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
    }

    override fun onDestroyView() {
        historyJob?.cancel()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(historyReceiver)
        super.onDestroyView()
    }

    private companion object {
        const val ACTION_HISTORY = "ACTION_HISTORY"
        val RUSSIAN_LOCALE = Locale("ru", "RU")
    }
}
