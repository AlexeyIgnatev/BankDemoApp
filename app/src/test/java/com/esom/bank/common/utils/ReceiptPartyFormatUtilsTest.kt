package com.esom.bank.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptPartyFormatUtilsTest {
    @Test
    fun prefersFullWalletOverMaskedReceiptValue() {
        assertEquals(
            "TRVh3EuuWTkCfECfXM77SGZZZQwJT49WBm",
            resolveReusableRecipient(
                "****wJT49WBm",
                "TRVh3EuuWTkCfECfXM77SGZZZQwJT49WBm"
            )
        )
    }

    @Test
    fun keepsFullPhoneForTemplate() {
        assertEquals(
            "+996777960777",
            resolveReusableRecipient("+996777960777", "****77960777")
        )
    }
}
