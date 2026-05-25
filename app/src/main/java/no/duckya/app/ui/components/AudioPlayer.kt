package no.duckya.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AudioPlayer(url: String, durationMs: Long) {
    var playing by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(url) {
        runCatching {
            player.setDataSource(url)
            player.prepareAsync()
        }
        onDispose { runCatching { player.release() } }
    }

    LaunchedEffect(playing) {
        while (playing && player.isPlaying) {
            val total = player.duration.coerceAtLeast(1)
            progress = player.currentPosition.toFloat() / total
            delay(100)
        }
        if (!player.isPlaying) progress = 0f
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (playing) {
                runCatching { player.pause() }
                playing = false
            } else {
                runCatching { player.start() }
                playing = true
            }
        }) {
            Icon(
                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Stopp" else "Spill"
            )
        }
        Column(modifier = Modifier.width(140.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(2.dp))
            Text("${(durationMs / 1000)}s")
        }
    }
}
