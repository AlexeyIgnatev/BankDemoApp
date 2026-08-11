package com.esom.bank.screens.wallets

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.esom.bank.common.utils.formatBalanceNew
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.getFontCompat
import com.esom.bank.databinding.FragmentWalletsBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.esom.bank.screens.main.model.WalletModel
import com.esom.bank.screens.wallets.model.WalletsService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletsFragment : Fragment() {
    private lateinit var binding: FragmentWalletsBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: WalletsUiStateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        binding = FragmentWalletsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.doOnApplyWindowInsets { insetView, insets, rect ->
            insetView.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        val adapter = WalletListAdapter(
            requireContext(),
            phoneProvider = { (model.myData.value as? UiState.Success)?.data?.phone },
            balancesVisibleProvider = { uiModel.uiState.value.balancesVisible },
            onWalletClick = { wallet ->
                findNavController().navigate(
                    MainNavGraphDirections.startWalletDetailFragment(wallet.currency.name)
                )
            }
        )
        binding.wallets.adapter = adapter
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }
        binding.eyeBtn.setOnClickListener {
            model.toggleBalancesVisibility()
        }
        createServices()
        model.balancesVisible.observe(viewLifecycleOwner) { visible ->
            uiModel.setBalancesVisible(visible)
            renderEye(adapter)
        }
        model.myData.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                val wallets = state.data.wallets.sortedBy { currencyOrder(it.currency) }
                uiModel.setWallets(wallets, ::balanceInSom)
                adapter.submitList(wallets)
                adapter.refreshBalanceVisibility()
                binding.emptyWallets.visibility = if (wallets.isEmpty()) View.VISIBLE else View.GONE
                renderTotalBalance()
            }
        }
        renderEye(adapter)
        if (model.myData.value !is UiState.Success) model.updateUserData()
    }

    private fun renderEye(adapter: WalletListAdapter) {
        adapter.refreshBalanceVisibility()
        binding.eyeBtn.setImageResource(if (uiModel.uiState.value.balancesVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed)
        binding.eyeBtn.setColorFilter(requireContext().getColor(R.color.title))
        renderTotalBalance()
    }

    private fun renderTotalBalance() {
        binding.totalBalance.setBalance(
            uiModel.uiState.value.totalBalanceInSom.formatBalanceNew(),
            uiModel.uiState.value.balancesVisible
        )
        binding.totalSomSign.animate()
            .alpha(if (uiModel.uiState.value.balancesVisible) 1f else 0f)
            .setDuration(220L)
            .start()
    }

    private fun balanceInSom(wallet: WalletModel): Double {
        if (wallet.currency == CurrencyEnum.SOM) return wallet.balance
        val rate = wallet.sellRate.takeIf { it > 0.0 }
            ?: wallet.buyRate.takeIf { it > 0.0 }
            ?: 1.0
        return wallet.balance * rate
    }

    private fun createServices() {
        val services = listOf(
            WalletsService("Перевод в Сом", "Отправить перевод в KGS", R.drawable.som_icon) {
                transfer(CurrencyEnum.SOM)
            },
            WalletsService("Перевод в Салам", "Отправить цифровые сомы", R.drawable.salam_icon) {
                transfer(CurrencyEnum.ESOM)
            },
            WalletsService("Перевод в USDT", "Отправить USDT", R.drawable.usdt_icon) {
                transfer(CurrencyEnum.USDT_TRC20)
            },
            WalletsService("Из Сом в Салам", "Быстрая конвертация", R.drawable.ic_swap) {
                swap(CurrencyEnum.SOM, CurrencyEnum.ESOM)
            },
            WalletsService("Из Салам в Сом", "Быстрая конвертация", R.drawable.ic_swap) {
                swap(CurrencyEnum.ESOM, CurrencyEnum.SOM)
            },
            WalletsService("Из Салам в USDT", "Быстрая конвертация", R.drawable.ic_swap) {
                swap(CurrencyEnum.ESOM, CurrencyEnum.USDT_TRC20)
            },
            WalletsService("Из USDT в Салам", "Быстрая конвертация", R.drawable.ic_swap) {
                swap(CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM)
            }
        )
        services.forEach { binding.servicesContainer.addView(serviceView(it)) }
    }

    private fun serviceView(service: WalletsService): View {
        val density = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((18 * density).toInt(), 0, (16 * density).toInt(), 0)
            background = GradientDrawable().apply {
                color =
                    android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.card_bg))
                cornerRadius = 22 * density
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (78 * density).toInt()
            ).apply { bottomMargin = (9 * density).toInt() }
            isClickable = true
            isFocusable = true
            setOnClickListener { service.action() }

            addView(ImageView(context).apply {
                setImageResource(service.icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams =
                    LinearLayout.LayoutParams((42 * density).toInt(), (42 * density).toInt())
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((14 * density).toInt(), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = service.title
                    setTextColor(requireContext().getColor(R.color.title))
                    textSize = 16f
                    typeface = requireContext().getFontCompat(R.font.mont_semibold)
                })
                addView(TextView(context).apply {
                    text = service.subtitle
                    setTextColor(requireContext().getColor(R.color.subtitle))
                    textSize = 11f
                    typeface = requireContext().getFontCompat(R.font.mont_regular)
                })
            })
            addView(ImageView(context).apply {
                setImageResource(R.drawable.arrow_bottom)
                setColorFilter(requireContext().getColor(R.color.title))
                rotation = -90f
                alpha = .45f
                layoutParams =
                    LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt())
            })
        }
    }

    private fun transfer(currency: CurrencyEnum) = findParentNavController().navigate(
        NavGraphDirections.startTransferRecipientFragment(currency.name)
    )

    private fun swap(from: CurrencyEnum, to: CurrencyEnum) = findParentNavController().navigate(
        NavGraphDirections.startSwapFragment(from.name, to.name)
    )

    private fun currencyOrder(currency: CurrencyEnum): Int = when (currency) {
        CurrencyEnum.SOM -> 0
        CurrencyEnum.ESOM -> 1
        CurrencyEnum.USDT_TRC20 -> 2
    }

}
