package no.duckya.app.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import no.duckya.app.data.model.Conversation
import no.duckya.app.data.model.ConversationSource
import no.duckya.app.data.model.SmsMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

    /** Sends a text SMS. Splits long messages automatically. */
    fun sendSms(toNumber: String, body: String) {
        val mgr = smsManager()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        val sent = PendingIntent.getBroadcast(
            context, toNumber.hashCode(),
            Intent(ACTION_SMS_SENT).setPackage(context.packageName), flags
        )
        val delivered = PendingIntent.getBroadcast(
            context, toNumber.hashCode() + 1,
            Intent(ACTION_SMS_DELIVERED).setPackage(context.packageName), flags
        )
        val parts = mgr.divideMessage(body)
        if (parts.size == 1) {
            mgr.sendTextMessage(toNumber, null, body, sent, delivered)
        } else {
            mgr.sendMultipartTextMessage(
                toNumber, null, parts,
                ArrayList<PendingIntent?>().apply { repeat(parts.size) { add(sent) } },
                ArrayList<PendingIntent?>().apply { repeat(parts.size) { add(delivered) } }
            )
        }
    }

    /** Reads ContentResolver-backed SMS conversation list. Requires READ_SMS at runtime. */
    suspend fun loadSmsConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        val uri = Telephony.Sms.Conversations.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Conversations.THREAD_ID,
            Telephony.Sms.Conversations.SNIPPET,
            Telephony.Sms.Conversations.MESSAGE_COUNT
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, "date DESC")
            ?: return@withContext emptyList()

        val out = mutableListOf<Conversation>()
        cursor.use { c ->
            while (c.moveToNext()) {
                val threadId = c.getLong(0)
                val snippet = c.getString(1) ?: ""
                val address = resolveAddressForThread(threadId) ?: "?"
                val lastTs = resolveLastTimestampForThread(threadId) ?: 0L
                out += Conversation(
                    id = "sms:$threadId",
                    source = ConversationSource.SMS,
                    title = address,
                    lastMessagePreview = snippet,
                    lastMessageTimestamp = lastTs,
                    phoneNumber = address
                )
            }
        }
        out
    }

    private fun resolveAddressForThread(threadId: Long): String? {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms"),
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1"
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    private fun resolveLastTimestampForThread(threadId: Long): Long? {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms"),
            arrayOf(Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT 1"
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    fun loadMessagesForAddress(address: String): Flow<List<SmsMessage>> = flow {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms"),
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            ),
            "${Telephony.Sms.ADDRESS} = ?",
            arrayOf(address),
            "${Telephony.Sms.DATE} ASC"
        )
        val list = mutableListOf<SmsMessage>()
        cursor?.use { c ->
            while (c.moveToNext()) {
                val type = c.getInt(4)
                list += SmsMessage(
                    id = c.getLong(0),
                    address = c.getString(1) ?: "",
                    body = c.getString(2) ?: "",
                    timestamp = c.getLong(3),
                    isIncoming = (type == Telephony.Sms.MESSAGE_TYPE_INBOX),
                    read = c.getInt(5) == 1
                )
            }
        }
        emit(list)
    }

    companion object {
        const val ACTION_SMS_SENT = "no.duckya.app.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "no.duckya.app.SMS_DELIVERED"
    }
}
