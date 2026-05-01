package com.esom.bank.screens.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentChatBinding
import com.esom.bank.screens.chat.adapter.ChatAdapter
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private lateinit var binding: FragmentChatBinding
    private val model: MainViewModel by activityViewModels()
    private val adapter = ChatAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                bottom = rect.bottom + if (insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0) insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                ).bottom
                else insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        binding.messages.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshMessages()
        }

        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        observeMessages()
        loadMessages()

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }

        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun loadMessages() {
        model.getMessages()
    }

    private fun observeMessages() {
        model.messages.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    binding.swipeRefreshLayout.isRefreshing = true
                }
                is UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.root.showErrorSnackbar(it.message)
                }
                is UiState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.submitSupportMessages(it.data)
                    scrollToLastMessage()
                }
            }
        }

        model.sendMessage.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    setSendingEnabled(false)
                }
                is UiState.Error -> {
                    setSendingEnabled(true)
                    binding.root.showErrorSnackbar(it.message)
                }
                is UiState.Success -> {
                    setSendingEnabled(true)
                    model.getMessages()
                }
            }
        }
    }

    private fun refreshMessages() {
        model.getMessages()
    }

    private fun sendMessage() {
        if (!binding.sendBtn.isEnabled) return

        val message = binding.messageInput.text?.toString()?.trim().orEmpty()
        if (message.isEmpty()) {
            binding.root.showErrorSnackbar(getString(R.string.enter_message))
        } else {
            model.sendMessage(message)
            binding.messageInput.setText("")
        }
    }

    private fun setSendingEnabled(enabled: Boolean) {
        binding.sendBtn.isEnabled = enabled
        binding.sendBtn.alpha = if (enabled) 1f else 0.5f
    }

    private fun scrollToLastMessage() {
        binding.messages.post {
            val lastPosition = adapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.messages.scrollToPosition(lastPosition)
            }
        }
    }
}
