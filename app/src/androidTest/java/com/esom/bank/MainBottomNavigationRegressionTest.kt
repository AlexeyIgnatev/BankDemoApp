package com.esom.bank

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.esom.bank.activities.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test

class MainBottomNavigationRegressionTest {
    @Test
    fun paymentsTabActuallyOpensPaymentsAndHomeTabReturnsHome() {
        grantPermission(Manifest.permission.READ_CONTACTS)

        ActivityScenario.launch(MainActivity::class.java).use {
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

            awaitDisplayed(R.id.wallets_header)
            onView(withId(R.id.nav_payments)).perform(click())
            awaitDisplayed(R.id.send_card)
            onView(withId(R.id.nav_payments)).check(matches(isSelected()))

            onView(withId(R.id.nav_home)).perform(click())
            awaitDisplayed(R.id.wallets_header)
            onView(withId(R.id.nav_home)).check(matches(isSelected()))
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
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withId(id)).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(200)
            }
        }
        throw AssertionError("Timed out waiting for displayed view id=$id", lastError)
    }
}
