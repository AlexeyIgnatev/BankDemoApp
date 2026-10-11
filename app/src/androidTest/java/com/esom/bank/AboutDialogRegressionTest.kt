package com.esom.bank

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.esom.bank.activities.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Test

class AboutDialogRegressionTest {
    @Test
    fun aboutRowOpensAndDismissesInformationDialog() {
        grantPermission(Manifest.permission.READ_CONTACTS)
        grantPermission(Manifest.permission.CAMERA)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Android 14 may leave the clipboard preview in front after text entry.
            var appWindowHasFocus = false
            scenario.onActivity { activity ->
                appWindowHasFocus = activity.window.decorView.hasWindowFocus()
            }
            if (!appWindowHasFocus) pressBack()

            val deadline = System.currentTimeMillis() + 20_000
            while (!isDisplayedNow(R.id.login_input) &&
                !isDisplayedNow(R.id.pinc_dig1_btn) &&
                !isDisplayedNow(R.id.nav_home) &&
                System.currentTimeMillis() < deadline
            ) Thread.sleep(200)

            when {
                isDisplayedNow(R.id.login_input) -> {
                    onView(withId(R.id.login_input)).perform(replaceText("local.demo"), closeSoftKeyboard())
                    onView(withId(R.id.password_input)).perform(replaceText("LocalDemo123!"), closeSoftKeyboard())
                    onView(withId(R.id.log_in_btn)).perform(click())
                    awaitDisplayed(R.id.pinc_dig1_btn)
                    enterTestPin()
                    enterTestPin()
                }
                isDisplayedNow(R.id.pinc_dig1_btn) -> {
                    enterTestPin()
                }
            }

            awaitDisplayed(R.id.nav_home)
            awaitDisplayed(R.id.wallets_header)
            awaitDisplayed(R.id.profile_btn)
            onView(withId(R.id.profile_btn)).perform(click())
            awaitDisplayed(R.id.settings_btn)
            onView(withId(R.id.settings_btn)).perform(click())
            awaitDisplayed(R.id.content_container)

            // Reproduce the nested settings routes that preceded the old E2E failure.
            onView(withText("Скрывать суммы")).perform(click())
            onView(allOf(
                withText("Платежи"),
                isDescendantOfA(withId(R.id.content_container))
            )).perform(click())
            awaitDisplayed(R.id.send_card)
            pressBack()
            awaitDisplayed(R.id.content_container)
            onView(withText("Уведомления")).perform(click())
            awaitDisplayed(R.id.notifications)
            onView(withId(R.id.back_btn)).perform(click())
            onView(withText("Безопасность")).perform(click())
            awaitDisplayed(R.id.bio_card)
            onView(withId(R.id.bio_card)).perform(click())
            awaitText("Биометрия недоступна или не настроена на устройстве")
            onView(withId(R.id.back_btn)).perform(click())
            awaitDisplayed(R.id.content_container)

            onView(allOf(isClickable(), hasDescendant(withText("О приложении"))))
                .perform(scrollTo(), click())
            onView(withText("FCB")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("Понятно")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.content_container)).check(matches(isDisplayed()))
        }
    }

    private fun enterTestPin() {
        listOf(R.id.pinc_dig2_btn, R.id.pinc_dig4_btn, R.id.pinc_dig6_btn, R.id.pinc_dig8_btn)
            .forEach { onView(withId(it)).perform(click()) }
    }

    private fun grantPermission(permission: String) {
        val command = "pm grant ${InstrumentationRegistry.getInstrumentation().targetContext.packageName} $permission"
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).close()
    }

    private fun isDisplayedNow(id: Int): Boolean = try {
        onView(withId(id)).check(matches(isDisplayed()))
        true
    } catch (_: Throwable) {
        false
    }

    private fun awaitDisplayed(id: Int, timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (_: Throwable) {
                Thread.sleep(200)
            }
        }
        error("Timed out waiting for displayed view id=$id")
    }

    private fun awaitText(text: String, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withText(text)).check(matches(isDisplayed()))
                return
            } catch (_: Throwable) {
                Thread.sleep(100)
            }
        }
        error("Timed out waiting for text=$text")
    }
}
