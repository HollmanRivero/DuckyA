package no.duckya.app.webrtc

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Firestore-backed signaling. Document layout:
 *   calls/{callId}              — { caller, callee, type, status, offer? }
 *   calls/{callId}/answer       — { sdp, type }
 *   calls/{callId}/callerIce/*  — caller ICE candidates
 *   calls/{callId}/calleeIce/*  — callee ICE candidates
 */
class SignalingService(private val firestore: FirebaseFirestore) {

    private var iceRegOffer: ListenerRegistration? = null
    private var iceRegAnswer: ListenerRegistration? = null
    private var answerReg: ListenerRegistration? = null
    private var offerReg: ListenerRegistration? = null

    suspend fun publishOffer(callId: String, callerId: String, calleeId: String, type: String, sdp: SessionDescription) {
        firestore.collection("calls").document(callId).set(
            mapOf(
                "caller" to callerId,
                "callee" to calleeId,
                "type" to type,
                "status" to "RINGING",
                "offerSdp" to sdp.description,
                "offerType" to sdp.type.canonicalForm(),
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun publishAnswer(callId: String, sdp: SessionDescription) {
        firestore.collection("calls").document(callId).set(
            mapOf(
                "answerSdp" to sdp.description,
                "answerType" to sdp.type.canonicalForm(),
                "status" to "CONNECTED"
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun publishIce(callId: String, isCaller: Boolean, candidate: IceCandidate) {
        val sub = if (isCaller) "callerIce" else "calleeIce"
        firestore.collection("calls").document(callId)
            .collection(sub)
            .add(
                mapOf(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "candidate" to candidate.sdp
                )
            ).await()
    }

    fun listenForAnswer(callId: String, onAnswer: (SessionDescription) -> Unit) {
        answerReg = firestore.collection("calls").document(callId)
            .addSnapshotListener { snap, _ ->
                val sdp = snap?.getString("answerSdp") ?: return@addSnapshotListener
                val typeStr = snap.getString("answerType") ?: "answer"
                onAnswer(SessionDescription(SessionDescription.Type.fromCanonicalForm(typeStr), sdp))
            }
    }

    fun listenForOffer(callId: String, onOffer: (SessionDescription) -> Unit) {
        offerReg = firestore.collection("calls").document(callId)
            .addSnapshotListener { snap, _ ->
                val sdp = snap?.getString("offerSdp") ?: return@addSnapshotListener
                val typeStr = snap.getString("offerType") ?: "offer"
                onOffer(SessionDescription(SessionDescription.Type.fromCanonicalForm(typeStr), sdp))
            }
    }

    fun listenForRemoteIce(callId: String, listenAsCaller: Boolean, onIce: (IceCandidate) -> Unit) {
        // Caller listens to callee's candidates; callee listens to caller's.
        val sub = if (listenAsCaller) "calleeIce" else "callerIce"
        val reg = firestore.collection("calls").document(callId)
            .collection(sub)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { ch ->
                    if (ch.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val d = ch.document
                        val sdpMid = d.getString("sdpMid") ?: return@forEach
                        val idx = (d.getLong("sdpMLineIndex") ?: 0L).toInt()
                        val cand = d.getString("candidate") ?: return@forEach
                        onIce(IceCandidate(sdpMid, idx, cand))
                    }
                }
            }
        if (listenAsCaller) iceRegAnswer = reg else iceRegOffer = reg
    }

    suspend fun endCall(callId: String) {
        firestore.collection("calls").document(callId).set(
            mapOf("status" to "ENDED", "endedAt" to System.currentTimeMillis()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    fun stop() {
        answerReg?.remove(); answerReg = null
        offerReg?.remove(); offerReg = null
        iceRegOffer?.remove(); iceRegOffer = null
        iceRegAnswer?.remove(); iceRegAnswer = null
    }
}
