package com.esom.bank.screens.messaging.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface MessagingLocalDataSource {
    fun isPushNotificationsEnabled(): Boolean
    fun setPushNotificationsEnabled(enabled: Boolean)
    fun getFcmToken(): String?
    fun setFcmToken(token: String?)
    fun getNextNotificationId(): Int
    fun clearMessagingData()
}

class MessagingLocalDataSourceImpl @Inject constructor() : MessagingLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "MessagingLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }

    override fun isPushNotificationsEnabled(): Boolean =
        storage.decodeBool(KEY_PUSH_NOTIFICATIONS_ENABLED, true)

    override fun setPushNotificationsEnabled(enabled: Boolean) {
        storage.encode(KEY_PUSH_NOTIFICATIONS_ENABLED, enabled)
    }

    override fun getFcmToken(): String? =
        storage.decodeString(KEY_FCM_TOKEN)

    override fun setFcmToken(token: String?) {
        storage.encode(KEY_FCM_TOKEN, token)
    }

    override fun getNextNotificationId(): Int {
        val nextId = storage.decodeInt(KEY_NOTIFICATION_ID, 0) + 1
        storage.encode(KEY_NOTIFICATION_ID, nextId)
        return nextId
    }

    override fun clearMessagingData() {
        storage.removeValueForKey(KEY_PUSH_NOTIFICATIONS_ENABLED)
        storage.removeValueForKey(KEY_FCM_TOKEN)
        storage.removeValueForKey(KEY_NOTIFICATION_ID)
    }

    private companion object {
        const val KEY_PUSH_NOTIFICATIONS_ENABLED = "pushNotificationsEnabled"
        const val KEY_FCM_TOKEN = "fcmToken"
        const val KEY_NOTIFICATION_ID = "notificationId"
    }
}
