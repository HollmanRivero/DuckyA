package no.duckya.app.data.sms

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import no.duckya.app.DuckyApp
import no.duckya.app.MainActivity
import no.duckya.app.R

/** Receives incoming SMS_RECEIVED broadcasts and shows a notification. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val byAddress = msgs.groupBy { it.originatingAddress ?: "Ukjent" }
            .mapValues { (_, ms) -> ms.joinToString("") { it.messageBody.orEmpty() } }

        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        byAddress.forEach { (address, body) ->
            val tap = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_CONVERSATION, address)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getActivity(context, address.hashCode(), tap, flags)

            val n = NotificationCompat.Builder(context, DuckyApp.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_sms)
                .setContentTitle(address)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(address.hashCode(), n)
        }
    }

    companion object {
        const val EXTRA_OPEN_CONVERSATION = "open_conversation_address"
    }
}

/** Receives delivery confirmations for outgoing SMS. */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Result code is implicitly available; could be persisted to Room for delivery status UI.
    }
}

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Same — could be persisted.
    }
}
