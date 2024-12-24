package com.esom.bank.screens.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.esom.bank.NavGraphDirections
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentWalletBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletFragment : Fragment() {
    private lateinit var binding: FragmentWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletBinding.inflate(inflater, container, false)
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
    }
}