package no.duckya.app.webrtc

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Thin wrapper around a single WebRTC PeerConnection.
 * Uses Google's free STUN server (no TURN — works on most home networks).
 */
class WebRtcClient(
    private val context: Context,
    private val isVideo: Boolean,
    private val onLocalIce: (IceCandidate) -> Unit,
    private val onRemoteStream: (MediaStream) -> Unit
) {
    val eglBase: EglBase = EglBase.create()
    private val factory: PeerConnectionFactory
    private var peer: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoCapturer: VideoCapturer? = null
    var localAudioTrack: AudioTrack? = null
        private set
    var localVideoTrack: VideoTrack? = null
        private set

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun start() {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peer = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) = onLocalIce(candidate)
            override fun onAddStream(stream: MediaStream) = onRemoteStream(stream)
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out MediaStream>?) {}
        })

        addLocalMedia()
    }

    private fun addLocalMedia() {
        val p = peer ?: return
        localAudioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack("audio0", localAudioSource).also {
            p.addTrack(it, listOf("stream0"))
        }
        if (isVideo) {
            val enumerator = Camera2Enumerator(context)
            val deviceName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
            if (deviceName != null) {
                val capturer = enumerator.createCapturer(deviceName, null)
                localVideoCapturer = capturer
                localVideoSource = factory.createVideoSource(capturer.isScreencast)
                val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                capturer.initialize(surfaceHelper, context, localVideoSource!!.capturerObserver)
                capturer.startCapture(640, 480, 30)
                localVideoTrack = factory.createVideoTrack("video0", localVideoSource).also {
                    p.addTrack(it, listOf("stream0"))
                }
            }
        }
    }

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
        }
        peer?.createOffer(simpleObserver { sdp ->
            peer?.setLocalDescription(simpleObserver { }, sdp)
            callback(sdp)
        }, constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peer?.createAnswer(simpleObserver { sdp ->
            peer?.setLocalDescription(simpleObserver { }, sdp)
            callback(sdp)
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peer?.setRemoteDescription(simpleObserver { }, sdp)
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        peer?.addIceCandidate(candidate)
    }

    fun dispose() {
        runCatching { localVideoCapturer?.stopCapture() }
        localVideoCapturer?.dispose()
        localVideoSource?.dispose()
        localAudioSource?.dispose()
        peer?.close()
        peer = null
        eglBase.release()
    }

    private fun simpleObserver(onSuccess: (SessionDescription) -> Unit) = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription) = onSuccess(p0)
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
