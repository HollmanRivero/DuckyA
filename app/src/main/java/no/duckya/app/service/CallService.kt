package no.duckya.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import no.duckya.app.DuckyApp
import no.duckya.app.MainActivity
import no.duckya.app.R

/** Foreground service that keeps an ongoing call alive. */
@AndroidEntryPoint
class CallService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val callId = intent?.getStringExtra(EXTRA_CALL_ID) ?: "ongoing"
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(callId), type)
        return START_STICKY
    }

    private fun buildNotification(callId: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("active_call_id", callId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, openIntent, flags)
        return NotificationCompat.Builder(this, DuckyApp.CHANNEL_CALL_ONGOING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Pågående samtale")
            .setContentText("Trykk for å åpne")
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val EXTRA_CALL_ID = "call_id"
        private const val NOTIFICATION_ID = 4242
    }
}
