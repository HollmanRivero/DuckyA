package no.duckya.app.data.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import no.duckya.app.data.model.UserProfile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Idle : AuthState
    data object SendingCode : AuthState
    data class CodeSent(val verificationId: String) : AuthState
    data object Verifying : AuthState
    data class SignedIn(val uid: String, val phone: String) : AuthState
    data class Error(val message: String) : AuthState
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _state = MutableStateFlow<AuthState>(
        auth.currentUser?.let {
            AuthState.SignedIn(it.uid, it.phoneNumber ?: "")
        } ?: AuthState.Idle
    )
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val currentUid: String? get() = auth.currentUser?.uid
    val currentPhone: String? get() = auth.currentUser?.phoneNumber

    fun sendOtp(phoneE164: String, activity: Activity) {
        _state.value = AuthState.SendingCode
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    _state.value = AuthState.Error(e.localizedMessage ?: "Verifisering feilet")
                }
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    _state.value = AuthState.CodeSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(verificationId: String, code: String) {
        _state.value = AuthState.Verifying
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    _state.value = AuthState.Error("Ingen bruker etter innlogging")
                    return@addOnSuccessListener
                }
                _state.value = AuthState.SignedIn(user.uid, user.phoneNumber ?: "")
                // Fire-and-forget profile upsert
                runCatching {
                    firestore.collection("users").document(user.uid).set(
                        mapOf(
                            "uid" to user.uid,
                            "phoneNumber" to (user.phoneNumber ?: ""),
                            "lastSeen" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
            }
            .addOnFailureListener {
                _state.value = AuthState.Error(it.localizedMessage ?: "Innlogging feilet")
            }
    }

    suspend fun saveFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
    }

    suspend fun loadProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val snap = firestore.collection("users").document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
    }

    fun signOut() {
        auth.signOut()
        _state.value = AuthState.Idle
    }
}
