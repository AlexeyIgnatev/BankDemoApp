package com.esom.bank.service.messaging

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.esom.bank.R
import com.esom.bank.activities.MainActivity
import com.esom.bank.screens.main.data.MainRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            handleDataMessage(data)
            return
        }

        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: getString(R.string.app_name)
            val text = notification.body ?: return
            showNotificationInternal(title, text)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        mainRepository.setFcmToken(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val commandId = data["id"] ?: RemoteCommandId.SHOW_NOTIFICATION.value
        when (RemoteCommandId.fromValue(commandId)) {
            RemoteCommandId.SHOW_NOTIFICATION -> showNotification(data)
            else -> Log.d(TAG, "unsupported commandId: $commandId")
        }
    }

    private fun showNotification(data: Map<String, String>) {
        val title = data["title"] ?: getString(R.string.app_name)
        val text = data["text"] ?: data["body"]

        if (text == null) {
            Log.d(TAG, "showNotification: Empty text")
            return
        }

        showNotificationInternal(title, text, data["url"])
    }

    private fun showNotificationInternal(
        title: String,
        text: String,
        url: String? = null
    ) {
        if (!mainRepository.isPushNotificationsEnabled()) return
        if (!hasNotificationPermission()) return

        val pendingIntent = createPendingIntent(url)
        val notification = NotificationCompat.Builder(this, FCM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setColor(ContextCompat.getColor(this, R.color.red))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(mainRepository.getNextNotificationId(), notification)
    }

    private fun createPendingIntent(url: String?): PendingIntent {
        val intent = if (url != null) {
            Intent(Intent.ACTION_VIEW, url.toUri())
        } else {
            Intent(this, MainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            FCM_NOTIFICATION_CHANNEL_ID,
            "FCM notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "FCM notifications"
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "FCMService"
        const val FCM_NOTIFICATION_CHANNEL_ID = "FCM_CHANNEL_ID"
    }
}
