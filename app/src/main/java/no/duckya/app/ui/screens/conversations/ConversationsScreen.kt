package no.duckya.app.ui.screens.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.duckya.app.data.chat.ChatRepository
import no.duckya.app.data.model.Conversation
import no.duckya.app.data.model.ConversationSource
import no.duckya.app.data.sms.SmsRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val sms = runCatching { smsRepository.loadSmsConversations() }.getOrDefault(emptyList())
            val ducky = runCatching { chatRepository.loadConversations() }.getOrDefault(emptyList())
            _conversations.value = (sms + ducky).sortedByDescending { it.lastMessageTimestamp }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ConversationsScreen(
    onOpenConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: ConversationsViewModel = hiltViewModel()
) {
    val items by vm.conversations.collectAsState()

    val perms = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_CONTACTS
        )
    )

    LaunchedEffect(Unit) {
        if (!perms.allPermissionsGranted) perms.launchMultiplePermissionRequest()
        vm.refresh()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DuckyA") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Innstillinger")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Ny chat") }
            )
        }
    ) { inner ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Ingen samtaler ennå")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(inner).fillMaxSize()) {
                items(items, key = { it.id }) { c ->
                    ConversationRow(c, onClick = { onOpenConversation(c) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (c.source == ConversationSource.SMS) Icons.Filled.Sms else Icons.Filled.Phone,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(c.title, style = MaterialTheme.typography.titleMedium)
            Text(
                c.lastMessagePreview,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            sdf.format(Date(c.lastMessageTimestamp)),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
