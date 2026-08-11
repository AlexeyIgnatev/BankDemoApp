package com.esom.bank.screens.qr.data

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrimaryCurrencyLocalDataSource @Inject constructor() {
    private val storage by lazy {
        MMKV.mmkvWithID(STORAGE_ID, MMKV.MULTI_PROCESS_MODE)
    }

    fun get(): CurrencyEnum =
        CurrencyEnum.fromNameOrNull(storage.decodeString(PRIMARY_CURRENCY_KEY))
            ?: CurrencyEnum.SOM

    fun set(currency: CurrencyEnum) {
        storage.encode(PRIMARY_CURRENCY_KEY, currency.name)
    }

    private companion object {
        const val STORAGE_ID = "PrimaryCurrencyStore"
        const val PRIMARY_CURRENCY_KEY = "primaryCurrency"
    }
}
