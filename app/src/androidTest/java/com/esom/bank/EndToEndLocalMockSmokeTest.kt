package com.esom.bank

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import com.esom.bank.activities.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI regression smoke flow against the isolated LOCAL_MOCK_INTEGRATIONS backend. */
class EndToEndLocalMockSmokeTest {
    @Test
    fun authenticatedNavigationAndMajorControlsRemainUsable() {
        grantPermission(Manifest.permission.READ_CONTACTS)
        grantPermission(Manifest.permission.CAMERA)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Dismiss Android 14's separate clipboard-preview activity only when it
            // actually retained input focus; never send Back through the app itself.
            var appWindowHasFocus = false
            scenario.onActivity { activity ->
                appWindowHasFocus = activity.window.decorView.hasWindowFocus()
            }
            if (!appWindowHasFocus) pressSystemBack()

            // Support a clean local install, an existing PIN lock, or a retained session.
            val initialDeadline = System.currentTimeMillis() + 20_000
            while (!isDisplayedNow(R.id.login_input) &&
                !isDisplayedNow(R.id.pinc_dig1_btn) &&
                !isDisplayedNow(R.id.nav_home) &&
                System.currentTimeMillis() < initialDeadline
            ) Thread.sleep(200)

            when {
                isDisplayedNow(R.id.login_input) -> {
                    // Credentials belong only to the local mock.
                    onView(withId(R.id.login_input)).perform(replaceText("local.demo"), closeSoftKeyboard())
                    onView(withId(R.id.password_input)).perform(replaceText("LocalDemo123!"), closeSoftKeyboard())
                    onView(withId(R.id.log_in_btn)).perform(click())
                    awaitVisible(R.id.pinc_dig1_btn)

                    // Exercise the PIN backspace control before creating a test-only PIN.
                    onView(withId(R.id.pinc_dig2_btn)).perform(click())
                    onView(withId(R.id.pinc_digback_btn)).perform(click())
                    enterTestPin()
                    enterTestPin()
                }
                isDisplayedNow(R.id.pinc_dig1_btn) -> enterTestPin()
                else -> awaitVisible(R.id.nav_home)
            }
            awaitVisible(R.id.nav_home)
            awaitVisible(R.id.wallets_header)

            // Cycle bottom tabs repeatedly to exercise rapid screen switching and restoration.
            repeat(10) {
                println("QA_TAB_CYCLE_START=$it")
                tapTab(scenario, R.id.nav_payments, withId(R.id.send_card))
                println("QA_TAB_CYCLE_PAYMENTS=$it")
                tapTab(scenario, R.id.nav_qr, withId(R.id.show_tab_btn))
                println("QA_TAB_CYCLE_QR=$it")
                tapTab(scenario, R.id.nav_services, withText("Раздел в разработке"))
                println("QA_TAB_CYCLE_SERVICES=$it")
                tapTab(scenario, R.id.nav_home, withId(R.id.wallets_header))
                println("QA_TAB_CYCLE_HOME=$it")
            }

            // Wallet dashboard controls and in-app destination links.
            // Harmless button-mash: eye toggle is tapped repeatedly and must remain responsive.
            repeat(10) { onView(withId(R.id.eye_btn)).perform(click()) }
            onView(withId(R.id.history_header)).perform(click(), click())
            onView(withId(R.id.wallets_header)).perform(click())
            awaitVisible(R.id.back_btn)
            awaitVisible(R.id.eye_btn)
            onView(withId(R.id.back_btn)).perform(click())
            awaitVisible(R.id.wallets_header)

            onView(withId(R.id.all_history_btn)).perform(click())
            awaitVisible(R.id.type_filter)
            onView(withId(R.id.balance_visibility_btn)).perform(click())
            onView(withId(R.id.type_filter)).perform(scrollTo(), click())
            pressBack()
            onView(withId(R.id.period_filter)).perform(scrollTo(), click())
            pressBack()
            onView(withId(R.id.wallet_filter)).perform(scrollTo(), click())
            pressBack()
            onView(withId(R.id.amount_filter)).perform(scrollTo(), click())
            // The amount sheet focuses its first field; tap Apply instead of a single Back,
            // which would only dismiss the keyboard and leave the sheet open.
            onView(withText("Применить")).perform(click())
            onView(withId(R.id.swipe_refresh_layout)).perform(swipeDown())
            onView(withId(R.id.expenses_card)).perform(click())
            awaitVisible(R.id.expenses_tab)
            onView(withId(R.id.back_btn)).perform(click())
            onView(withId(R.id.income_card)).perform(click())
            awaitVisible(R.id.income_tab)
            onView(withId(R.id.back_btn)).perform(click())
            onView(withId(R.id.back_btn)).perform(click())
            awaitVisible(R.id.wallets_header)

            // Notifications and support/chat entry points; refresh is exercised without sending.
            onView(withId(R.id.notification_btn)).perform(click())
            awaitVisible(R.id.notifications)
            onView(withId(R.id.swipeRefreshLayout)).perform(swipeDown())
            onView(withId(R.id.back_btn)).perform(click())
            onView(withId(R.id.qr_btn)).perform(click())
            awaitVisible(R.id.send_btn)
            onView(withId(R.id.back_btn)).perform(click())

            // QR receive mode and primary-currency selectors.
            tapTab(scenario, R.id.nav_qr, withId(R.id.show_tab_btn))
            onView(withId(R.id.show_tab_btn)).perform(click())
            awaitVisible(R.id.phone_qr_card)
            onView(withId(R.id.phone_primary_btn)).perform(scrollTo(), click())
            onView(withId(R.id.salam_primary_btn)).perform(scrollTo(), click())
            onView(withId(R.id.usdt_primary_btn)).perform(scrollTo(), click())
            navigateToHomeAndRecordFastTap()
            awaitVisible(R.id.wallets_header)
            Thread.sleep(500)
            navigateToPaymentsAndRecordFastTap()

            // Recipient/conversion forms and currency selectors.
            awaitVisible(R.id.send_card)
            // Expand/collapse the quick templates and contacts panels; neither is allowed
            // to block the primary payment cards when its content is empty.
            if (viewRotation(R.id.templates_arrow) <= 90f) {
                onView(withId(R.id.templates_header)).perform(scrollTo(), click())
                awaitRotation(R.id.templates_arrow, expanded = true)
            }
            assertTrue("Expanded templates panel showed neither rows nor empty state",
                isDisplayedNow(R.id.templates_empty) || isDisplayedNow(R.id.templates_container))
            onView(withId(R.id.templates_header)).perform(scrollTo(), click())
            awaitRotation(R.id.templates_arrow, expanded = false)
            assertTrue("Collapsed templates panel still showed content",
                !isDisplayedNow(R.id.templates_empty) && !isDisplayedNow(R.id.templates_container))
            onView(withId(R.id.templates_header)).perform(scrollTo(), click())
            awaitRotation(R.id.templates_arrow, expanded = true)
            assertTrue("Reopened templates panel showed neither rows nor empty state",
                isDisplayedNow(R.id.templates_empty) || isDisplayedNow(R.id.templates_container))

            if (viewRotation(R.id.contacts_arrow) <= 90f) {
                onView(withId(R.id.contacts_header)).perform(scrollTo(), click())
                awaitRotation(R.id.contacts_arrow, expanded = true)
            }
            assertTrue("Expanded contacts panel showed neither rows nor empty state",
                isDisplayedNow(R.id.contacts_empty) || isDisplayedNow(R.id.contacts_list))
            onView(withId(R.id.contacts_header)).perform(scrollTo(), click())
            awaitRotation(R.id.contacts_arrow, expanded = false)
            assertTrue("Collapsed contacts panel still showed content",
                !isDisplayedNow(R.id.contacts_empty) && !isDisplayedNow(R.id.contacts_list))
            onView(withId(R.id.contacts_header)).perform(scrollTo(), click())
            awaitRotation(R.id.contacts_arrow, expanded = true)
            assertTrue("Reopened contacts panel showed neither rows nor empty state",
                isDisplayedNow(R.id.contacts_empty) || isDisplayedNow(R.id.contacts_list))

            onView(withId(R.id.currency_som_chip)).perform(click())
            onView(withId(R.id.currency_salam_chip)).perform(click())
            onView(withId(R.id.currency_usdt_chip)).perform(click())
            onView(withId(R.id.send_card)).perform(click())
            awaitVisible(R.id.continue_btn)
            onView(withId(R.id.back_btn)).perform(click())
            awaitVisible(R.id.send_card)
            onView(withId(R.id.convert_card)).perform(click())
            awaitVisible(R.id.send_btn)
            onView(withId(R.id.sum)).perform(replaceText("50"), closeSoftKeyboard())
            onView(withId(R.id.people_sum_50_layout)).perform(click())
            onView(withId(R.id.currencies_btn)).perform(click())
            awaitVisible(R.id.first_som_btn)
            onView(withId(R.id.first_som_btn)).perform(click())
            // Source was changed to SOM while target remained USDT. Choose ESOM first,
            // then reopen target choices and select USDT (the currently-selected target
            // is intentionally absent from its own alternative list).
            onView(withId(R.id.people_currencies_btn)).perform(click())
            awaitVisible(R.id.people_digital_btn)
            onView(withId(R.id.people_digital_btn)).perform(click())
            onView(withId(R.id.people_currencies_btn)).perform(click())
            awaitVisible(R.id.people_usdt_btn)
            onView(withId(R.id.people_usdt_btn)).perform(click())
            onView(withId(R.id.back_btn)).perform(click())

            // Transfer validation + explicit cancellation must not dispatch a transfer.
            onView(withId(R.id.send_card)).perform(click())
            awaitVisible(R.id.continue_btn)
            onView(withId(R.id.continue_btn)).perform(click())
            awaitVisible(withText("Введите номер телефона или адрес кошелька"), timeoutMs = 5_000)
            assertTrue("Blank recipient unexpectedly opened transfer form", !isDisplayedNow(R.id.sum_input))
            onView(withId(R.id.recipient_input)).perform(replaceText("+996700000002"), closeSoftKeyboard())
            onView(withId(R.id.continue_btn)).perform(click())
            awaitVisible(R.id.send_btn)
            onView(withId(R.id.send_btn)).perform(click())
            awaitVisible(withText("Введите сумму для перевода"), timeoutMs = 5_000)
            assertTrue("Empty transfer amount unexpectedly opened confirmation", !isDisplayedNow(R.id.confirm_btn))

            onView(withId(R.id.sum_input)).perform(replaceText("1"), closeSoftKeyboard())
            onView(withId(R.id.contact)).perform(replaceText("12"), closeSoftKeyboard())
            onView(withId(R.id.send_btn)).perform(click())
            awaitVisible(withText("Введите корректный номер телефона"), timeoutMs = 5_000)
            assertTrue("Invalid phone unexpectedly opened confirmation", !isDisplayedNow(R.id.confirm_btn))

            onView(withId(R.id.contact)).perform(replaceText("+996700000002"), closeSoftKeyboard())
            Thread.sleep(800) // let the local mock recipient lookup finish
            onView(withId(R.id.send_btn)).perform(click())
            awaitVisible(R.id.confirm_btn)
            onView(withText("Подтверждение перевода")).check(matches(isDisplayed()))
            onView(withId(R.id.amount_value)).check(matches(withText(org.hamcrest.Matchers.containsString("1"))))
            onView(withId(R.id.total_debited_value)).check(matches(isDisplayed()))
            onView(withId(R.id.cancel_btn)).perform(click())
            awaitVisible(R.id.send_btn)
            assertTrue("Cancelled transfer confirmation remained open", !isDisplayedNow(R.id.confirm_btn))

            // Confirm one tiny transfer to the second local mock user, then exercise its success actions.
            onView(withId(R.id.send_btn)).perform(click())
            awaitVisible(R.id.confirm_btn)
            onView(withId(R.id.confirm_btn)).perform(click())
            val transferSucceeded = awaitOperationOutcome()
            println("QA_LOCAL_TRANSFER_0700000002=" + if (transferSucceeded) "SUCCESS" else "FAIL_SCREEN")
            if (transferSucceeded) {
                onView(withId(R.id.preview_receipt_btn)).perform(click())
                dismissExternalActivityIfNeeded(scenario)
                awaitVisible(R.id.success_title)
                onView(withId(R.id.share_btn)).perform(click())
                dismissExternalActivityIfNeeded(scenario)
                awaitVisible(R.id.success_title)

                onView(withId(R.id.create_template_btn)).perform(click())
                onView(withHint("Например, Детский сад")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(allOf(withText("Создать шаблон"), isClickable())).inRoot(isDialog()).perform(click())
                onView(withHint("Например, Детский сад")).inRoot(isDialog()).perform(replaceText("QA local recipient"))
                onView(allOf(withText("Создать шаблон"), isClickable())).inRoot(isDialog()).perform(click())
                onView(withId(R.id.repeat_operation_btn)).perform(click())
                awaitVisible(R.id.sum_input)
                assertTrue("Repeat did not restore the transfer amount", viewText(R.id.sum_input).contains("1"))
                assertTrue("Repeat did not restore recipient phone", viewText(R.id.contact).filter(Char::isDigit).endsWith("700000002"))
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.continue_btn)
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.send_card)
            } else {
                onView(withId(R.id.close_btn)).perform(click())
                awaitVisible(R.id.send_btn)
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.continue_btn)
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.send_card)
            }

            // Exercise every supported conversion direction against the local chain mocks.
            val conversionPairs = listOf(
                "SOM" to "ESOM",
                "SOM" to "USDT_TRC20",
                "ESOM" to "SOM",
                "ESOM" to "USDT_TRC20",
                "USDT_TRC20" to "ESOM",
                "USDT_TRC20" to "SOM",
            )
            for ((from, to) in conversionPairs) {
                onView(withId(R.id.convert_card)).perform(click())
                awaitVisible(R.id.send_btn)
                selectConversionPair(from, to)
                repeat(5) { onView(withId(R.id.send_btn)).perform(click()) }
                assertTrue("Empty conversion unexpectedly opened confirmation for $from->$to", !isDisplayedNow(R.id.confirm_btn))
                onView(withId(R.id.sum)).perform(replaceText("1"), closeSoftKeyboard())
                onView(withId(R.id.send_btn)).perform(click())
                awaitVisible(R.id.confirm_btn)
                onView(withText("Подтверждение конвертации")).check(matches(isDisplayed()))
                onView(withId(R.id.amount_value)).check(matches(withText(org.hamcrest.Matchers.containsString("1"))))
                onView(withId(R.id.fee_value)).check(matches(isDisplayed()))
                assertTrue(
                    "Confirmation credit currency does not match $from->$to: ${viewText(R.id.credited_value)}",
                    currencyMatches(viewText(R.id.credited_value), to),
                )
                assertTrue(
                    "Confirmation debit currency does not match $from->$to: ${viewText(R.id.total_debited_value)}",
                    currencyMatches(viewText(R.id.total_debited_value), from),
                )
                onView(withId(R.id.confirm_btn)).perform(click())
                val succeeded = awaitOperationOutcome()
                println("QA_LOCAL_CONVERSION_${from}_TO_${to}=" + if (succeeded) "SUCCESS" else "FAIL_SCREEN")
                if (succeeded) {
                    awaitVisible(R.id.success_title)
                    onView(withId(R.id.back_btn)).perform(click())
                    awaitVisible(R.id.wallets_header)
                } else {
                    onView(withId(R.id.close_btn)).perform(click())
                    awaitVisible(R.id.send_btn)
                    onView(withId(R.id.back_btn)).perform(click())
                    awaitVisible(R.id.send_card)
                }
                navigateToPaymentsAndRecordFastTap()
            }

            // QR scanner controls, flashlight, image picker entry, all receive-copy buttons, and share chooser.
            tapTab(scenario, R.id.nav_qr, withId(R.id.flashlight_btn))
            onView(withId(R.id.flashlight_btn)).perform(click(), click())
            onView(withId(R.id.scan_gallery_btn)).perform(click())
            dismissExternalActivityIfNeeded(scenario)
            awaitVisible(R.id.flashlight_btn)
            onView(withId(R.id.show_tab_btn)).perform(click())
            awaitVisible(R.id.phone_qr_card)
            onView(withId(R.id.phone_qr_copy_btn)).perform(scrollTo(), click())
            onView(withId(R.id.salam_qr_copy_btn)).perform(scrollTo(), click())
            onView(withId(R.id.usdt_qr_copy_btn)).perform(scrollTo(), click())
            onView(withId(R.id.phone_qr_share_btn)).perform(scrollTo(), click())
            dismissExternalActivityIfNeeded(scenario)
            awaitVisible(R.id.phone_qr_card)
            onView(withId(R.id.salam_qr_share_btn)).perform(scrollTo(), click())
            dismissExternalActivityIfNeeded(scenario)
            awaitVisible(R.id.phone_qr_card)
            onView(withId(R.id.usdt_qr_share_btn)).perform(scrollTo(), click())
            dismissExternalActivityIfNeeded(scenario)
            awaitVisible(R.id.phone_qr_card)
            onView(withId(R.id.show_tab_btn)).perform(click())
            awaitVisible(R.id.flashlight_btn)

            // Settings, dynamic rows, security page, and theme recreation regression.
            navigateToHomeAndRecordFastTap()
            awaitVisible(R.id.profile_btn)
            onView(withId(R.id.profile_btn)).perform(click())
            awaitVisible(R.id.settings_btn)
            onView(withId(R.id.settings_btn)).perform(click())
            awaitVisible(R.id.content_container)
            onView(withText("Скрывать суммы")).perform(click())
            onView(allOf(withText("Платежи"), isDescendantOfA(withId(R.id.content_container))))
                .perform(click())
            awaitVisible(R.id.send_card)
            // The actions screen is a nested destination without its own toolbar back button.
            pressBack()
            awaitVisible(R.id.content_container)
            onView(withText("Уведомления")).perform(click())
            awaitVisible(R.id.notifications)
            onView(withId(R.id.back_btn)).perform(click())
            onView(withText("Техподдержка")).perform(click())
            awaitVisible(R.id.message_input)
            onView(withId(R.id.send_btn)).perform(click())
            awaitVisible(withText("Введите сообщение"), timeoutMs = 5_000)
            onView(withId(R.id.back_btn)).perform(click())
            onView(withText("Безопасность")).perform(click())
            awaitVisible(R.id.change_pin_btn)
            onView(withId(R.id.change_pin_btn)).perform(click())
            awaitVisible(R.id.pin_mode_btn)
            onView(withId(R.id.pattern_mode_btn)).perform(click())
            onView(withId(R.id.pin_mode_btn)).perform(click())
            onView(withId(R.id.back_btn)).perform(click())
            awaitVisible(R.id.change_pin_btn)
            onView(withId(R.id.bio_card)).perform(click())
            awaitVisible(withText("Биометрия недоступна или не настроена на устройстве"), timeoutMs = 5_000)
            onView(withId(R.id.bio_switch)).check(matches(org.hamcrest.Matchers.not(isChecked())))
            onView(withId(R.id.back_btn)).perform(click())
            // The About row is in the final card and can sit partly below the
            // viewport after returning from nested screens. Filter it into view
            // before clicking so this checks the action, not a clipped tap target.
            onView(withId(R.id.search_input)).perform(replaceText("о приложении"), closeSoftKeyboard())
            onView(withText("О приложении")).perform(click())
            awaitVisible(withText("FCB"), timeoutMs = 5_000)
            onView(withText("Понятно")).inRoot(isDialog()).perform(click())
            println("QA_SETTINGS_ABOUT_DIALOG=OPEN_AND_DISMISSED")
            onView(withId(R.id.search_input)).perform(replaceText(""), closeSoftKeyboard())
            onView(allOf(isClickable(), hasDescendant(withText("Тема"))))
                .perform(scrollTo(), click())
            awaitVisible(R.id.content_container)
            // A theme change may recreate MainActivity; it must not reset the app to PIN/auth.
            assertTrue("Theme recreation lost the settings destination", isDisplayedNow(R.id.content_container))

            // Search filters rows live; canceling logout must preserve the authenticated screen.
            onView(withId(R.id.search_input)).perform(replaceText("поддерж"))
            onView(withText("Техподдержка")).check(matches(isDisplayed()))
            onView(withText("Безопасность")).check(matches(org.hamcrest.Matchers.not(isDisplayed())))
            onView(withId(R.id.search_input)).perform(replaceText(""))
            onView(allOf(isClickable(), hasDescendant(withText("Выйти"))))
                .perform(scrollTo(), click())
            awaitVisible(withText("Выйти из приложения?"), timeoutMs = 5_000)
            onView(withText("Отмена")).perform(click())
            println("QA_SETTINGS_LOGOUT_CANCEL=PASS")
            onView(withId(R.id.content_container)).check(matches(isDisplayed()))

            // Exercise all dynamic quick actions on the profile settings screen.
            onView(withId(R.id.back_btn)).perform(click())
            awaitVisible(R.id.settings_btn)
            val quickTransfers = listOf("Перевод в Сом", "Перевод в Салам", "Перевод в USDT")
            for (label in quickTransfers) {
                onView(withText(label)).perform(scrollTo(), click())
                awaitVisible(R.id.recipient_input)
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.settings_btn)
            }
            val quickConversions = listOf(
                Triple("Из Сом в Салам", "SOM", "ESOM"),
                Triple("Из Салам в Сом", "ESOM", "SOM"),
                Triple("Из Салам в USDT", "ESOM", "USDT_TRC20"),
                Triple("Из USDT в Салам", "USDT_TRC20", "ESOM"),
            )
            for ((label, from, to) in quickConversions) {
                onView(withText(label)).perform(scrollTo(), click())
                awaitVisible(R.id.send_btn)
                assertTrue("Wrong source currency for $label: ${viewText(R.id.currency_title)}",
                    currencyMatches(viewText(R.id.currency_title), from))
                assertTrue("Wrong target currency for $label: ${viewText(R.id.people_title)}",
                    currencyMatches(viewText(R.id.people_title), to))
                onView(withId(R.id.back_btn)).perform(click())
                awaitVisible(R.id.settings_btn)
            }
            onView(withId(R.id.call_btn)).perform(scrollTo(), click())
            dismissExternalActivityIfNeeded(scenario)
            println("QA_PROFILE_CALL_ACTION=DIALER_OPENED_WITHOUT_PLACING_CALL")
        }
    }

    private fun enterTestPin() {
        listOf(R.id.pinc_dig2_btn, R.id.pinc_dig4_btn, R.id.pinc_dig6_btn, R.id.pinc_dig8_btn)
            .forEach { onView(withId(it)).perform(click()) }
    }

    private fun selectConversionPair(from: String, to: String) {
        val fromButton = when (from) {
            "SOM" -> R.id.first_som_btn
            "ESOM" -> R.id.first_digital_btn
            "USDT_TRC20" -> R.id.first_usdt_btn
            else -> error("Unsupported test source $from")
        }
        val toButton = when (to) {
            "SOM" -> R.id.people_som_btn
            "ESOM" -> R.id.people_digital_btn
            "USDT_TRC20" -> R.id.people_usdt_btn
            else -> error("Unsupported test target $to")
        }

        if (!currencyMatches(viewText(R.id.currency_title), from)) {
            onView(withId(R.id.currencies_btn)).perform(click())
            awaitVisible(fromButton)
            onView(withId(fromButton)).perform(click())
        }

        if (!currencyMatches(viewText(R.id.people_title), to)) {
            onView(withId(R.id.people_currencies_btn)).perform(click())
            awaitVisible(toButton)
            onView(withId(toButton)).perform(click())
        }
        println("QA_CONVERSION_PAIR_SELECTED=$from->$to")
    }

    private fun currencyMatches(text: String, currency: String): Boolean {
        val normalized = text.lowercase()
        return when (currency) {
            "SOM" -> "сом" in normalized
            "ESOM" -> "салам" in normalized || "esom" in normalized
            "USDT_TRC20" -> "usdt" in normalized
            else -> error("Unsupported test currency $currency")
        }
    }

    private fun awaitOperationOutcome(timeoutMs: Long = 60_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isDisplayedNow(R.id.success_title)) return true
            if (isDisplayedNow(R.id.fail_title)) return false
            Thread.sleep(250)
        }
        throw AssertionError("Operation produced neither success nor failure screen within ${timeoutMs}ms")
    }

    private fun viewText(id: Int): String {
        var value = ""
        onView(withId(id)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isDisplayed()
            override fun getDescription(): String = "read displayed text"
            override fun perform(uiController: androidx.test.espresso.UiController, view: android.view.View) {
                value = (view as? android.widget.TextView)?.text?.toString().orEmpty()
            }
        })
        return value
    }

    private fun viewRotation(id: Int): Float {
        var value = Float.NaN
        onView(withId(id)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<android.view.View> = isDisplayed()
            override fun getDescription(): String = "read displayed rotation"
            override fun perform(uiController: androidx.test.espresso.UiController, view: android.view.View) {
                value = view.rotation
            }
        })
        return value
    }

    private fun awaitRotation(id: Int, expanded: Boolean, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if ((viewRotation(id) > 90f) == expanded) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for panel $id expanded=$expanded; rotation=${viewRotation(id)}")
    }

    private fun dismissExternalActivityIfNeeded(scenario: ActivityScenario<MainActivity>) {
        Thread.sleep(900)
        var appWindowHasFocus = false
        scenario.onActivity { activity ->
            appWindowHasFocus = activity.window.decorView.hasWindowFocus()
        }
        println("QA_EXTERNAL_CHOOSER_OPEN=" + !appWindowHasFocus)
        if (!appWindowHasFocus) pressSystemBack()
    }

    private fun grantPermission(permission: String) {
        val command = "pm grant ${InstrumentationRegistry.getInstrumentation().targetContext.packageName} $permission"
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }

    private fun pressSystemBack() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK")
            .close()
        Thread.sleep(400)
    }

    private fun tapTab(
        scenario: ActivityScenario<MainActivity>,
        tabId: Int,
        expectedView: org.hamcrest.Matcher<android.view.View>,
    ) {
        onView(withId(tabId)).perform(click())
        try {
            awaitVisible(expectedView, timeoutMs = 5_000)
            return
        } catch (firstAttempt: Throwable) {
            var appWindowHasFocus = false
            scenario.onActivity { activity ->
                appWindowHasFocus = activity.window.decorView.hasWindowFocus()
            }
            if (!appWindowHasFocus) {
                println("QA_EXTERNAL_SYSTEM_WINDOW_DISMISSED=tab:$tabId")
                pressSystemBack()
            } else {
                println("QA_TAB_FIRST_TAP_IGNORED=tab:$tabId")
            }
            onView(withId(tabId)).perform(click())
            try {
                awaitVisible(expectedView, timeoutMs = 5_000)
                println("QA_TAB_SECOND_TAP_RECOVERED=tab:$tabId")
            } catch (secondAttempt: Throwable) {
                secondAttempt.addSuppressed(firstAttempt)
                throw secondAttempt
            }
        }
    }

    private fun awaitVisible(id: Int, timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(250)
            }
        }
        throw AssertionError("Timed out waiting for view id=$id", lastError)
    }

    private fun awaitVisible(matcher: org.hamcrest.Matcher<android.view.View>, timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(250)
            }
        }
        throw AssertionError("Timed out waiting for text matcher", lastError)
    }

    private fun awaitSelected(id: Int, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(id)).check(matches(isSelected()))
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(100)
            }
        }
        throw AssertionError("Timed out waiting for selected view id=$id", lastError)
    }

    private fun navigateToPaymentsAndRecordFastTap() {
        onView(withId(R.id.nav_payments)).perform(click())
        try {
            awaitSelected(R.id.nav_payments, timeoutMs = 3_000)
            println("QA_FAST_NAV_PAYMENTS=FIRST_TAP_OK")
        } catch (firstTapError: Throwable) {
            println("QA_FAST_NAV_PAYMENTS=FIRST_TAP_IGNORED")
            if (!isDisplayedNow(R.id.wallets_header)) throw firstTapError
            onView(withId(R.id.nav_payments)).perform(click())
            awaitSelected(R.id.nav_payments)
            println("QA_FAST_NAV_PAYMENTS=RECOVERED_ON_SECOND_TAP")
        }
        awaitVisible(R.id.send_card)
    }

    private fun navigateToHomeAndRecordFastTap() {
        onView(withId(R.id.nav_home)).perform(click())
        try {
            awaitSelected(R.id.nav_home, timeoutMs = 3_000)
            println("QA_FAST_NAV_HOME=FIRST_TAP_OK")
        } catch (firstTapError: Throwable) {
            println("QA_FAST_NAV_HOME=FIRST_TAP_IGNORED")
            if (!isDisplayedNow(R.id.show_tab_btn)) throw firstTapError
            onView(withId(R.id.nav_home)).perform(click())
            awaitSelected(R.id.nav_home)
            println("QA_FAST_NAV_HOME=RECOVERED_ON_SECOND_TAP")
        }
        awaitVisible(R.id.wallets_header)
    }

    private fun isDisplayedNow(id: Int): Boolean = try {
        onView(withId(id)).check(matches(isDisplayed()))
        true
    } catch (_: Throwable) {
        false
    }
}
