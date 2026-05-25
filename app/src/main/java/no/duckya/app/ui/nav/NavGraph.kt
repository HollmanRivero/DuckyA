package no.duckya.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.lifecycle.HiltViewModel
import no.duckya.app.data.auth.AuthRepository
import no.duckya.app.data.auth.AuthState
import no.duckya.app.data.call.CallRepository
import no.duckya.app.data.model.CallType
import no.duckya.app.ui.screens.call.CallScreen
import no.duckya.app.ui.screens.chat.ChatScreen
import no.duckya.app.ui.screens.chat.NewChatScreen
import no.duckya.app.ui.screens.conversations.ConversationsScreen
import no.duckya.app.ui.screens.login.LoginScreen
import no.duckya.app.ui.screens.settings.SettingsScreen
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    val authRepository: AuthRepository,
    val callRepository: CallRepository
) : ViewModel()

@Composable
fun DuckyNavGraph(root: RootViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val authState by root.authRepository.state.collectAsState()
    val startDest = if (authState is AuthState.SignedIn) "conversations" else "login"

    NavHost(navController = nav, startDestination = startDest) {
        composable("login") {
            LoginScreen(onSignedIn = {
                nav.navigate("conversations") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("conversations") {
            ConversationsScreen(
                onOpenConversation = { conv ->
                    if (conv.id.startsWith("ducky:")) {
                        val id = conv.id.removePrefix("ducky:")
                        val title = URLEncoder.encode(conv.title, "UTF-8")
                        nav.navigate("chat/$id/$title")
                    } else {
                        val phone = URLEncoder.encode(conv.phoneNumber ?: conv.title, "UTF-8")
                        nav.navigate("sms/$phone")
                    }
                },
                onNewChat = { nav.navigate("new_chat") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }

        composable("new_chat") {
            NewChatScreen(
                onBack = { nav.popBackStack() },
                onChatReady = { convId, title ->
                    val t = URLEncoder.encode(title, "UTF-8")
                    nav.navigate("chat/$convId/$t") {
                        popUpTo("new_chat") { inclusive = true }
                    }
                }
            )
        }

        composable(
            "chat/{convId}/{title}",
            arguments = listOf(
                navArgument("convId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) {
            ChatScreen(
                onBack = { nav.popBackStack() },
                onStartCall = { isAudio ->
                    val type = if (isAudio) CallType.AUDIO else CallType.VIDEO
                    // For demo: derive callee uid from conversation participants.
                    nav.navigate("call/${type.name}")
                }
            )
        }

        composable(
            "sms/{address}",
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) {
            // SMS thread view — kept simple for v0.1: redirect to system Messages app would also work.
            ChatScreen(
                onBack = { nav.popBackStack() },
                onStartCall = { /* SMS thread: starting WebRTC requires uid; skipped */ }
            )
        }

        composable(
            "call/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) {
            CallScreen(onEnd = { nav.popBackStack() })
        }

        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onSignedOut = {
                    nav.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
