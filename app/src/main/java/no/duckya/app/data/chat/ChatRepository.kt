package no.duckya.app.data.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import no.duckya.app.data.model.Conversation
import no.duckya.app.data.model.ConversationSource
import no.duckya.app.data.model.Message
import no.duckya.app.data.model.MessageType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val conversationsRef = firestore.collection("conversations")

    /** Deterministic conversation id for a pair of uids. */
    private fun pairId(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")

    suspend fun loadConversations(): List<Conversation> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snap = conversationsRef
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snap.documents.map { d ->
            Conversation(
                id = "ducky:${d.id}",
                source = ConversationSource.DUCKY,
                title = (d.getString("title") ?: "DuckyA-chat"),
                lastMessagePreview = d.getString("lastMessagePreview") ?: "",
                lastMessageTimestamp = d.getLong("lastMessageTimestamp") ?: 0L,
                participants = (d.get("participants") as? List<*>)?.filterIsInstance<String>()
                    ?: emptyList()
            )
        }
    }

    fun observeMessages(convId: String): Flow<List<Message>> = callbackFlow {
        val reg = conversationsRef.document(convId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(500)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { d ->
                    Message(
                        id = d.id,
                        senderId = d.getString("senderId") ?: "",
                        body = d.getString("body") ?: "",
                        type = runCatching {
                            MessageType.valueOf(d.getString("type") ?: "TEXT")
                        }.getOrDefault(MessageType.TEXT),
                        audioUrl = d.getString("audioUrl"),
                        audioDurationMs = d.getLong("audioDurationMs"),
                        timestamp = d.getLong("timestamp") ?: 0L
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun sendText(convId: String, body: String) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val convRef = conversationsRef.document(convId)
        convRef.collection("messages").add(
            mapOf(
                "senderId" to uid,
                "body" to body,
                "type" to MessageType.TEXT.name,
                "timestamp" to now
            )
        ).await()
        convRef.set(
            mapOf(
                "lastMessagePreview" to body.take(80),
                "lastMessageTimestamp" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun sendAudio(convId: String, audioUrl: String, durationMs: Long) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val convRef = conversationsRef.document(convId)
        convRef.collection("messages").add(
            mapOf(
                "senderId" to uid,
                "body" to "",
                "type" to MessageType.AUDIO.name,
                "audioUrl" to audioUrl,
                "audioDurationMs" to durationMs,
                "timestamp" to now
            )
        ).await()
        convRef.set(
            mapOf(
                "lastMessagePreview" to "🎤 Lydklipp",
                "lastMessageTimestamp" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    /** Create or fetch a 1:1 conversation between current user and [otherUid]. */
    suspend fun ensureConversation(otherUid: String, otherTitle: String): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val id = pairId(uid, otherUid)
        val ref = conversationsRef.document(id)
        ref.set(
            mapOf(
                "participants" to listOf(uid, otherUid),
                "title" to otherTitle,
                "lastMessageTimestamp" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
        return id
    }
}
