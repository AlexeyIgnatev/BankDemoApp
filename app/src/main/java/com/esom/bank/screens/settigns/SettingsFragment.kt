package com.esom.bank.screens.settigns

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.getFontCompat
import com.esom.bank.common.utils.views.showErrorSnackbar
import com.esom.bank.databinding.FragmentSettingsBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.settigns.model.SettingsService
import com.esom.bank.screens.main.enums.CurrencyEnum
import dagger.hilt.android.AndroidEntryPoint
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.header.doOnApplyWindowInsets { header, insets, rect ->
            header.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            val headerHeight = resources.getDimensionPixelSize(R.dimen._68dp) +
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            header.layoutParams.height = headerHeight
            binding.headerBackground.post {
                binding.headerBackground.clipBounds = Rect(
                    0,
                    0,
                    binding.headerBackground.width,
                    headerHeight
                )
                binding.headerBackground.visibility = View.VISIBLE
            }
            insets
        }
        binding.backBtn.setOnClickListener { findNavController().navigateUp() }
        binding.settingsBtn.setOnClickListener { findNavController().navigate(R.id.startAppSettingsFragment) }
        binding.callBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$PERSONAL_MANAGER_PHONE")))
        }
        setupCollapsingProfile()
        createServices()
        observeUser()
    }

    private fun observeUser() {
        model.myData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> Unit
                is UiState.Error -> binding.root.showErrorSnackbar(state.message)
                is UiState.Success -> {
                    val fullName = listOf(
                        state.data.lastName,
                        state.data.firstName,
                        state.data.middleName.orEmpty()
                    ).filter(String::isNotBlank).joinToString(" ").ifBlank { "Профиль" }
                    binding.fio.text = fullName
                    binding.compactName.text = state.data.firstName.ifBlank { fullName }
                    binding.fullName.text = fullName
                    binding.login.text = model.getLogin().ifBlank { "Не указан" }
                    binding.phone.text = state.data.phone.formatPhone()
                    binding.mail.text = state.data.email.ifBlank { "Не указана" }
                }
                null -> Unit
            }
        }
    }

    private fun setupCollapsingProfile() {
        val transition = resources.getDimensionPixelSize(R.dimen._92dp).toFloat()
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val progress = (scrollY / transition).coerceIn(0f, 1f)
            val headerBackgroundProgress = ((progress - 0.88f) / 0.12f).coerceIn(0f, 1f)
            binding.compactAvatar.alpha = progress
            binding.compactName.alpha = progress
            binding.headerBackground.alpha = headerBackgroundProgress
            binding.heroAvatar.alpha = 1f - progress
            binding.fio.alpha = 1f - progress
            binding.compactAvatar.translationY = (1f - progress) * resources.getDimensionPixelSize(R.dimen._8dp)
            binding.compactName.translationY = binding.compactAvatar.translationY
        }
    }

    private fun createServices() {
        val services = listOf(
            SettingsService("Перевод в Сом", "Отправить перевод в KGS", R.drawable.som_icon) { transfer(CurrencyEnum.SOM) },
            SettingsService("Перевод в Салам", "Отправить цифровые сомы", R.drawable.salam_icon) { transfer(CurrencyEnum.ESOM) },
            SettingsService("Перевод в USDT", "Отправить USDT", R.drawable.usdt_icon) { transfer(CurrencyEnum.USDT_TRC20) },
            SettingsService("Из Сом в Салам", "Быстрая конвертация", R.drawable.ic_swap) { swap(CurrencyEnum.SOM, CurrencyEnum.ESOM) },
            SettingsService("Из Салам в Сом", "Быстрая конвертация", R.drawable.ic_swap) { swap(CurrencyEnum.ESOM, CurrencyEnum.SOM) },
            SettingsService("Из Салам в USDT", "Быстрая конвертация", R.drawable.ic_swap) { swap(CurrencyEnum.ESOM, CurrencyEnum.USDT_TRC20) },
            SettingsService("Из USDT в Салам", "Быстрая конвертация", R.drawable.ic_swap) { swap(CurrencyEnum.USDT_TRC20, CurrencyEnum.ESOM) }
        )
        services.forEach { binding.servicesContainer.addView(serviceView(it)) }
    }

    private fun serviceView(service: SettingsService): View {
        val density = resources.displayMetrics.density
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((18 * density).toInt(), 0, (16 * density).toInt(), 0)
            background = GradientDrawable().apply {
                color = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.card_bg))
                cornerRadius = 22 * density
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (78 * density).toInt()).apply {
                bottomMargin = (9 * density).toInt()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { service.action() }
        }
        row.addView(ImageView(requireContext()).apply {
            setImageResource(service.icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams((42 * density).toInt(), (42 * density).toInt())
        })
        row.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * density).toInt(), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(context).apply {
                text = service.title; setTextColor(requireContext().getColor(R.color.title)); textSize = 16f
                typeface = requireContext().getFontCompat(R.font.mont_semibold)
            })
            addView(TextView(context).apply {
                text = service.subtitle; setTextColor(requireContext().getColor(R.color.subtitle)); textSize = 11f
                typeface = requireContext().getFontCompat(R.font.mont_regular)
            })
        })
        row.addView(ImageView(requireContext()).apply {
            setImageResource(R.drawable.arrow_bottom); rotation = -90f; alpha = .45f
            setColorFilter(requireContext().getColor(R.color.title))
            layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt())
        })
        return row
    }

    private fun transfer(currency: CurrencyEnum) = findParentNavController().navigate(
        NavGraphDirections.startTransferRecipientFragment(currency.name)
    )

    private fun swap(from: CurrencyEnum, to: CurrencyEnum) = findParentNavController().navigate(
        NavGraphDirections.startSwapFragment(from.name, to.name)
    )


    companion object {
        private const val PERSONAL_MANAGER_PHONE = "+996555123456"
        fun String.formatPhone(): String {
            val mask: Mask = MaskImpl(PHONE_NUMBER, true)
            mask.insertFront(replace(" ", "").replace("+996", ""))
            return mask.toString()
        }
        val PHONE_NUMBER: Array<Slot> = arrayOf(
            PredefinedSlots.hardcodedSlot('+'), PredefinedSlots.hardcodedSlot('9'),
            PredefinedSlots.hardcodedSlot('9'), PredefinedSlots.hardcodedSlot('6'),
            PredefinedSlots.hardcodedSlot(' '), PredefinedSlots.hardcodedSlot('('),
            PredefinedSlots.digit(), PredefinedSlots.digit(), PredefinedSlots.digit(),
            PredefinedSlots.hardcodedSlot(')'), PredefinedSlots.hardcodedSlot(' '),
            PredefinedSlots.digit(), PredefinedSlots.digit(), PredefinedSlots.digit(),
            PredefinedSlots.hardcodedSlot('-'), PredefinedSlots.digit(),
            PredefinedSlots.digit(), PredefinedSlots.digit()
        )
    }
}
