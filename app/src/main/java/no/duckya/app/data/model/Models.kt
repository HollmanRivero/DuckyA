package no.duckya.app.data.model

data class UserProfile(
    val uid: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val fcmToken: String? = null
)

enum class MessageType { TEXT, AUDIO, IMAGE }

data class Message(
    val id: String = "",
    val senderId: String = "",
    val body: String = "",
    val type: MessageType = MessageType.TEXT,
    val audioUrl: String? = null,
    val audioDurationMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/** Type of conversation row in the merged list. */
enum class ConversationSource { SMS, DUCKY }

data class Conversation(
    val id: String,
    val source: ConversationSource,
    val title: String,
    val lastMessagePreview: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val phoneNumber: String? = null,
    val participants: List<String> = emptyList()
)

enum class CallType { AUDIO, VIDEO }
enum class CallStatus { RINGING, CONNECTED, ENDED, MISSED, REJECTED }

data class CallSession(
    val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val type: CallType = CallType.AUDIO,
    val status: CallStatus = CallStatus.RINGING,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val read: Boolean
)
