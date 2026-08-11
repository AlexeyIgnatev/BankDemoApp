package com.esom.bank.screens.history

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.esom.bank.MainNavGraphDirections
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.getFontCompat
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentHistoryBinding
import com.esom.bank.screens.history.adapter.HistoryAdapter
import com.esom.bank.screens.history.model.HistoryTypeFilter
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.TransactionSuccessMapper
import com.esom.bank.screens.history.model.filterHistoryTransactions
import com.esom.bank.screens.history.model.isHistoryExpenseTransaction
import com.esom.bank.screens.history.model.isHistoryIncomeTransaction
import com.esom.bank.screens.history.model.isHistoryTransferTransaction
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    private val uiModel: HistoryUiStateViewModel by viewModels()

    private val historyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { root, insets, rect ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(
                top = rect.top + systemBars.top,
                bottom = rect.bottom + systemBars.bottom
            )
            insets
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(historyReceiver, IntentFilter(ACTION_HISTORY))

        uiModel.setWithoutTransfers(model.getWithoutTransactions())
        val adapter = HistoryAdapter(
            stateProvider = { uiModel.uiState.value.adapterState },
            balancesVisibleProvider = { model.balancesVisible.value ?: true },
            onTransactionClick = ::openSuccessTransfer
        )
        binding.history.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener(::refreshData)
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.balanceVisibilityBtn.setOnClickListener { model.toggleBalancesVisibility() }
        binding.typeFilter.setOnClickListener { showTypeFilter() }
        binding.periodFilter.setOnClickListener { showPeriodFilter() }
        binding.walletFilter.setOnClickListener { showWalletFilter() }
        binding.amountFilter.setOnClickListener { showAmountFilter() }
        binding.expensesCard.setOnClickListener { openAnalysis(MODE_EXPENSES) }
        binding.incomeCard.setOnClickListener { openAnalysis(MODE_INCOME) }
        binding.transfersCard.setOnClickListener {
            uiModel.setTypeFilter(HistoryTypeFilter.TRANSFERS)
            adapter.regroup()
            binding.typeFilter.text = "Переводы"
            renderStats(model.balancesVisible.value ?: true)
        }

        model.balancesVisible.observe(viewLifecycleOwner) { visible ->
            adapter.refreshBalanceVisibility()
            binding.balanceVisibilityBtn.setImageResource(
                if (visible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
            renderStats(visible)
        }
        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                uiModel.setWallets(state.data.wallets)
                renderStats(model.balancesVisible.value ?: true)
            }
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
                    uiModel.setPeriodTransactions(state.data.filterNotNull())
                    renderStats(model.balancesVisible.value ?: true)
                }
            }
        }
        updateFilterLabels()
        refreshData()
    }

    private fun refreshData() {
        updateFilterLabels()
        loadTransactions()
        model.monthTransactions()
    }

    private fun loadTransactions() {
        val adapter = binding.history.adapter as HistoryAdapter
        uiModel.replaceHistoryJob(viewLifecycleOwner.lifecycleScope.launch {
            try {
                model.historyPaging(model.getCurrency(), model.getFromTime(), model.getToTime())
                    .collectLatest { data ->
                        adapter.submitData(PagingData.empty())
                        adapter.submitData(data)
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(error.message ?: getString(R.string.something_went_wrong))
                }
            }
        })
    }

    private fun renderStats(visible: Boolean) {
        val transactions = visiblePeriodTransactions()
        val income = transactions.filter { it.isHistoryIncomeTransaction() }.sumOf(::amountInSom)
        val transfers = transactions.filter { it.isHistoryTransferTransaction() && !it.isHistoryIncomeTransaction() }.sumOf(::amountInSom)
        val expenses = transactions.filter { it.isHistoryExpenseTransaction() }.sumOf(::amountInSom)
        binding.expensesTitle.text = "Расходы за ${statsPeriodLabel()}"
        binding.expenses.setBalance("${expenses.formatBalanceNew()} сом", visible)
        binding.transfers.setBalance("${transfers.formatBalanceNew()} сом", visible)
        binding.income.setBalance("+${income.formatBalanceNew()} сом", visible)
    }

    private fun visiblePeriodTransactions(): List<TransactionModel> =
        uiModel.uiState.value.periodTransactions
            .filter { it.currencyEnum == null || it.currencyEnum in model.getCurrency() }
            .filterHistoryTransactions(uiModel.uiState.value.adapterState)

    private fun amountInSom(transaction: TransactionModel): Double {
        val rate = when (transaction.currencyEnum) {
            CurrencyEnum.SOM, null -> 1.0
            else -> uiModel.uiState.value.wallets.firstOrNull { it.currency == transaction.currencyEnum }
                ?.let { if (it.sellRate > 0.0) it.sellRate else it.buyRate }
                ?.takeIf { it > 0.0 } ?: 1.0
        }
        return (transaction.amount ?: 0.0) * rate
    }

    private fun showTypeFilter() {
        showOptions(
            "Что показывать",
            listOf(
                "Все операции" to HistoryTypeFilter.ALL,
                "Переводы" to HistoryTypeFilter.TRANSFERS,
                "Конвертации" to HistoryTypeFilter.CONVERSIONS,
                "Зачисления" to HistoryTypeFilter.INCOME,
                "Расходы" to HistoryTypeFilter.EXPENSES
            )
        ) { label, filter ->
            uiModel.setTypeFilter(filter)
            (binding.history.adapter as HistoryAdapter).regroup()
            binding.typeFilter.text = label
            renderStats(model.balancesVisible.value ?: true)
        }
    }

    private fun showPeriodFilter() {
        showOptions(
            "Период",
            listOf("Неделя" to 7, "Месяц" to 30, "Год" to 365, "За период" to -1)
        ) { label, days ->
            if (days < 0) {
                findParentNavController().navigate(NavGraphDirections.startChooseDateFragment())
            } else {
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                }
                val start = (end.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -days)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                model.setFromTime(start.timeInMillis)
                model.setToTime(end.timeInMillis)
                binding.periodFilter.text = label
                refreshData()
            }
        }
    }

    private fun showWalletFilter() {
        showOptions(
            "Кошелёк",
            listOf(
                "Все кошельки" to CurrencyEnum.supportedValues,
                "Сом" to listOf(CurrencyEnum.SOM),
                "Салам" to listOf(CurrencyEnum.ESOM),
                "USDT" to listOf(CurrencyEnum.USDT_TRC20)
            )
        ) { label, currencies ->
            model.setCurrency(currencies)
            binding.walletFilter.text = label
            renderStats(model.balancesVisible.value ?: true)
            refreshData()
        }
    }

    private fun showAmountFilter() {
        val dialog = BottomSheetDialog(requireContext())
        val content = dialogContainer("Сумма")
        val fields = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val from = amountInput("От").apply { layoutParams = weightedParams(8) }
        val to = amountInput("До").apply { layoutParams = weightedParams(0) }
        fields.addView(from)
        fields.addView(to)
        content.addView(fields)
        content.addView(actionButton(getString(R.string.apply_filter)).apply {
            setOnClickListener {
                uiModel.setAmountFilter(from.number(), to.number())
                (binding.history.adapter as HistoryAdapter).regroup()
                binding.amountFilter.text = when {
                    from.text.isNotBlank() && to.text.isNotBlank() -> "${from.text}–${to.text}"
                    from.text.isNotBlank() -> "От ${from.text}"
                    to.text.isNotBlank() -> "До ${to.text}"
                    else -> "Сумма"
                }
                renderStats(model.balancesVisible.value ?: true)
                dialog.dismiss()
            }
        })
        dialog.setContentView(content)
        dialog.setOnShowListener { from.requestFocus(); showKeyboard(from) }
        dialog.show()
    }

    private fun <T> showOptions(title: String, values: List<Pair<String, T>>, selected: (String, T) -> Unit) {
        val dialog = BottomSheetDialog(requireContext())
        val content = dialogContainer(title)
        values.forEach { (label, value) ->
            content.addView(TextView(requireContext()).apply {
                text = label
                textSize = 18f
                setTextColor(requireContext().getColor(R.color.title))
                setPadding(4.dp, 18.dp, 4.dp, 18.dp)
                setOnClickListener { selected(label, value); dialog.dismiss() }
            })
        }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun dialogContainer(title: String) = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20.dp)
        setBackgroundColor(requireContext().getColor(R.color.card_bg))
        addView(TextView(context).apply {
            text = title
            textSize = 25f
            setTextColor(context.getColor(R.color.title))
            typeface = requireContext().getFontCompat(R.font.mont_bold)
            setPadding(0, 4.dp, 0, 18.dp)
        })
    }

    private fun amountInput(hintText: String) = EditText(requireContext()).apply {
        hint = hintText
        inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        setTextColor(context.getColor(R.color.title))
        setHintTextColor(context.getColor(R.color.subtitle))
        setBackgroundResource(R.drawable.phone_password_input_background)
        setPadding(16.dp)
    }

    private fun actionButton(label: String) = TextView(requireContext()).apply {
        text = label
        gravity = android.view.Gravity.CENTER
        textSize = 18f
        setTextColor(context.getColor(R.color.red))
        typeface = context.getFontCompat(R.font.mont_bold)
        setBackgroundResource(R.drawable.accept_btn_background)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 58.dp).apply {
            topMargin = 22.dp
            bottomMargin = 16.dp
        }
    }

    private fun weightedParams(endMargin: Int) = LinearLayout.LayoutParams(0, 64.dp, 1f).apply {
        marginEnd = endMargin.dp
    }

    private fun EditText.number(): Double? = text.toString().replace(',', '.').toDoubleOrNull()
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    private fun showKeyboard(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(150)
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateFilterLabels() {
        val days = TimeUnit.MILLISECONDS.toDays((model.getToTime() - model.getFromTime()).coerceAtLeast(0L))
        binding.periodFilter.text = when {
            days <= 8 -> "Неделя"
            days <= 32 -> "Месяц"
            days <= 370 -> "Год"
            else -> "Период"
        }
        binding.walletFilter.text = if (model.getCurrency().size == CurrencyEnum.supportedValues.size) {
            "Все кошельки"
        } else {
            "Кошелёк: ${model.getCurrency().size}"
        }
    }

    private fun statsPeriodLabel(): String =
        if (isSameMonth(model.getFromTime(), model.getToTime())) {
            SimpleDateFormat("LLLL", Locale("ru", "RU")).format(Date(model.getFromTime()))
        } else {
            "период"
        }

    private fun isSameMonth(from: Long, to: Long): Boolean {
        val start = Calendar.getInstance().apply { timeInMillis = from }
        val end = Calendar.getInstance().apply { timeInMillis = to }
        return start.get(Calendar.YEAR) == end.get(Calendar.YEAR) &&
            start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
    }

    private fun openAnalysis(mode: String) {
        findNavController().navigate(MainNavGraphDirections.startFinancialAnalysisFragment(mode))
    }

    private fun openSuccessTransfer(transaction: TransactionModel) {
        val user = (model.myData.value as? UiState.Success)?.data
        model.setLastSuccessOperation(TransactionSuccessMapper.toSuccessOperation(requireContext(), transaction, user))
        findParentNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(historyReceiver)
        super.onDestroyView()
    }

    companion object {
        const val MODE_EXPENSES = "expenses"
        const val MODE_INCOME = "income"
        private const val ACTION_HISTORY = "ACTION_HISTORY"
    }
}
