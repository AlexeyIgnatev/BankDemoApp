package com.esom.bank.screens.walletdetail

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
import androidx.navigation.fragment.navArgs
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletDetailBinding
import com.esom.bank.screens.history.model.TransactionSuccessMapper
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallet.adapter.HomeTransactionAdapter
import com.esom.bank.screens.wallet.model.toHomeTransactionItems
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletDetailFragment : Fragment() {
    private lateinit var binding: FragmentWalletDetailBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: WalletDetailUiStateViewModel by viewModels()
    private val args: WalletDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentWalletDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiModel.setCurrency(CurrencyEnum.fromNameOrNull(args.currency) ?: CurrencyEnum.SOM)
        val currency = uiModel.uiState.value.currency
        binding.header.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        binding.walletArt.setCurrency(currency)
        binding.walletIcon.setCurrency(currency)
        binding.walletTitle.text = currencyTitle(currency)
        binding.screenTitle.text = currencyTitle(currency)
        binding.currencyCode.text = currencyCode(currency)
        val transactionAdapter = setupHistory()
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.eyeBtn.setOnClickListener {
            model.toggleBalancesVisibility()
        }
        model.balancesVisible.observe(viewLifecycleOwner) { visible ->
            uiModel.setBalanceVisible(visible)
            renderWallet()
            transactionAdapter.refreshBalanceVisibility()
        }
        binding.transferBtn.setOnClickListener {
            findParentNavController().navigate(
                NavGraphDirections.startTransferRecipientFragment(currency.name)
            )
        }
        binding.convertBtn.setOnClickListener {
            val (from, to) = when (currency) {
                CurrencyEnum.SOM -> CurrencyEnum.SOM to CurrencyEnum.ESOM
                CurrencyEnum.ESOM -> CurrencyEnum.ESOM to CurrencyEnum.SOM
                CurrencyEnum.USDT_TRC20 -> CurrencyEnum.ESOM to CurrencyEnum.USDT_TRC20
            }
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(from.name, to.name))
        }

        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                uiModel.setWallet(state.data.wallets.firstOrNull { it.currency == currency })
                renderWallet(state.data.phone)
            }
        }
        model.history.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Error -> {
                    binding.emptyHistory.visibility = View.VISIBLE
                    binding.root.showErrorSnackbar(state.message)
                }
                is UiState.Success -> {
                    val transactions = state.data.filterNotNull()
                    val items = transactions
                        .toHomeTransactionItems(transactions.size.coerceAtLeast(HISTORY_LIMIT))
                        .filter { item ->
                            item.transaction.currencyEnum == currency ||
                                item.conversionFrom?.currencyEnum == currency ||
                                item.conversionTo?.currencyEnum == currency
                        }
                        .take(HISTORY_LIMIT)
                    transactionAdapter.submitList(items)
                    binding.emptyHistory.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        renderWallet()
        if (model.myData.value !is UiState.Success) model.updateUserData()
        model.latestTransactions(currency)
    }

    private fun setupHistory(): HomeTransactionAdapter {
        val transactionAdapter = HomeTransactionAdapter(
            context = requireContext(),
            balancesVisibleProvider = { uiModel.uiState.value.balanceVisible },
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

    private fun renderWallet(phone: String? = (model.myData.value as? UiState.Success)?.data?.phone) {
        val current = uiModel.uiState.value.wallet
        val balance = current?.balance?.formatBalanceNew() ?: "0"
        binding.cardBalance.setBalance(balance, uiModel.uiState.value.balanceVisible)
        binding.eyeBtn.setImageResource(if (uiModel.uiState.value.balanceVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed)
        binding.eyeBtn.setColorFilter(requireContext().getColor(R.color.title))
        val account = when (uiModel.uiState.value.currency) {
            CurrencyEnum.SOM -> phone?.takeLast(4)?.let { "Счёт •• $it" }
                ?: "Счёт •• ${current?.address.orEmpty().takeLast(4)}"
            else -> "Кошелёк •• ${current?.address.orEmpty().takeLast(4)}"
        }
        binding.cardAccount.text = account
    }

    private fun currencyTitle(value: CurrencyEnum): String = when (value) {
        CurrencyEnum.SOM -> getString(R.string.som_wallet)
        CurrencyEnum.ESOM -> getString(R.string.digital)
        CurrencyEnum.USDT_TRC20 -> "USDT TRC20"
    }

    private fun currencyCode(value: CurrencyEnum): String = when (value) {
        CurrencyEnum.SOM -> "KGS"
        CurrencyEnum.ESOM -> "SALAM"
        CurrencyEnum.USDT_TRC20 -> "USDT"
    }

    companion object {
        private const val HISTORY_LIMIT = 4
    }

}
