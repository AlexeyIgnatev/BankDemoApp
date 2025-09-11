package com.esom.bank.screens.history.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
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
        binding.chooseBtn.setOnClickListener {
            val currencies: MutableList<CurrencyEnum> = mutableListOf()
            if (binding.usdtCheck.isChecked) currencies.add(CurrencyEnum.USDT_TRC20)
            if (binding.bitcoinCheck.isChecked) currencies.add(CurrencyEnum.BTC)
            if (binding.ethCheck.isChecked) currencies.add(CurrencyEnum.ETH)
            if (binding.fiatCheck.isChecked) currencies.add(CurrencyEnum.SOM)
            if (binding.digitalCheck.isChecked) currencies.add(CurrencyEnum.ESOM)
            model.setCurrency(currencies)
            dismiss()
        }

    }
}