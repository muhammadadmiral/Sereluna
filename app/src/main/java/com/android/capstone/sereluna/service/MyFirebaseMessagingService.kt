package com.android.capstone.sereluna.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.capstone.sereluna.MainActivity
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data["type"] == "notification_refresh") {
            sendBroadcast(Intent(ACTION_NOTIFICATION_REFRESH).setPackage(packageName))
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            val title = it.title ?: "New Notification"
            val body = it.body ?: "You have a new message."
            sendNotification(title, body)
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token == null) return
        if (FirebaseAuth.getInstance().currentUser == null) {
            getSharedPreferences("fcm_token", MODE_PRIVATE)
                .edit()
                .putString("pending_token", token)
                .apply()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SerelunaRepository().submitDeviceToken(token)
                Log.d(TAG, "FCM token submitted to backend")
            } catch (e: Exception) {
                getSharedPreferences("fcm_token", MODE_PRIVATE)
                    .edit()
                    .putString("pending_token", token)
                    .apply()
                Log.w(TAG, "Error submitting FCM token", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val ACTION_NOTIFICATION_REFRESH = "com.android.capstone.sereluna.NOTIFICATION_REFRESH"
    }
}
