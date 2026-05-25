package no.duckya.app.data.call

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.duckya.app.data.model.CallStatus
import no.duckya.app.data.model.CallType
import no.duckya.app.webrtc.SignalingService
import no.duckya.app.webrtc.WebRtcClient
import org.webrtc.MediaStream
import org.webrtc.VideoTrack
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CallUiState(
    val callId: String? = null,
    val isCaller: Boolean = false,
    val status: CallStatus = CallStatus.ENDED,
    val type: CallType = CallType.AUDIO,
    val localVideo: VideoTrack? = null,
    val remoteVideo: VideoTrack? = null
)

@Singleton
class CallRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val signaling = SignalingService(firestore)
    private var client: WebRtcClient? = null

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    fun startCall(calleeUid: String, type: CallType) {
        val callerUid = auth.currentUser?.uid ?: return
        val callId = UUID.randomUUID().toString()
        _state.value = CallUiState(
            callId = callId, isCaller = true, status = CallStatus.RINGING, type = type
        )
        val isVideo = type == CallType.VIDEO
        val c = WebRtcClient(
            context = context,
            isVideo = isVideo,
            onLocalIce = { ice ->
                kotlinx.coroutines.runBlocking {
                    runCatching { signaling.publishIce(callId, isCaller = true, ice) }
                }
            },
            onRemoteStream = { stream -> attachRemote(stream) }
        )
        client = c
        c.start()
        _state.value = _state.value.copy(localVideo = c.localVideoTrack)

        c.createOffer { sdp ->
            kotlinx.coroutines.runBlocking {
                runCatching {
                    signaling.publishOffer(callId, callerUid, calleeUid, type.name, sdp)
                }
            }
        }
        signaling.listenForAnswer(callId) { answer -> c.setRemoteDescription(answer) }
        signaling.listenForRemoteIce(callId, listenAsCaller = true) { ice ->
            c.addRemoteIceCandidate(ice)
        }
    }

    fun acceptCall(callId: String, type: CallType) {
        _state.value = CallUiState(
            callId = callId, isCaller = false, status = CallStatus.CONNECTED, type = type
        )
        val isVideo = type == CallType.VIDEO
        val c = WebRtcClient(
            context = context,
            isVideo = isVideo,
            onLocalIce = { ice ->
                kotlinx.coroutines.runBlocking {
                    runCatching { signaling.publishIce(callId, isCaller = false, ice) }
                }
            },
            onRemoteStream = { stream -> attachRemote(stream) }
        )
        client = c
        c.start()
        _state.value = _state.value.copy(localVideo = c.localVideoTrack)

        signaling.listenForOffer(callId) { offer ->
            c.setRemoteDescription(offer)
            c.createAnswer { answer ->
                kotlinx.coroutines.runBlocking {
                    runCatching { signaling.publishAnswer(callId, answer) }
                }
            }
        }
        signaling.listenForRemoteIce(callId, listenAsCaller = false) { ice ->
            c.addRemoteIceCandidate(ice)
        }
    }

    private fun attachRemote(stream: MediaStream) {
        val video = stream.videoTracks.firstOrNull()
        _state.value = _state.value.copy(remoteVideo = video, status = CallStatus.CONNECTED)
    }

    fun endCall() {
        val callId = _state.value.callId
        signaling.stop()
        client?.dispose()
        client = null
        if (callId != null) {
            kotlinx.coroutines.runBlocking {
                runCatching { signaling.endCall(callId) }
            }
        }
        _state.value = CallUiState()
    }

    fun toggleMute(mute: Boolean) {
        client?.localAudioTrack?.setEnabled(!mute)
    }

    fun toggleVideo(enable: Boolean) {
        client?.localVideoTrack?.setEnabled(enable)
    }
}
