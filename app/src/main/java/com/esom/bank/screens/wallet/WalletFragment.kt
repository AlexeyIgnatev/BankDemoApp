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
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.adapter.Card
import com.esom.bank.screens.wallet.adapter.CardAdapter
import com.esom.bank.screens.wallet.adapter.Currency
import com.esom.bank.screens.wallet.adapter.CurrencyAdapter
import com.esom.bank.screens.wallet.adapter.News
import com.esom.bank.screens.wallet.adapter.NewsAdapter
import com.esom.bank.screens.wallet.adapter.Transaction
import com.esom.bank.screens.wallet.adapter.TransactionAdapter
import com.esom.bank.screens.wallet.adapter.TypeOfCard
import com.esom.bank.screens.wallet.adapter.TypeOfCurrency
import com.esom.bank.screens.wallet.adapter.TypeOfTransaction
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding

    private val model: MainViewModel by activityViewModels()
    private var cards: List<Card> = emptyList()
    private var infiniteList: List<Card> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
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

        var transactionAdapter = TransactionAdapter(requireContext())
        val transactionsSom = listOf(
            Transaction(TypeOfTransaction.SOM, "", 1231),
            Transaction(TypeOfTransaction.SOM, "", -1231),
            Transaction(TypeOfTransaction.SOM, "", 222),
            Transaction(TypeOfTransaction.SOM, "", -1212),
            Transaction(TypeOfTransaction.SOM, "", 9999),
        )
        val transactionsDigit = listOf(
            Transaction(TypeOfTransaction.DIGITAL, "", 1231),
            Transaction(TypeOfTransaction.DIGITAL, "", -1231),
            Transaction(TypeOfTransaction.DIGITAL, "", 222),
            Transaction(TypeOfTransaction.DIGITAL, "", -1212),
            Transaction(TypeOfTransaction.DIGITAL, "", 9999),
        )
        val transactionsUSDT = listOf(
            Transaction(TypeOfTransaction.USDT, "", 1231),
            Transaction(TypeOfTransaction.USDT, "", -1231),
            Transaction(TypeOfTransaction.USDT, "", 222),
            Transaction(TypeOfTransaction.USDT, "", -1212),
            Transaction(TypeOfTransaction.USDT, "", 9999),
        )
        val transactionsBitcoin = listOf(
            Transaction(TypeOfTransaction.BITCOIN, "", 1231),
            Transaction(TypeOfTransaction.BITCOIN, "", -1231),
            Transaction(TypeOfTransaction.BITCOIN, "", 222),
            Transaction(TypeOfTransaction.BITCOIN, "", -1212),
            Transaction(TypeOfTransaction.BITCOIN, "", 9999),
        )
        val transactionsEth = listOf(
            Transaction(TypeOfTransaction.ETH, "", 1231),
            Transaction(TypeOfTransaction.ETH, "", -1231),
            Transaction(TypeOfTransaction.ETH, "", 222),
            Transaction(TypeOfTransaction.ETH, "", -1212),
            Transaction(TypeOfTransaction.ETH, "", 9999),
        )
        binding.transactions.adapter = transactionAdapter
        transactionAdapter.submitList(transactionsSom)

        val adapter = CardAdapter(requireContext(), { currency ->
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(if (currency == CurrencyEnum.ESOM) 1 else 0))
        }, { currency ->
            val address =
                (model.myData.value as? UiState.Success)?.data?.wallets?.find { it.currency == currency }?.address
            Log.e("address", address.toString())
            findParentNavController().navigate(
                NavGraphDirections.startReceiveFragment(
                    address ?: "", currency
                )
            )
        }, {
            findParentNavController().navigate(NavGraphDirections.startTransferFragment(it))
        })
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

                when (realPosition) {
                    0 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(1, false)
                        }, 150)
                        binding.pager.layoutParams.height = resources.getDimensionPixelSize(R.dimen._189dp)
                        binding.pager.requestLayout()
                        binding.infoLayout.layoutParams.height = resources.getDimensionPixelSize(R.dimen._559dp)
                        binding.infoLayout.requestLayout()
                        updateTransactions(transactionsSom, transactionAdapter)
                        Log.e("currency", "SOM")
                    }

                    1 -> {
                        binding.pager.layoutParams.height = resources.getDimensionPixelSize(R.dimen._159dp)
                        binding.pager.requestLayout()
                        binding.infoLayout.layoutParams.height = resources.getDimensionPixelSize(R.dimen._529dp)
                        binding.infoLayout.requestLayout()
                        updateTransactions(transactionsUSDT, transactionAdapter)
                        Log.e("currency", "USDT")
                    }

                    2 -> {
                        binding.pager.layoutParams.height = resources.getDimensionPixelSize(R.dimen._159dp)
                        binding.pager.requestLayout()
                        binding.infoLayout.layoutParams.height = resources.getDimensionPixelSize(R.dimen._529dp)
                        binding.infoLayout.requestLayout()
                        updateTransactions(transactionsBitcoin, transactionAdapter)
                        Log.e("currency", "BITCOIN")
                    }

                    3 -> {
                        binding.pager.layoutParams.height = resources.getDimensionPixelSize(R.dimen._159dp)
                        binding.pager.requestLayout()
                        binding.infoLayout.layoutParams.height = resources.getDimensionPixelSize(R.dimen._529dp)
                        binding.infoLayout.requestLayout()
                        updateTransactions(transactionsEth, transactionAdapter)
                        Log.e("currency", "ETHEREUM")
                    }

                    4 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(cards.size, false)
                        }, 150)
                        binding.pager.layoutParams.height = resources.getDimensionPixelSize(R.dimen._189dp)
                        binding.pager.requestLayout()
                        binding.infoLayout.layoutParams.height = resources.getDimensionPixelSize(R.dimen._559dp)
                        binding.infoLayout.requestLayout()
                        updateTransactions(transactionsDigit, transactionAdapter)
                        Log.e("currency", "DIGIT")
                    }
                }
            }
        })

        binding.notificationBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startNotificationFragment())
        }

        model.updateUserData()
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

    private fun updateCards(wallets: List<WalletModel>) {
        cards = wallets.map { wallet ->
            when (wallet.currency) {
                CurrencyEnum.SOM -> Card(
                    TypeOfCard.CARD, wallet.balance.format(2), "*${
                        (model.myData.value as? UiState.Success)?.data?.phone?.takeLast(3)
                    }"
                )

                CurrencyEnum.ESOM -> Card(
                    TypeOfCard.DIGITAL,
                    wallet.balance.format(2),
                    "*${wallet.address.takeLast(3)}"
                )

                CurrencyEnum.USDT_TRC20 -> Card(
                    TypeOfCard.USDT,
                    wallet.balance.format(2),
                    "*${wallet.address.takeLast(3)}"
                )

                CurrencyEnum.BTC -> Card(
                    TypeOfCard.BITCOIN,
                    wallet.balance.format(2),
                    "*${wallet.address.takeLast(3)}"
                )

                CurrencyEnum.ETH -> Card(
                    TypeOfCard.ETH,
                    wallet.balance.format(2),
                    "*${wallet.address.takeLast(3)}"
                )
            }
        }

        infiniteList = mutableListOf<Card>().apply {
            add(cards.last())
            addAll(cards)
            add(cards.first())
        }

        (binding.pager.adapter as? CardAdapter)?.submitList(infiniteList)
        binding.pager.setCurrentItem(1, false)
    }

    private fun updateCurrencies(wallets: List<WalletModel>) {
        val currencies = wallets.map { wallet ->
            when (wallet.currency) {
                CurrencyEnum.SOM -> Currency(
                    TypeOfCurrency.FIAT,
                    wallet.buyRate.format(2),
                    wallet.sellRate.format(2)
                )

                CurrencyEnum.ESOM -> Currency(
                    TypeOfCurrency.DIGITAL,
                    wallet.buyRate.format(2),
                    wallet.sellRate.format(2)
                )

                CurrencyEnum.USDT_TRC20 -> Currency(
                    TypeOfCurrency.USDT,
                    wallet.buyRate.format(2),
                    wallet.sellRate.format(2)
                )

                CurrencyEnum.BTC -> Currency(
                    TypeOfCurrency.BITCOIN,
                    wallet.buyRate.format(2),
                    wallet.sellRate.format(2)
                )

                CurrencyEnum.ETH -> Currency(
                    TypeOfCurrency.ETH,
                    wallet.buyRate.format(2),
                    wallet.sellRate.format(2)
                )
            }
        }

        (binding.currencies.adapter as? CurrencyAdapter)?.submitList(currencies)
    }

    private fun updateTotalBalance(wallets: List<WalletModel>) {
        binding.monthWaste.text = "0"

        var totalBalanceInSoms = 0.0

        wallets.forEach { wallet ->
            val balanceInSoms = wallet.balance * wallet.buyRate
            totalBalanceInSoms += balanceInSoms
        }

        binding.totalWaste.text = totalBalanceInSoms.format(2)
    }

    private fun updateTransactions(
        transactions: List<Transaction>,
        transactionAdapter: TransactionAdapter
    ) {
        transactionAdapter.submitList(transactions)
        binding.transactions.adapter = transactionAdapter
    }
}