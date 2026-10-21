package com.esom.bank

import android.content.res.Configuration
import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class QrModeButtonLayoutTest {
    @Test
    fun modeLabelsFitNarrowScreensAndLargeFonts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (widthDp in listOf(320, 360, 411)) {
                for (scale in listOf(1f, 1.3f, 2f)) {
                    val base = instrumentation.targetContext
                    val config = Configuration(base.resources.configuration).apply {
                        fontScale = scale
                        screenWidthDp = widthDp
                        screenHeightDp = 800
                        smallestScreenWidthDp = widthDp
                    }
                    val context = ContextThemeWrapper(
                        base.createConfigurationContext(config),
                        R.style.Theme_BankDemoApp
                    )
                    val root = LayoutInflater.from(context)
                        .inflate(R.layout.fragment_qr, null) as ViewGroup
                    val button = root.findViewById<MaterialButton>(R.id.show_tab_btn)
                    val density = context.resources.displayMetrics.density
                    val widthPx = (widthDp * density).roundToInt()
                    val heightPx = (800 * density).roundToInt()
                    for (label in listOf("Показать мой QR", "Сканировать")) {
                        button.text = label
                        root.measure(
                            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
                        )
                        root.layout(0, 0, widthPx, heightPx)
                        val details = "$label, width=$widthDp, fontScale=$scale"
                        val textLayout = checkNotNull(button.layout) { details }
                        assertTrue(details, textLayout.lineCount > 0)
                        assertEquals(details, label.length, textLayout.getLineEnd(textLayout.lineCount - 1))
                        for (line in 0 until textLayout.lineCount) {
                            assertEquals(details, 0, textLayout.getEllipsisCount(line))
                            assertTrue(details, textLayout.getLineWidth(line) <= textLayout.width + 1)
                        }
                        val bounds = Rect(0, 0, button.width, button.height)
                        root.offsetDescendantRectToMyCoords(button, bounds)
                        assertTrue(details, bounds.left >= 0 && bounds.right <= widthPx)
                        val availableHeight = button.height - button.compoundPaddingTop - button.compoundPaddingBottom
                        assertTrue(details, textLayout.height <= availableHeight)
                    }
                }
            }
        }
    }
}
