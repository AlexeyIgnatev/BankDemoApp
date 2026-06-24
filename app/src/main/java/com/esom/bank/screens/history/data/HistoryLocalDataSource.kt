package com.esom.bank.screens.history.data

import com.esom.bank.screens.main.enums.CurrencyEnum
import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface HistoryLocalDataSource {
    fun getCurrency(): List<CurrencyEnum>
    fun setCurrency(currency: List<CurrencyEnum>)

    fun getFromTime(): Long
    fun setFromTime(time: Long)

    fun getToTime(): Long
    fun setToTime(time: Long)

    fun getWithoutTransactions(): Boolean
    fun setWithoutTransactions(without: Boolean)

    fun clearAllHistoryData()
}

class HistoryLocalDataSourceImpl @Inject constructor(): HistoryLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "HistoryLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }

    override fun getCurrency(): List<CurrencyEnum> {
        val currencyString = storage.decodeString("currency")
        return currencyString?.split(",")?.mapNotNull {
            try { CurrencyEnum.valueOf(it) } catch (e: Exception) { null }
        }?.ifEmpty {
            listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM, CurrencyEnum.USDT_TRC20)
        } ?: listOf(CurrencyEnum.SOM, CurrencyEnum.ESOM, CurrencyEnum.USDT_TRC20)
    }

    override fun setCurrency(currency: List<CurrencyEnum>) {
        val currencyString = currency.joinToString(",") { it.name }
        storage.encode("currency", currencyString)
    }

    override fun getFromTime(): Long {
        val stored = storage.decodeLong("from_time")
        return if (stored != 0L) {
            stored
        } else {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
            calendar.add(java.util.Calendar.MONTH, -1)
            calendar.timeInMillis
        }
    }

    override fun setFromTime(time: Long) {
        storage.encode("from_time", time)
    }

    override fun getToTime(): Long {
        val stored = storage.decodeLong("to_time")
        return if (stored != 0L) {
            stored
        } else {
            System.currentTimeMillis()
        }
    }

    override fun setToTime(time: Long) {
        storage.encode("to_time", time)
    }

    override fun getWithoutTransactions(): Boolean =
        storage.decodeBool("without_transactions", false)

    override fun setWithoutTransactions(without: Boolean) {
        storage.encode("without_transactions", without)
    }

    override fun clearAllHistoryData() {
        storage.removeValueForKey("currency")
        storage.removeValueForKey("from_time")
        storage.removeValueForKey("to_time")
        storage.removeValueForKey("without_transactions")
    }
}
