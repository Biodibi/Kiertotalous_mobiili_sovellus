package com.oamk.kiertotalous.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_VIBRATE
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.oamk.kiertotalous.Const
import com.oamk.kiertotalous.R
import com.oamk.kiertotalous.model.UserRole
import com.oamk.kiertotalous.ui.MainActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

class FcmService : FirebaseMessagingService() {
    val localDataRepository: LocalDataRepository by inject()

    override fun onNewToken(refreshedToken: String) {
        super.onNewToken(refreshedToken)

        FirebaseMessaging.getInstance().subscribeToTopic(Const.TOPIC_DELIVERIES)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.d("Push received", remoteMessage.data["data"])
        if (localDataRepository.userAccount?.userRole() == UserRole.COURIER) {
            remoteMessage.messageId?.let { messageId ->
                remoteMessage.data["title"]?.let { title ->
                    remoteMessage.data["body"]?.let { body ->
                        notify(messageId, title, body)
                    }
                }
            }
        }
    }

    private fun notify(messageId: String, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // TODO: Do we need to support grouping?
        NotificationCompat.Builder(applicationContext, Const.CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(body)
            setSmallIcon(R.drawable.ic_nature)
            setAutoCancel(true)
            setDefaults(DEFAULT_VIBRATE)
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_MAX
        }.run {
            notificationManager.notify(messageId, SystemClock.uptimeMillis().toInt(), build())
        }
    }
}