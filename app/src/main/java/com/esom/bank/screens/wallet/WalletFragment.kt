package com.esom.bank.screens.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.MainNavGraphDirections
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.history.model.TransactionSuccessMapper
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.adapter.HomeWalletAdapter
import com.esom.bank.screens.wallet.adapter.HomeTransactionAdapter
import com.esom.bank.screens.wallet.adapter.HomeExchangeRateAdapter
import com.esom.bank.screens.wallet.model.toHomeTransactionItems
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: WalletUiStateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiModel.restoreHistoryExpanded(model.isWalletHistoryExpanded())

        binding.header.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        val cardAdapter = setupCards()
        val transactionAdapter = setupHistory()
        val exchangeRateAdapter = setupExchangeRates()
        setupClicks()
        observeData(cardAdapter, transactionAdapter, exchangeRateAdapter)
        renderBalanceVisibility(cardAdapter)
        renderHistoryVisibility()

        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
        refresh()
    }

    private fun setupCards(): HomeWalletAdapter {
        val cardAdapter = HomeWalletAdapter(
            context = requireContext(),
            phoneProvider = { (model.myData.value as? UiState.Success)?.data?.phone },
            balancesVisibleProvider = { uiModel.uiState.value.balancesVisible },
            onWalletClick = { wallet ->
                findNavController().navigate(
                    MainNavGraphDirections.startWalletDetailFragment(wallet.currency.name)
                )
            }
        )
        binding.pager.adapter = cardAdapter
        binding.pager.clipToPadding = false
        binding.pager.clipChildren = false
        binding.pager.setPadding(resources.getDimensionPixelSize(R.dimen._18dp), 0,
            resources.getDimensionPixelSize(R.dimen._18dp), 0)
        return cardAdapter
    }

    private fun setupHistory(): HomeTransactionAdapter {
        val transactionAdapter = HomeTransactionAdapter(
            context = requireContext(),
            balancesVisibleProvider = { uiModel.uiState.value.balancesVisible },
            onTransactionClick = { item ->
            val user = (model.myData.value as? UiState.Success)?.data
            model.setLastSuccessOperation(
                TransactionSuccessMapper.toSuccessOperation(requireContext(), item.transaction, user)
            )
            findParentNavController().navigate(NavGraphDirections.startSuccessTransferFragment())
            }
        )
        binding.transactions.adapter = transactionAdapter
        return transactionAdapter
    }

    private fun setupExchangeRates(): HomeExchangeRateAdapter {
        val exchangeRateAdapter = HomeExchangeRateAdapter(requireContext())
        binding.exchangeRates.apply {
            adapter = exchangeRateAdapter
            clipChildren = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        return exchangeRateAdapter
    }

    private fun setupClicks() = with(binding) {
        profileBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startSettingsFragment())
        }
        notificationBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startNotificationFragment())
        }
        qrBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startQrFragment())
        }
        securityBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startSecurityFragment())
        }
        walletsHeader.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startWalletsFragment())
        }
        eyeBtn.setOnClickListener {
            model.toggleBalancesVisibility()
        }
        historyHeader.setOnClickListener {
            uiModel.toggleHistory()
            model.setWalletHistoryExpanded(uiModel.uiState.value.historyExpanded)
            renderHistoryVisibility()
        }
        allHistoryBtn.setOnClickListener {
            findNavController().navigate(MainNavGraphDirections.startHistoryFragment())
        }
    }

    private fun observeData(
        cardAdapter: HomeWalletAdapter,
        transactionAdapter: HomeTransactionAdapter,
        exchangeRateAdapter: HomeExchangeRateAdapter
    ) {
        model.balancesVisible.observe(viewLifecycleOwner) { visible ->
            uiModel.setBalancesVisible(visible)
            renderBalanceVisibility(cardAdapter)
            transactionAdapter.refreshBalanceVisibility()
        }

        model.hasUnreadNotifications.observe(viewLifecycleOwner) { hasUnread ->
            binding.notificationIcon.setImageResource(
                if (hasUnread) R.drawable.notification_icon_unread else R.drawable.notification_icon
            )
        }

        model.notifications.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Error) binding.notificationIcon.setImageResource(R.drawable.notification_icon)
        }

        model.myData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(state.message)
                }
                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    val wallets = state.data.wallets
                        .filter { it.currency in CurrencyEnum.supportedValues }
                        .sortedBy { currencyOrder(it.currency) }
                    uiModel.setWallets(wallets)
                    cardAdapter.submitList(wallets)
                    exchangeRateAdapter.submitList(wallets)
                    cardAdapter.refreshBalanceVisibility()
                    binding.emptyWallets.visibility = if (wallets.isEmpty()) View.VISIBLE else View.GONE
                    binding.exchangeRatesCard.visibility = if (wallets.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }

        model.history.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(state.message)
                }
                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    val items = state.data.filterNotNull().toHomeTransactionItems(4)
                    transactionAdapter.submitList(items)
                    binding.emptyHistory.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    renderHistoryVisibility()
                }
            }
        }
    }

    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        model.updateUserData()
        model.getSettings()
        model.latestTransactions()
        model.loadNotifications()
    }

    private fun renderBalanceVisibility(cardAdapter: HomeWalletAdapter) {
        cardAdapter.refreshBalanceVisibility()
        binding.eyeBtn.setImageResource(
            if (uiModel.uiState.value.balancesVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
        )
        binding.eyeBtn.contentDescription = getString(
            if (uiModel.uiState.value.balancesVisible) R.string.hide_balances else R.string.show_balances
        )
    }

    private fun renderHistoryVisibility() {
        binding.historyContent.visibility = if (uiModel.uiState.value.historyExpanded) View.VISIBLE else View.GONE
        binding.historyArrow.rotation = if (uiModel.uiState.value.historyExpanded) 180f else 0f
    }

    private fun currencyOrder(currency: CurrencyEnum): Int = when (currency) {
        CurrencyEnum.SOM -> 0
        CurrencyEnum.ESOM -> 1
        CurrencyEnum.USDT_TRC20 -> 2
    }

}
