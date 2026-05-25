package no.duckya.app.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.duckya.app.data.audio.AudioRepository
import no.duckya.app.data.chat.ChatRepository
import no.duckya.app.data.model.Message
import no.duckya.app.data.model.MessageType
import no.duckya.app.ui.components.AudioPlayer
import no.duckya.app.ui.components.AudioRecorderButton
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {
    val convId: String = checkNotNull(savedState["convId"])
    val title: String = savedState.get<String>("title") ?: "Chat"

    val messages: StateFlow<List<Message>> =
        chatRepository.observeMessages(convId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    fun sendText(body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            runCatching { chatRepository.sendText(convId, body.trim()) }
            _sending.value = false
        }
    }

    fun onAudioRecorded(filePath: String, durationMs: Long) {
        viewModelScope.launch {
            _sending.value = true
            runCatching {
                val url = audioRepository.uploadAudio(convId, filePath)
                chatRepository.sendAudio(convId, url, durationMs)
            }
            _sending.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onStartCall: (audio: Boolean) -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val messages by vm.messages.collectAsState()
    val sending by vm.sending.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tilbake")
                    }
                },
                actions = {
                    IconButton(onClick = { onStartCall(true) }) {
                        Icon(Icons.Filled.Call, contentDescription = "Lydsamtale")
                    }
                    IconButton(onClick = { onStartCall(false) }) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Videosamtale")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Melding...") },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                AudioRecorderButton(onRecorded = vm::onAudioRecorded)
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { vm.sendText(input); input = "" },
                    enabled = input.isNotBlank() && !sending
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg -> MessageBubble(msg) }
        }
    }
}

@Composable
private fun MessageBubble(msg: Message) {
    val isMine = msg.senderId == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (msg.type) {
                    MessageType.AUDIO -> AudioPlayer(
                        url = msg.audioUrl.orEmpty(),
                        durationMs = msg.audioDurationMs ?: 0L
                    )
                    else -> Text(msg.body)
                }
            }
        }
    }
}
