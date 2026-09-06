package com.esom.bank.screens.main.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.esom.bank.screens.swap.model.SwapTemplate
import com.esom.bank.screens.transfer.model.TransferTemplate
import java.math.BigDecimal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentTemplateLocalDataSourceTest {

    @Test
    fun clearTemplatesRemovesTransferAndSwapTemplates() {
        val dataSource = RecentTemplateLocalDataSource()
        dataSource.clearTemplates()

        dataSource.addTransferTemplate(
            TransferTemplate(
                amount = BigDecimal("100"),
                currency = "SOM",
                recipient = "+996555123456",
                isPhone = true,
                name = "Тестовый перевод"
            )
        )
        dataSource.addSwapTemplate(
            SwapTemplate(
                amount = BigDecimal("50"),
                fromCurrency = "SOM",
                toCurrency = "ESOM",
                name = "Тестовый обмен"
            )
        )

        assertTrue(dataSource.getTransferTemplates().isNotEmpty())
        assertTrue(dataSource.getSwapTemplates().isNotEmpty())

        dataSource.clearTemplates()

        assertFalse(dataSource.getTransferTemplates().isNotEmpty())
        assertFalse(dataSource.getSwapTemplates().isNotEmpty())
    }
}
