package no.duckya.app.ui.screens.login

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import no.duckya.app.data.auth.AuthRepository
import no.duckya.app.data.auth.AuthState
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {
    val state: StateFlow<AuthState> = authRepository.state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val activity = LocalActivity.current

    var phone by remember { mutableStateOf("+47") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is AuthState.SignedIn) onSignedIn()
        if (state is AuthState.CodeSent) verificationId = (state as AuthState.CodeSent).verificationId
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Logg inn på DuckyA") }) }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Skriv inn telefonnummeret ditt i E.164-format (f.eks. +4798765432)",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefonnummer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = verificationId == null,
                modifier = Modifier.fillMaxWidth()
            )

            if (verificationId != null) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                    label = { Text("SMS-kode (6 sifre)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { vm.authRepository.verifyCode(verificationId!!, code) },
                    enabled = code.length == 6 && state !is AuthState.Verifying,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Verifiser kode") }
            } else {
                Button(
                    onClick = { activity?.let { vm.authRepository.sendOtp(phone, it) } },
                    enabled = phone.startsWith("+") && phone.length >= 8 && state !is AuthState.SendingCode,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Send SMS-kode") }
            }

            when (val s = state) {
                is AuthState.SendingCode -> {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                    Text("Sender kode...")
                }
                is AuthState.Verifying -> {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                    Text("Logger inn...")
                }
                is AuthState.Error -> {
                    Spacer(Modifier.height(8.dp))
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }
        }
    }
}
