package no.duckya.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAt: Long = 0L

    fun startRecording(): File {
        val out = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        currentFile = out
        startedAt = System.currentTimeMillis()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(64_000)
            setOutputFile(out.absolutePath)
            prepare()
            start()
        }
        recorder = r
        return out
    }

    /** Returns (filePath, durationMs) or null if nothing was recorded. */
    fun stopRecording(): Pair<String, Long>? {
        val r = recorder ?: return null
        val f = currentFile ?: return null
        val duration = System.currentTimeMillis() - startedAt
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        return f.absolutePath to duration
    }

    fun cancelRecording() {
        recorder?.runCatching { stop() }
        recorder?.runCatching { release() }
        recorder = null
        currentFile?.delete()
        currentFile = null
    }

    /** Uploads a local audio file to Firebase Storage and returns the download URL. */
    suspend fun uploadAudio(convId: String, localPath: String): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val name = "${UUID.randomUUID()}.m4a"
        val ref = storage.reference.child("audio/$convId/$uid/$name")
        val file = File(localPath)
        val taskSnap = ref.putFile(android.net.Uri.fromFile(file)).await()
        return taskSnap.storage.downloadUrl.await().toString()
    }
}
