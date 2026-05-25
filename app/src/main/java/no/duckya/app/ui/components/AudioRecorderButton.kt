package no.duckya.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import no.duckya.app.data.audio.AudioRepository
import javax.inject.Inject

@HiltViewModel
class AudioRecorderViewModel @Inject constructor(
    val audioRepository: AudioRepository
) : ViewModel()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecorderButton(
    onRecorded: (filePath: String, durationMs: Long) -> Unit,
    vm: AudioRecorderViewModel = hiltViewModel()
) {
    val perm = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    var recording by remember { mutableStateOf(false) }

    FilledIconButton(
        onClick = {},
        modifier = Modifier
            .size(48.dp)
            .pointerInput(perm.status.isGranted) {
                detectTapGestures(
                    onPress = {
                        if (!perm.status.isGranted) {
                            perm.launchPermissionRequest()
                            return@detectTapGestures
                        }
                        try {
                            vm.audioRepository.startRecording()
                            recording = true
                            val released = tryAwaitRelease()
                            recording = false
                            val result = vm.audioRepository.stopRecording()
                            if (released && result != null && result.second > 500) {
                                onRecorded(result.first, result.second)
                            }
                        } catch (_: Throwable) {
                            recording = false
                            vm.audioRepository.cancelRecording()
                        }
                    }
                )
            },
        colors = if (recording)
            IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        else IconButtonDefaults.filledIconButtonColors()
    ) {
        Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "Spille inn lyd")
    }
}

private val com.google.accompanist.permissions.PermissionStatus.isGranted: Boolean
    @OptIn(ExperimentalPermissionsApi::class)
    get() = this is com.google.accompanist.permissions.PermissionStatus.Granted
