package com.esom.bank.screens.appsettings

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.esom.bank.BuildConfig
import com.esom.bank.NavGraphDirections
import com.esom.bank.R
import com.esom.bank.common.utils.views.doOnApplyWindowInsets
import com.esom.bank.common.utils.views.getFontCompat
import com.esom.bank.databinding.FragmentAppSettingsBinding
import com.esom.bank.screens.main.MainFragment.Companion.findParentNavController
import com.esom.bank.screens.main.MainViewModel
import com.esom.bank.screens.appsettings.model.AppSettingsCreatedRow as CreatedRow
import com.esom.bank.screens.appsettings.model.AppSettingsRowSpec as RowSpec
import com.esom.bank.screens.appsettings.model.AppSettingsSearchRow as SearchRow
import com.esom.bank.screens.appsettings.model.AppSettingsSection as Section
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppSettingsFragment : Fragment() {
    private lateinit var binding: FragmentAppSettingsBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.doOnApplyWindowInsets { root, insets, rect ->
            root.updatePadding(
                bottom = rect.bottom +
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }
        binding.header.doOnApplyWindowInsets { header, insets, rect ->
            header.updatePadding(top = rect.top + insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        binding.backBtn.setOnClickListener { findNavController().navigateUp() }
        binding.version.text = getString(R.string.version_title, BuildConfig.VERSION_NAME)

        val sections = createContent()
        observeBalances(sections)
        setupSearch(sections)
    }

    private fun createContent(): List<Section> = listOf(
        addSection("Внешний вид", listOf(
            RowSpec("Тема", themeName(), R.drawable.ic_settings_gear, "тема внешний вид системная светлая темная") {
                cycleTheme()
            }
        )),

        addSection("Основные экраны", listOf(
            RowSpec("Скрывать суммы", "Скрывать балансы и суммы операций", R.drawable.ic_eye_closed,
                "скрывать суммы баланс глаз", hasSwitch = true) {},
            RowSpec("Платежи", "Переводы и конвертация", R.drawable.ic_bottom_payments,
                "платежи переводы конвертация") {
                findNavController().navigate(R.id.startActionsFragment)
            }
        )),

        addSection("Сервисы", listOf(
            RowSpec("Уведомления", "Сообщения и важные события", R.drawable.notification_icon,
                "уведомления сообщения") {
                findNavController().navigate(R.id.startNotificationFragment)
            },
            RowSpec("Безопасность", "PIN-код, биометрия и защита", R.drawable.ic_security,
                "безопасность пин биометрия защита") {
                findNavController().navigate(R.id.startSecurityFragment)
            },
            RowSpec("Техподдержка", "Чат со службой поддержки", R.drawable.ic_bottom_support,
                "техподдержка поддержка чат") {
                findNavController().navigate(R.id.startChatFragment)
            }
        )),

        addSection(null, listOf(
            RowSpec("О приложении", "Информация о FCB", R.drawable.ic_info_outline,
                "о приложении информация версия") { showAbout() },
            RowSpec("Выйти", "Завершить текущую сессию", R.drawable.ic_logout_outline,
                "выйти выход сессия", destructive = true) { confirmLogout() }
        ))
    )

    private fun addSection(
        title: String?,
        specs: List<RowSpec>
    ): Section {
        val density = resources.displayMetrics.density
        val card = MaterialCardView(requireContext()).apply {
            radius = 24 * density
            cardElevation = 0f
            setCardBackgroundColor(requireContext().getColor(R.color.card_bg))
            strokeWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (14 * density).toInt() }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * density).toInt(), (18 * density).toInt(),
                (14 * density).toInt(), (10 * density).toInt())
        }
        card.addView(content)
        title?.let {
            content.addView(TextView(requireContext()).apply {
                text = it
                setTextColor(requireContext().getColor(R.color.subtitle))
                textSize = 15f
                typeface = requireContext().getFontCompat(R.font.mont_medium)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            })
        }

        val rows = specs.mapIndexed { index, spec ->
            val row = createRow(spec)
            content.addView(row.view)
            if (index != specs.lastIndex) content.addView(View(requireContext()).apply {
                setBackgroundColor(requireContext().getColor(R.color.lock_pattern_regular))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1.coerceAtLeast((density).toInt())
                ).apply { marginStart = (58 * density).toInt() }
            })
            SearchRow(row.view, spec.keywords.lowercase(), row.switch)
        }
        binding.contentContainer.addView(card)
        return Section(card, rows)
    }

    private fun createRow(spec: RowSpec): CreatedRow {
        val density = resources.displayMetrics.density
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (72 * density).toInt()
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { spec.action() }
        }
        row.addView(ImageView(requireContext()).apply {
            setImageResource(spec.icon)
            setColorFilter(requireContext().getColor(if (spec.destructive) R.color.red else R.color.title))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams((38 * density).toInt(), (38 * density).toInt())
        })
        val subtitleView = TextView(requireContext()).apply {
            text = spec.subtitle
            setTextColor(requireContext().getColor(R.color.subtitle))
            textSize = 11f
            typeface = requireContext().getFontCompat(R.font.mont_regular)
        }
        row.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * density).toInt(), 0, (8 * density).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            addView(TextView(context).apply {
                text = spec.title
                setTextColor(requireContext().getColor(if (spec.destructive) R.color.red else R.color.title))
                textSize = 16f
                typeface = requireContext().getFontCompat(R.font.mont_semibold)
            })
            addView(subtitleView)
        })

        val switch = if (spec.hasSwitch) SwitchMaterial(requireContext()).apply {
            isClickable = false
            isFocusable = false
            thumbTintList = resources.getColorStateList(R.color.security_switch_thumb_tint, null)
            trackTintList = resources.getColorStateList(R.color.security_switch_track_tint, null)
            scaleX = .92f
            scaleY = .92f
            row.setOnClickListener { isChecked = !isChecked }
        } else null
        if (switch != null) {
            row.addView(switch)
        } else {
            row.addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.arrow_bottom)
                rotation = -90f
                alpha = .42f
                layoutParams = LinearLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt())
            })
        }
        return CreatedRow(row, switch)
    }

    private fun observeBalances(sections: List<Section>) {
        val balancesSwitch = sections.asSequence()
            .flatMap { it.rows.asSequence() }
            .mapNotNull { it.switch }
            .first()
        model.balancesVisible.observe(viewLifecycleOwner) { visible ->
            if (balancesSwitch.isChecked == visible) {
                balancesSwitch.isChecked = !visible
            }
        }
        balancesSwitch.setOnCheckedChangeListener { _, hide ->
            val currentlyHidden = !(model.balancesVisible.value ?: true)
            if (hide != currentlyHidden) model.toggleBalancesVisibility()
        }
        balancesSwitch.isChecked = !(model.balancesVisible.value ?: true)
    }

    private fun setupSearch(sections: List<Section>) {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(sections, s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun filter(sections: List<Section>, rawQuery: String) {
        val query = rawQuery.trim().lowercase()
        sections.forEach { section ->
            section.rows.forEach { row ->
                row.view.visibility = if (query.isEmpty() || query in row.keywords) View.VISIBLE else View.GONE
            }
            section.card.visibility = if (section.rows.any { it.view.visibility == View.VISIBLE }) {
                View.VISIBLE
            } else View.GONE
        }
    }

    private fun cycleTheme() {
        val values = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )
        val current = values.indexOf(model.getThemeMode()).coerceAtLeast(0)
        val next = (current + 1) % values.size
        model.setThemeMode(values[next])
        AppCompatDelegate.setDefaultNightMode(values[next])
    }

    private fun themeName(): String = when (model.getThemeMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> "Светлая"
        AppCompatDelegate.MODE_NIGHT_YES -> "Тёмная"
        else -> "Системная"
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("FCB")
            .setMessage("Мобильное приложение ФинансКредитБанка\n${getString(R.string.version_title, BuildConfig.VERSION_NAME)}")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выйти из приложения?")
            .setMessage("Для следующего входа потребуется авторизация.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Выйти") { _, _ ->
                model.clearAllDataAndNavigate()
                findParentNavController().navigate(NavGraphDirections.startAuthFragment())
            }
            .show()
    }

}
