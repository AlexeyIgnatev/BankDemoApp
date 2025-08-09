package com.esom.bank.screens.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.wallet.adapter.Card
import com.esom.bank.screens.wallet.adapter.CardAdapter
import com.esom.bank.screens.wallet.adapter.TypeOfCard
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding

    private val model: MainViewModel by activityViewModels()

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
        val adapter = CardAdapter(requireContext())

        val pageMarginPx = resources.getDimension(R.dimen._3dp).toInt()
        val offsetPx = resources.getDimension(R.dimen._32dp).toInt()

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
        val cards = listOf(
            Card(TypeOfCard.CARD, "333 333\u20C0", "*998"),
            Card(TypeOfCard.USDT, "444 444", "*1w6"),
            Card(TypeOfCard.BITCOIN, "111 111", "*w77"),
            Card(TypeOfCard.ETH, "555 555", "*w56"),
            Card(TypeOfCard.DIGITAL, "666 666\u20C0", "*998"),
        )

        val infiniteList = mutableListOf<Card>().apply {
            add(cards.last())
            addAll(cards)
            add(cards.first())
        }

        binding.pager.adapter = adapter
        adapter.submitList(infiniteList)
        binding.pager.setCurrentItem(1, false)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(cards.size, false)
                        }, 150)
                    }
                    infiniteList.size - 1 -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.pager.setCurrentItem(1, false)
                        }, 150)
                    }
                }
            }
        })

//        binding.somToEsomBtn.setOnClickListener {
//            findParentNavController().navigate(NavGraphDirections.startSwapFragment(0))
//        }
//
//        binding.esomToSomBtn.setOnClickListener {
//            findParentNavController().navigate(NavGraphDirections.startSwapFragment(1))
//        }
//
//        binding.sendEsomCard.setOnClickListener {
//            findParentNavController().navigate(NavGraphDirections.startTransferFragment())
//        }
//
//        binding.receiveEsomCard.setOnClickListener {
//            findParentNavController().navigate(NavGraphDirections.startReceiveFragment())
//        }

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
//                    binding.somText.text = "${it.data.balance.somBalance.format(2)} Сом"
//                    binding.esomText.text = "${it.data.balance.esomBalance.format(2)} ЕСом"
                    binding.title.text = "${it.data.firstName} ${it.data.lastName}"
                    updateTotalBalance()
                }
            }
        }
    }

    private fun updateTotalBalance() {
        val fiat = (model.myData.value as? UiState.Success)?.data?.balance?.somBalance ?: 0.0
        val token = (model.myData.value as? UiState.Success)?.data?.balance?.esomBalance ?: 0.0

        val total = fiat + token
        //binding.totalBalanceText.text = "${total.format(2)} Сом"
    }
}