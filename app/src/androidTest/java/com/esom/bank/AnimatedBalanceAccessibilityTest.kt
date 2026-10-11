package com.esom.bank

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import com.esom.bank.activities.MainActivity
import com.esom.bank.common.utils.views.AnimatedBalanceView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedBalanceAccessibilityTest {
    @Test
    fun hiddenBalanceDoesNotExposeRawAmountToAccessibility() {
        val rawAmount = "10002.25 SALAM"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val balance = AnimatedBalanceView(activity)
                balance.setBalance(rawAmount, visible = false)

                assertFalse(balance.text.toString().contains("10002.25"))

                val node = AccessibilityNodeInfo.obtain()
                try {
                    balance.onInitializeAccessibilityNodeInfo(node)
                    assertFalse(node.text?.toString().orEmpty().contains("10002.25"))
                    assertFalse(node.contentDescription?.toString().orEmpty().contains("10002.25"))
                    assertEquals("Баланс скрыт", node.contentDescription.toString())
                } finally {
                    node.recycle()
                }

                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
                try {
                    balance.onPopulateAccessibilityEvent(event)
                    assertFalse(event.text.joinToString().contains("10002.25"))
                    assertTrue(event.text.contains("Баланс скрыт"))
                } finally {
                    event.recycle()
                }

                balance.setBalance(rawAmount, visible = true)
                assertTrue(balance.text.toString().contains("10002.25"))
            }
        }
    }
}
