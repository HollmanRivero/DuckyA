package no.duckya.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import no.duckya.app.DuckyApp
import no.duckya.app.MainActivity
import no.duckya.app.R
import javax.inject.Inject

@AndroidEntryPoint
class DuckyMessagingService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            "incoming_call" -> notifyIncomingCall(data)
            else -> notifyMessage(message)
        }
    }

    private fun notifyMessage(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "DuckyA"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val nm = getSystemService(NotificationManager::class.java) ?: return

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, openIntent, flags)

        val n = NotificationCompat.Builder(this, DuckyApp.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_sms)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), n)
    }

    private fun notifyIncomingCall(data: Map<String, String>) {
        val callId = data["callId"] ?: return
        val callerName = data["caller"] ?: "Ukjent"
        val type = data["callType"] ?: "AUDIO"
        val nm = getSystemService(NotificationManager::class.java) ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("incoming_call_id", callId)
            putExtra("incoming_call_type", type)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, callId.hashCode(), intent, flags)

        val n = NotificationCompat.Builder(this, DuckyApp.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Innkommende anrop")
            .setContentText("$callerName ringer ($type)")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        nm.notify(callId.hashCode(), n)
    }
}
