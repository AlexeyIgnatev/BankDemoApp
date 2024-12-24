package com.esom.bank.screens.actions

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentActionsBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActionsFragment : Fragment() {
    private lateinit var binding: FragmentActionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentActionsBinding.inflate(inflater, container, false)
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

        binding.somToEsomCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(0))
        }

        binding.esomToSomCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startSwapFragment(1))
        }

        binding.sendCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startTransferFragment())
        }

        binding.receiveCard.setOnClickListener {
            findParentNavController().navigate(NavGraphDirections.startReceiveFragment())
        }
    }
}