package com.esom.bank.screens.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentChatBinding
import com.esom.bank.screens.chat.adapter.ChatAdapter
import com.esom.bank.screens.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private lateinit var binding: FragmentChatBinding
    private val model: MainViewModel by activityViewModels()
    private val uiModel: ChatUiStateViewModel by viewModels()
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
        binding.swipeRefreshLayout.doOnApplyWindowInsets { _, insets, rect ->
            uiModel.setInitialTopPadding(rect.top)
            applyKeyboardInsets(insets)
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.swipeRefreshLayout,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    applyKeyboardInsets(insets)
                    return insets
                }
            }
        )

        binding.messages.adapter = adapter
        binding.messages.itemAnimator = null

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshMessages()
        }

        observeMessages()
        loadMessages()
        startMessagesPolling()

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
                    val shouldScrollToBottom = uiModel.uiState.value.lastMessageCount == 0 || it.data.size > uiModel.uiState.value.lastMessageCount
                    uiModel.setMessageCount(it.data.size)
                    adapter.submitSupportMessages(it.data) {
                        if (shouldScrollToBottom) scrollToLastMessage()
                    }
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
                }
            }
        }
    }

    private fun refreshMessages() {
        binding.swipeRefreshLayout.isRefreshing = true
        model.getMessages(showLoading = false)
    }

    private fun startMessagesPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(5000)
                    model.getMessages(showLoading = false)
                }
            }
        }
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

    private fun applyKeyboardInsets(insets: WindowInsetsCompat) {
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        binding.swipeRefreshLayout.updatePadding(
            top = uiModel.uiState.value.initialTopPadding + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
            bottom = if (imeVisible) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        )
    }
}
