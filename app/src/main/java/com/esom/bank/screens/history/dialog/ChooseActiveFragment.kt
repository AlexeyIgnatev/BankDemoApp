package com.esom.bank.screens.history.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentChooseActiveBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChooseActiveFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentChooseActiveBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChooseActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.usdtBtn.setOnClickListener {
            binding.usdtCheck.isChecked = !binding.usdtCheck.isChecked
        }
        binding.bitcoinBtn.setOnClickListener {
            binding.bitcoinCheck.isChecked = !binding.bitcoinCheck.isChecked
        }
        binding.ethBtn.setOnClickListener {
            binding.ethCheck.isChecked = !binding.ethCheck.isChecked
        }
        binding.somBtn.setOnClickListener {
            binding.fiatCheck.isChecked = !binding.fiatCheck.isChecked
        }
        binding.salamBtn.setOnClickListener {
            binding.digitalCheck.isChecked = !binding.digitalCheck.isChecked
        }

        model.myData.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}
                is UiState.Error -> binding.root.showErrorSnackbar(it.message)
                is UiState.Success -> {
                    binding.usdt.text = it.data.wallets
                        .find { currency -> currency.currency == CurrencyEnum.USDT_TRC20 }?.address?.takeLast(
                            3
                        )
                    binding.bitcoin.text = it.data.wallets
                        .find { currency -> currency.currency == CurrencyEnum.BTC }?.address?.takeLast(
                            3
                        )
                    binding.eth.text = it.data.wallets
                        .find { currency -> currency.currency == CurrencyEnum.ETH }?.address?.takeLast(
                            3
                        )
                    binding.fiat.text = it.data.wallets
                        .find { currency -> currency.currency == CurrencyEnum.SOM }?.address?.takeLast(
                            3
                        )
                    binding.digital.text = it.data.wallets
                        .find { currency -> currency.currency == CurrencyEnum.ESOM }?.address?.takeLast(
                            3
                        )

                }
            }
        }

        binding.chooseBtn.setOnClickListener {
            val currencies: MutableList<CurrencyEnum> = mutableListOf()
            if (binding.usdtCheck.isChecked) currencies.add(CurrencyEnum.USDT_TRC20)
            if (binding.bitcoinCheck.isChecked) currencies.add(CurrencyEnum.BTC)
            if (binding.ethCheck.isChecked) currencies.add(CurrencyEnum.ETH)
            if (binding.fiatCheck.isChecked) currencies.add(CurrencyEnum.SOM)
            if (binding.digitalCheck.isChecked) currencies.add(CurrencyEnum.ESOM)
            model.setCurrency(currencies)
            val intent = Intent("ACTION_HISTORY")
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

            dismiss()

        }
    }

    override fun onResume() {
        super.onResume()
        val currencies = model.getCurrency()
        binding.usdtCheck.isChecked = (currencies.find { it == CurrencyEnum.USDT_TRC20 }
                == CurrencyEnum.USDT_TRC20)
        binding.bitcoinCheck.isChecked = (currencies.find { it == CurrencyEnum.BTC }
                == CurrencyEnum.BTC)
        binding.ethCheck.isChecked = (currencies.find { it == CurrencyEnum.ETH }
                == CurrencyEnum.ETH)
        binding.fiatCheck.isChecked = (currencies.find { it == CurrencyEnum.SOM }
                == CurrencyEnum.SOM)
        binding.digitalCheck.isChecked = (currencies.find { it == CurrencyEnum.ESOM }
                == CurrencyEnum.ESOM)
    }
}