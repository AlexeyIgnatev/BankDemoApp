package com.esom.bank.screens.main.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesLocalDataSource @Inject constructor() {
    private val storage by lazy { MMKV.mmkvWithID(STORAGE_ID, MMKV.MULTI_PROCESS_MODE) }

    fun getSeenNotificationIds(): Set<String> =
        storage.decodeStringSet(SEEN_NOTIFICATION_IDS, emptySet()).orEmpty()

    fun setSeenNotificationIds(ids: Set<String>) {
        storage.encode(SEEN_NOTIFICATION_IDS, ids)
    }

    fun areBalancesVisible(): Boolean = storage.decodeBool(BALANCES_VISIBLE, true)

    fun setBalancesVisible(visible: Boolean) {
        storage.encode(BALANCES_VISIBLE, visible)
    }

    fun getThemeMode(): Int = storage.decodeInt(THEME_MODE, DEFAULT_THEME_MODE)

    fun setThemeMode(mode: Int) {
        storage.encode(THEME_MODE, mode)
    }

    fun isWalletHistoryExpanded(): Boolean = storage.decodeBool(WALLET_HISTORY_EXPANDED, true)

    fun setWalletHistoryExpanded(expanded: Boolean) {
        storage.encode(WALLET_HISTORY_EXPANDED, expanded)
    }

    private companion object {
        const val STORAGE_ID = "AppPreferencesLocalDataSource"
        const val SEEN_NOTIFICATION_IDS = "seen_notification_ids"
        const val BALANCES_VISIBLE = "balances_visible"
        const val THEME_MODE = "theme_mode"
        const val WALLET_HISTORY_EXPANDED = "wallet_history_expanded"
        const val DEFAULT_THEME_MODE = -1
    }
}
