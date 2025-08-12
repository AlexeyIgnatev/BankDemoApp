package com.esom.bank.screens.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.databinding.FragmentChatBinding
import com.esom.bank.screens.chat.adapter.ChatAdapter
import com.esom.bank.screens.chat.adapter.Message
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private lateinit var binding: FragmentChatBinding
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
                top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            )
            insets
        }

        binding.messageLayout.doOnApplyWindowInsets { view, insets, rect ->
            view.updatePadding(
                bottom = rect.bottom + if(insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                else insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }
        binding.messages.adapter = adapter
        val messages = listOf(
            ChatAdapter.MessageItem.Date("21 декабря"),
            ChatAdapter.MessageItem.SenderMessage(
                Message("Сайт рыбатекст поможет дизайнеру, верстальщику, вебмастеру сгенерировать несколько абзацев более менее осмысленного текста рыбы на русском языке, а начинающему оратору отточить навык публичных выступлений в домашних условиях", "20:21")),
            ChatAdapter.MessageItem.ReceiverMessage(
                Message("Сайт рыбатекст поможет дизайнеру, верстальщику, вебмастеру сгенерировать несколько абзацев более менее осмысленного текста рыбы на русском языке, а начинающему оратору отточить навык публичных выступлений в домашних условиях", "20:21", "Оператор Алия"))
        )
        adapter.submitList(messages)
    }
}