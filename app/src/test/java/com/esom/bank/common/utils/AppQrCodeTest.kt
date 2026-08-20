package com.esom.bank.common.utils

import com.esom.bank.screens.main.enums.CurrencyEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppQrCodeTest {

    @Test
    fun `normalizes local phone from qr to kyrgyzstan format`() {
        val payload = AppQrCode.parsePayload(
            "ESOM_BANK_QR|v=1|currency=SOM|contact=0 777 960 777"
        )

        assertEquals("+996777960777", payload?.contact)
        assertEquals(CurrencyEnum.SOM, payload?.currency)
    }

    @Test
    fun `keeps wallet address unchanged`() {
        val address = "TRVh3EuuWTkCfECfXM77SGZZZQwJT49WBm"

        assertEquals(
            address,
            AppQrCode.parsePayload(AppQrCode.buildContent(address, CurrencyEnum.USDT_TRC20))?.contact
        )
    }

    @Test
    fun `rejects incomplete phone from qr`() {
        val payload = AppQrCode.parsePayload(
            "ESOM_BANK_QR|v=1|currency=SOM|contact=00000000"
        )

        assertNull(payload)
    }
}
