package com.esom.bank.screens.main.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton
import com.esom.bank.screens.transfer.model.TransferTemplate
import com.esom.bank.screens.swap.model.SwapTemplate

@Singleton
class RecentTemplateLocalDataSource @Inject constructor() {
    private val storage by lazy {
        MMKV.mmkvWithID(STORAGE_ID, MMKV.MULTI_PROCESS_MODE)
    }
    private val gson = Gson()

    fun getTransferTemplates(): List<TransferTemplate> = readList(TRANSFER_KEY)

    fun getSwapTemplates(): List<SwapTemplate> = readList(SWAP_KEY)

    fun addTransferTemplate(template: TransferTemplate) {
        writeList(TRANSFER_KEY, listOf(template) + getTransferTemplates())
    }

    fun addSwapTemplate(template: SwapTemplate) {
        writeList(SWAP_KEY, listOf(template) + getSwapTemplates())
    }

    private inline fun <reified T> readList(key: String): List<T> {
        val raw = storage.decodeString(key) ?: return emptyList()
        return try {
            gson.fromJson<List<T>>(raw, object : TypeToken<List<T>>() {}.type).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun <T> writeList(key: String, templates: List<T>) {
        storage.encode(key, gson.toJson(templates.take(MAX_TEMPLATES)))
    }

    private companion object {
        const val STORAGE_ID = "RecentTemplateStore"
        const val TRANSFER_KEY = "transferTemplates"
        const val SWAP_KEY = "swapTemplates"
        const val MAX_TEMPLATES = 5
    }
}
