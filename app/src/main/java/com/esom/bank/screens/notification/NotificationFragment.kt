package com.esom.bank.screens.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentNotificationBinding
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.notification.adapter.NotificationAdapter
import com.esom.bank.screens.notification.model.Notification
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding: FragmentNotificationBinding
        get() = _binding ?: error("Binding accessed outside of the view lifecycle")
    private val model: MainViewModel by activityViewModels()
    private val adapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backLayout.doOnApplyWindowInsets { insetView, insets, _ ->
            insetView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = resources.getDimensionPixelSize(R.dimen._6dp) +
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            }
            insets
        }
        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            model.loadNotifications()
        }
        model.loadNotifications()
        binding.notifications.adapter = adapter
        model.notifications.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    binding.swipeRefreshLayout.isRefreshing = true
                    binding.emptyState.isVisible = false
                }
                is UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.emptyState.isVisible = false
                    binding.root.showErrorSnackbar(it.message)
                }
                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    val sortedList = it.data.sortedByDescending { notification -> notification.createdAt }
                    adapter.submitNotifications(sortedList)
                    binding.emptyState.isVisible = sortedList.isEmpty()
                    model.markNotificationsSeen(it.data)
                }
                null -> Unit
            }
        }
    }

    override fun onDestroyView() {
        _binding?.notifications?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
