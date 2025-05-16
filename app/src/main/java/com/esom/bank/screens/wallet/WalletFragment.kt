package com.esom.bank.screens.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.format
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

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

        binding.somToEsomBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(0))
        }

        binding.esomToSomBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(1))
        }

        binding.sendEsomCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startTransferFragment())
        }

        binding.receiveEsomCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startReceiveFragment())
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
                    binding.somText.text = "${it.data.balance.somBalance.format(2)} Сом"
                    binding.esomText.text = "${it.data.balance.esomBalance.format(2)} ЕСом"
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
        binding.totalBalanceText.text = "${total.format(2)} Сом"
    }
}