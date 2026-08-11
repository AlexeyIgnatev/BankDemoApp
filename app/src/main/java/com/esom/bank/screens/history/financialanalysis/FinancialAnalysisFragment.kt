package com.esom.bank.screens.history.financialanalysis

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.getFontCompat
import com.esom.bank.databinding.FragmentFinancialAnalysisBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.HistoryFragment
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.history.model.FinancialAnalysisCategory
import com.esom.bank.screens.history.model.isUserTransfer
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class FinancialAnalysisFragment : Fragment() {
    private lateinit var binding: FragmentFinancialAnalysisBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: FinancialAnalysisUiStateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentFinancialAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiModel.setMode(arguments?.getString("mode") ?: HistoryFragment.MODE_EXPENSES)
        binding.root.doOnApplyWindowInsets { root, insets, rect ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(
                top = rect.top + systemBars.top,
                bottom = rect.bottom + systemBars.bottom
            )
            insets
        }
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.expensesTab.setOnClickListener { uiModel.setMode(HistoryFragment.MODE_EXPENSES); render() }
        binding.incomeTab.setOnClickListener { uiModel.setMode(HistoryFragment.MODE_INCOME); render() }
        model.balancesVisible.observe(viewLifecycleOwner) { render() }
        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                uiModel.setWallets(state.data.wallets)
                render()
            }
        }
        model.month.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                uiModel.setTransactions(state.data.filterNotNull())
                render()
            }
        }
        model.monthTransactions()
        render()
    }

    private fun render() {
        val incomeMode = uiModel.uiState.value.mode == HistoryFragment.MODE_INCOME
        binding.expensesTab.setBackgroundResource(
            if (!incomeMode) R.drawable.data_period_background else R.drawable.gray_period_background
        )
        binding.incomeTab.setBackgroundResource(
            if (incomeMode) R.drawable.data_period_background else R.drawable.gray_period_background
        )
        binding.expensesTab.setTextColor(requireContext().getColor(if (!incomeMode) R.color.white else R.color.title))
        binding.incomeTab.setTextColor(requireContext().getColor(if (incomeMode) R.color.white else R.color.title))

        val categories = buildCategories(incomeMode)
        val total = categories.sumOf { it.amount }
        binding.total.setBalance(
            "${if (incomeMode) "+" else ""}${total.formatBalanceNew()} сом",
            model.balancesVisible.value ?: true
        )
        binding.chart.setData(categories.map { it.amount }, monthLabel())
        binding.categoriesContainer.removeAllViews()
        categories.forEachIndexed { index, category -> addCategory(category, index) }
        if (categories.isEmpty()) {
            binding.categoriesContainer.addView(TextView(requireContext()).apply {
                text = "За выбранный период операций нет"
                textSize = 16f
                setTextColor(context.getColor(R.color.subtitle))
                setPadding(8.dp, 24.dp, 8.dp, 24.dp)
            })
        }
    }

    private fun buildCategories(incomeMode: Boolean): List<FinancialAnalysisCategory> {
        val relevant = uiModel.uiState.value.transactions.filter { item ->
            if (incomeMode) {
                item.isIncoming() || item.type == TransactionEnum.CONVERSION &&
                    item.conversionSide == ConversionSide.IN
            } else {
                item.isOutgoing() || item.type == TransactionEnum.CONVERSION &&
                    item.conversionSide != ConversionSide.IN
            }
        }
        return relevant
            .groupBy(::categoryTitle)
            .map { (title, items) -> FinancialAnalysisCategory(title, items.sumOf(::amountInSom), items.size) }
            .sortedByDescending { it.amount }
    }

    private fun categoryTitle(item: TransactionModel): String {
        val currency = when (item.currencyEnum) {
            CurrencyEnum.SOM -> "Сом"
            CurrencyEnum.ESOM -> "Салам"
            CurrencyEnum.USDT_TRC20 -> "USDT"
            null -> "другой валюте"
        }
        return when {
            item.type == TransactionEnum.CONVERSION && item.conversionSide == ConversionSide.IN ->
                "Конвертация в $currency"
            item.type == TransactionEnum.CONVERSION -> "Конвертация из $currency"
            item.isIncoming() && item.isUserTransfer() -> "Получения в $currency"
            item.isOutgoing() && item.isUserTransfer() -> "Переводы в $currency"
            item.isIncoming() -> "Зачисления в $currency"
            else -> "Расходы в $currency"
        }
    }

    private fun addCategory(category: FinancialAnalysisCategory, index: Int) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 14.dp, 0, 14.dp)
        }
        val colors = intArrayOf(R.color.red, R.color.finance_orange, R.color.finance_cyan, R.color.finance_purple)
        val badge = TextView(requireContext()).apply {
            text = (index + 1).toString()
            gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.white))
            textSize = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(colors[index % colors.size]))
            }
            layoutParams = LinearLayout.LayoutParams(46.dp, 46.dp).apply { marginEnd = 14.dp }
        }
        val labels = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply {
                text = category.title
                textSize = 17f
                setTextColor(context.getColor(R.color.title))
                typeface = requireContext().getFontCompat(R.font.mont_semibold)
            })
            addView(TextView(context).apply {
                text = resources.getQuantityString(R.plurals.history_operations, category.count, category.count)
                textSize = 14f
                setTextColor(context.getColor(R.color.subtitle))
            })
        }
        val amount = TextView(requireContext()).apply {
            text = "${category.amount.formatBalanceNew()} сом"
            textSize = 17f
            setTextColor(context.getColor(R.color.title))
            typeface = requireContext().getFontCompat(R.font.mont_semibold)
        }
        row.addView(badge)
        row.addView(labels)
        row.addView(amount)
        binding.categoriesContainer.addView(row)
    }

    private fun amountInSom(item: TransactionModel): Double {
        val rate = when (item.currencyEnum) {
            CurrencyEnum.SOM, null -> 1.0
            else -> uiModel.uiState.value.wallets.firstOrNull { it.currency == item.currencyEnum }
                ?.let { if (it.sellRate > 0.0) it.sellRate else it.buyRate }
                ?.takeIf { it > 0.0 } ?: 1.0
        }
        return (item.amount ?: 0.0) * rate
    }

    private fun TransactionModel.isIncoming() =
        type == TransactionEnum.INCOME || type == TransactionEnum.INFLOW ||
            (!senderFullName.isNullOrBlank() && recipientFullName.isNullOrBlank())

    private fun TransactionModel.isOutgoing() =
        type != TransactionEnum.CONVERSION && !isIncoming() &&
            (type == TransactionEnum.EXPENSE || isUserTransfer())

    private fun monthLabel() = SimpleDateFormat("LLLL", Locale("ru", "RU"))
        .format(Date(model.getFromTime())).replaceFirstChar { it.uppercase(Locale("ru", "RU")) }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

}
