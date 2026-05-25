package no.duckya.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DuckyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Meldinger",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Nye SMS og DuckyA-meldinger" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CALLS,
                "Innkommende anrop",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ringer fra andre DuckyA-brukere"
                setSound(null, null)
                enableVibration(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CALL_ONGOING,
                "Pågående samtale",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_CALLS = "calls_incoming"
        const val CHANNEL_CALL_ONGOING = "calls_ongoing"
    }
}
