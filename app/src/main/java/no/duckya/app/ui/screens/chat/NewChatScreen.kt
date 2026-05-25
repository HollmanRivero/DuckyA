package no.duckya.app.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import no.duckya.app.data.chat.ChatRepository
import javax.inject.Inject

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    sealed interface UiState {
        data object Idle : UiState
        data object Searching : UiState
        data class Found(val convId: String, val title: String) : UiState
        data class NotFound(val message: String) : UiState
    }

    fun startChatByPhone(phoneE164: String) {
        viewModelScope.launch {
            _state.value = UiState.Searching
            val snap = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneE164)
                .limit(1).get().await()
            val user = snap.documents.firstOrNull()
            if (user == null) {
                _state.value = UiState.NotFound("Ingen DuckyA-bruker med dette nummeret")
                return@launch
            }
            val otherUid = user.getString("uid") ?: user.id
            val title = user.getString("displayName") ?: phoneE164
            val id = chatRepository.ensureConversation(otherUid, title)
            _state.value = UiState.Found(id, title)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onChatReady: (convId: String, title: String) -> Unit,
    vm: NewChatViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var phone by remember { mutableStateOf("+47") }

    LaunchedEffect(state) {
        if (state is NewChatViewModel.UiState.Found) {
            val s = state as NewChatViewModel.UiState.Found
            onChatReady(s.convId, s.title)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ny DuckyA-chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Skriv inn telefonnummeret til DuckyA-brukeren du vil chatte med (E.164).")
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telefonnummer") }
            )
            Button(
                onClick = { vm.startChatByPhone(phone) },
                enabled = phone.startsWith("+") && phone.length >= 8 && state !is NewChatViewModel.UiState.Searching,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start chat") }
            when (val s = state) {
                is NewChatViewModel.UiState.Searching -> CircularProgressIndicator()
                is NewChatViewModel.UiState.NotFound -> Text(s.message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
