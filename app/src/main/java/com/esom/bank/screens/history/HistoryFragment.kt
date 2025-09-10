package com.esom.bank.screens.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentHistoryBinding
import com.esom.bank.screens.history.adapter.HistoryAdapter
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.wallet.adapter.Transaction
import com.esom.bank.screens.wallet.adapter.TypeOfTransaction
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val model: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        //model.history()

        val adapter = HistoryAdapter(requireContext())
        binding.history.adapter = adapter
        val transactions = listOf(
            Transaction(TypeOfTransaction.SOM, "", 1231),
            Transaction(TypeOfTransaction.DIGITAL, "", -1231),
            Transaction(TypeOfTransaction.USDT, "", 222),
            Transaction(TypeOfTransaction.BITCOIN, "", -1212),
            Transaction(TypeOfTransaction.ETH, "", 9999),
        )
        val history = listOf(
            HistoryAdapter.HistoryItem.HistoryDate("Сегодня", -123456L),
            HistoryAdapter.HistoryItem.History(transactions),
            HistoryAdapter.HistoryItem.HistoryDate("Вчера", -432121L),
            HistoryAdapter.HistoryItem.History(transactions)
        )
        adapter.submitList(history)

        binding.dataPeriodBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChooseDateFragment())
        }
        binding.periodBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChoosePeriodFragment())
        }
        binding.activeBtn.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startChooseActiveFragment())
        }
    }
}