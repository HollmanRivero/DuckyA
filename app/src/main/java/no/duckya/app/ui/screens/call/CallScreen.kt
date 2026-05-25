package no.duckya.app.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import no.duckya.app.data.call.CallRepository
import no.duckya.app.data.call.CallUiState
import no.duckya.app.data.model.CallStatus
import no.duckya.app.data.model.CallType
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val callRepository: CallRepository
) : ViewModel()

@Composable
fun CallScreen(
    onEnd: () -> Unit,
    vm: CallViewModel = hiltViewModel()
) {
    val state by vm.callRepository.state.collectAsState()
    var muted by remember { mutableStateOf(false) }
    var videoOff by remember { mutableStateOf(false) }
    val eglBase = remember { EglBase.create() }

    DisposableEffect(Unit) {
        onDispose { eglBase.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (state.type == CallType.VIDEO) {
            VideoTracks(state, eglBase)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = when (state.status) {
                        CallStatus.RINGING -> "Ringer..."
                        CallStatus.CONNECTED -> "Tilkoblet"
                        else -> "Avsluttet"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FilledIconButton(onClick = {
                muted = !muted
                vm.callRepository.toggleMute(muted)
            }) {
                Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null)
            }
            if (state.type == CallType.VIDEO) {
                FilledIconButton(onClick = {
                    videoOff = !videoOff
                    vm.callRepository.toggleVideo(!videoOff)
                }) {
                    Icon(if (videoOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam, null)
                }
            }
            FilledIconButton(
                onClick = { vm.callRepository.endCall(); onEnd() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = "Avslutt")
            }
        }
    }
}

@Composable
private fun VideoTracks(state: CallUiState, eglBase: EglBase) {
    Box(modifier = Modifier.fillMaxSize()) {
        state.remoteVideo?.let { track ->
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setMirror(false)
                        track.addSink(this)
                    }
                }
            )
        }
        state.localVideo?.let { track ->
            AndroidView(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp),
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setMirror(true)
                        setZOrderMediaOverlay(true)
                        track.addSink(this)
                    }
                }
            )
        }
    }
}
