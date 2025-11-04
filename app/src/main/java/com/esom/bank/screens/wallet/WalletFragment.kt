package com.esom.bank.screens.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.esom.bank.MainNavGraphDirections
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.history.enums.TransactionEnum
import com.esom.bank.screens.history.model.TransactionModel
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.adapter.CardAdapter
import com.esom.bank.screens.wallet.adapter.Currency
import com.esom.bank.screens.wallet.adapter.CurrencyAdapter
import com.esom.bank.screens.wallet.adapter.News
import com.esom.bank.screens.wallet.adapter.NewsAdapter
import com.esom.bank.screens.wallet.adapter.TransactionAdapter
import com.esom.bank.screens.wallet.adapter.TypeOfCurrency
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class WalletFragment : Fragment() {

    private lateinit var binding: FragmentWalletBinding
    private val model: MainViewModel by activityViewModels()
    private var cards: List<WalletModel> = emptyList()
    private var infiniteList: List<WalletModel> = emptyList()
    private var currentCurrency: CurrencyEnum = CurrencyEnum.SOM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dataLayout.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        val newsAdapter = NewsAdapter()
        val news = listOf(
            News(R.drawable.new_1),
            News(R.drawable.new_2),
            News(R.drawable.new_3),
            News(R.drawable.new_4),
            News(R.drawable.new_5)
        )
        binding.news.adapter = newsAdapter
        newsAdapter.submitList(news)

        val currencyAdapter = CurrencyAdapter(requireContext())
        binding.currencies.adapter = currencyAdapter

        val transactionAdapter = TransactionAdapter(requireContext())
        binding.transactions.adapter = transactionAdapter

        transactionAdapter.submitList(emptyList())

        model.latestTransactions(CurrencyEnum.SOM)
        model.history.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                }
                is UiState.Error -> {
                    binding.root.showErrorSnackbar(state.message)
                    transactionAdapter.submitList(emptyList())
                }
                is UiState.Success -> {
                    if (isDataForCurrentCurrency(state.data)) {
                        transactionAdapter.submitList(state.data)

                        if (state.data.isEmpty()) {
                            binding.transactionLayout.visibility = View.GONE
                            binding.lastTransTitle.visibility = View.GONE
                            binding.historyBtn.visibility = View.GONE
                        } else {
                            binding.transactionLayout.visibility = View.VISIBLE
                            binding.lastTransTitle.visibility = View.VISIBLE
                            binding.historyBtn.visibility = View.VISIBLE
                        }

                        val calendar = java.util.Calendar.getInstance()
                        val currentYear = calendar.get(java.util.Calendar.YEAR)
                        val currentMonth = calendar.get(java.util.Calendar.MONTH)

                        val calendarStart = java.util.Calendar.getInstance().apply {
                            set(currentYear, currentMonth, 1, 0, 0, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }

                        val fromTimeMonth = calendarStart.timeInMillis

                        val monthFormat = java.text.SimpleDateFormat("LLLL", Locale("ru"))
                        val monthText = monthFormat.format(Date(fromTimeMonth))

                        val monthInGenitive = when (monthText.lowercase(Locale.getDefault())) {
                            "январь" -> "январе"
                            "февраль" -> "феврале"
                            "март" -> "марте"
                            "апрель" -> "апреле"
                            "май" -> "мае"
                            "июнь" -> "июне"
                            "июль" -> "июле"
                            "август" -> "августе"
                            "сентябрь" -> "сентябре"
                            "октябрь" -> "октябре"
                            "ноябрь" -> "ноябре"
                            "декабрь" -> "декабре"
                            else -> monthText
                        }

                        binding.monthWasteTitle.text = "Расходы в $monthInGenitive"
                    }
                }
            }
        }

        val adapter = CardAdapter(
            requireContext(),
            { fromCurrency, toCurrency ->
                findParentNavController().navigate(
                    NavGraphDirections.startSwapFragment(
                        fromCurrency, toCurrency
                    )
                )
            },
            { currency ->
                val address = (model.myData.value as? UiState.Success)
                    ?.data?.wallets?.find { it.currency == currency }?.address
                Log.e("address", address.toString())
                findParentNavController().navigate(
                    NavGraphDirections.startReceiveFragment(
                        address ?: "", currency
                    )
                )
            },
            {
                findParentNavController().navigate(
                    NavGraphDirections.startTransferFragment(it)
                )
            }
        )

        val pageMarginPx = resources.getDimension(R.dimen._3dp).toInt()
        val offsetPx = resources.getDimension(R.dimen._32dp).toInt()

        binding.title.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startSettingsFragment())
        }

        binding.historyBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startHistoryFragment())
        }

        binding.pager.apply {
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 2
            setPadding(offsetPx, 0, offsetPx, 0)

            setPageTransformer { page, position ->
                val offset = position * -(2 * pageMarginPx + offsetPx)
                if (position < -1) {
                    page.translationX = -offset
                    page.alpha = 0.3f
                    page.scaleX = 0.8f
                    page.scaleY = 0.8f
                } else if (position <= 1) {
                    when {
                        position < 0 -> {
                            page.translationX = offset
                            page.alpha = 0.3f + (1 - abs(position)) * 0.7f
                            val scale = 0.8f + (1 - abs(position)) * 0.2f
                            page.scaleX = scale
                            page.scaleY = scale
                        }

                        position > 0 -> {
                            page.translationX = offset
                            page.alpha = 0.3f + (1 - position) * 0.7f
                            val scale = 0.8f + (1 - position) * 0.2f
                            page.scaleX = scale
                            page.scaleY = scale
                        }

                        else -> {
                            page.translationX = 0f
                            page.alpha = 1f
                            page.scaleX = 1f
                            page.scaleY = 1f
                        }
                    }
                } else {
                    page.translationX = offset
                    page.alpha = 0.3f
                    page.scaleX = 0.8f
                    page.scaleY = 0.8f
                }
            }
        }

        binding.pager.adapter = adapter
        binding.pager.setCurrentItem(1, false)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (cards.isEmpty()) return

                val realPosition = when (position) {
                    0 -> cards.size - 1
                    infiniteList.size - 1 -> 0
                    else -> position - 1
                }

                val targetCurrency = when (realPosition) {
                    0 -> CurrencyEnum.SOM
                    1 -> CurrencyEnum.ESOM
                    2 -> CurrencyEnum.BTC
                    3 -> CurrencyEnum.ETH
                    4 -> CurrencyEnum.USDT_TRC20
                    else -> return
                }

                currentCurrency = targetCurrency

                (binding.transactions.adapter as? TransactionAdapter)?.submitList(emptyList())

                model.latestTransactions(targetCurrency)

                when (realPosition) {
                    0 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(1, false)
                        }, 150)
                    }
                    4 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(cards.size, false)
                        }, 150)
                    }
                }
            }
        })

        binding.notificationBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startNotificationFragment())
        }

        model.updateUserData()
        model.getSettings()
        model.myData.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}
                is UiState.Error -> {
                    binding.root.showErrorSnackbar(it.message)
                    if (it.message == getString(R.string.logged_out)) {
                        findParentNavController().navigate(
                            NavGraphDirections.startAuthFragment()
                        )
                    }
                }

                is UiState.Success -> {
                    binding.title.text = "${it.data.firstName} ${it.data.lastName}"
                    updateCards(it.data.wallets)
                    updateCurrencies(it.data.wallets)
                    updateTotalBalance(it.data.wallets)
                }
            }
        }
    }

    private fun isDataForCurrentCurrency(transactions: List<TransactionModel?>): Boolean {
        return transactions.any { it?.currencyEnum == currentCurrency } || transactions.isEmpty()
    }

    private fun updateCards(wallets: List<WalletModel>) {
        cards = wallets
        infiniteList = mutableListOf<WalletModel>().apply {
            add(cards.last())
            addAll(cards)
            add(cards.first())
        }

        (binding.pager.adapter as? CardAdapter)?.submitList(infiniteList)
        binding.pager.setCurrentItem(1, false)
    }

    private fun updateCurrencies(wallets: List<WalletModel>) {
        val sortedWallets = wallets.sortedWith(compareBy {
            when (it.currency) {
                CurrencyEnum.SOM -> 0
                CurrencyEnum.USDT_TRC20 -> 1
                CurrencyEnum.BTC -> 2
                CurrencyEnum.ETH -> 3
                CurrencyEnum.ESOM -> 4
            }
        })

        val currencies = sortedWallets.map { wallet ->
            when (wallet.currency) {
                CurrencyEnum.SOM -> Currency(
                    TypeOfCurrency.FIAT,
                    wallet.buyRate.formatBalanceNew(),
                    wallet.sellRate.formatBalanceNew()
                )

                CurrencyEnum.USDT_TRC20 -> Currency(
                    TypeOfCurrency.USDT,
                    wallet.buyRate.formatBalanceNew(),
                    wallet.sellRate.formatBalanceNew()
                )

                CurrencyEnum.BTC -> Currency(
                    TypeOfCurrency.BITCOIN,
                    wallet.buyRate.formatBalanceNew(),
                    wallet.sellRate.formatBalanceNew()
                )

                CurrencyEnum.ETH -> Currency(
                    TypeOfCurrency.ETH,
                    wallet.buyRate.formatBalanceNew(),
                    wallet.sellRate.formatBalanceNew()
                )

                CurrencyEnum.ESOM -> Currency(
                    TypeOfCurrency.DIGITAL,
                    wallet.buyRate.formatBalanceNew(),
                    wallet.sellRate.formatBalanceNew()
                )
            }
        }

        (binding.currencies.adapter as? CurrencyAdapter)?.submitList(currencies)
    }

    private fun updateTotalBalance(wallets: List<WalletModel>) {
        var totalBalanceInSoms = 0.0
        wallets.forEach { wallet ->
            val balanceInSoms = wallet.balance * wallet.buyRate
            totalBalanceInSoms += balanceInSoms
        }
        binding.totalWaste.text = totalBalanceInSoms.format(2).trimEnd('0')
            .trimEnd('.').ifEmpty { "0" }
    }
}