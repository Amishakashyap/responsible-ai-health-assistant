package com.example.foodtracker.domain.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages downloading, storage and lifecycle of the on-device LLM model file.
 * Model is stored in app-private internal storage (no extra permissions needed).
 */
class OnDeviceModelManager(private val context: Context) {

    companion object {
        private const val TAG = "OnDeviceModel"
        private const val MODELS_DIR = "models"
        private const val MODEL_FILENAME = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"
        // Gemma3 1B Instruct – int4 quantized, 2048 KV-cache, good quality on modern phones
        // Requires HuggingFace token (gated model – user must accept Gemma license at huggingface.co)
        const val MODEL_URL =
            "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"
        const val MODEL_SIZE_BYTES = 555_000_000L // ~555 MB
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progressPercent: Int) : DownloadState()
        object Completed : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    @Volatile
    private var cancelled = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val config = AIConfig(context)

    /** Directory where models are stored. */
    private fun modelsDir(): File =
        File(context.filesDir, MODELS_DIR).also { if (!it.exists()) it.mkdirs() }

    /** Full path to the model file on disk. */
    fun modelFile(): File = File(modelsDir(), MODEL_FILENAME)

    /** Whether the model has already been downloaded. */
    fun isModelDownloaded(): Boolean {
        val f = modelFile()
        return f.exists() && f.length() > 400_000_000 // > 400 MB sanity check for Gemma3-1B
    }

    /** Human-readable size of the downloaded model. */
    fun modelSizeMb(): Int = if (isModelDownloaded()) (modelFile().length() / 1_048_576).toInt() else 0

    /** Download the model with progress reporting.  Call from a coroutine. */
    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) {
            markReady()
            _state.value = DownloadState.Completed
            return@withContext true
        }
        cancelled = false
        _state.value = DownloadState.Downloading(0)
        val tmpFile = File(modelsDir(), "$MODEL_FILENAME.tmp")
        try {
            val hfToken = config.huggingFaceToken
            val requestBuilder = Request.Builder().url(MODEL_URL)
            if (hfToken.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $hfToken")
            }
            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val msg = when (response.code) {
                        401 -> "Invalid HuggingFace token. Check your token at huggingface.co/settings/tokens"
                        403 -> "Access denied. Accept the Gemma license at huggingface.co/litert-community/Gemma3-1B-IT then retry"
                        404 -> "Model file not found. The download URL may have changed."
                        else -> "HTTP ${response.code}"
                    }
                    _state.value = DownloadState.Error(msg)
                    return@withContext false
                }
                val body = response.body ?: run {
                    _state.value = DownloadState.Error("Empty response")
                    return@withContext false
                }
                val contentLength = body.contentLength().takeIf { it > 0 } ?: MODEL_SIZE_BYTES
                tmpFile.outputStream().buffered().use { out ->
                    val buf = ByteArray(131_072) // 128 KB buffer
                    var totalRead = 0L
                    body.byteStream().use { input ->
                        while (true) {
                            if (cancelled) {
                                tmpFile.delete()
                                _state.value = DownloadState.Idle
                                return@withContext false
                            }
                            val read = input.read(buf)
                            if (read == -1) break
                            out.write(buf, 0, read)
                            totalRead += read
                            val pct = (totalRead * 100 / contentLength).toInt().coerceIn(0, 99)
                            _state.value = DownloadState.Downloading(pct)
                        }
                    }
                }
            }
            // Rename tmp → final
            if (tmpFile.exists()) {
                val dest = modelFile()
                if (dest.exists()) dest.delete()
                tmpFile.renameTo(dest)
            }
            markReady()
            _state.value = DownloadState.Completed
            Log.i(TAG, "Model downloaded: ${modelFile().length() / 1_048_576} MB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            tmpFile.delete()
            _state.value = DownloadState.Error(e.message ?: "Unknown error")
            false
        }
    }

    /** Cancel an in-progress download. */
    fun cancelDownload() {
        cancelled = true
    }

    /** Delete the downloaded model to free storage. */
    fun deleteModel() {
        modelFile().delete()
        File(modelsDir(), "$MODEL_FILENAME.tmp").delete()
        config.onDeviceModelPath = ""
        config.isOnDeviceReady = false
        _state.value = DownloadState.Idle
    }

    private fun markReady() {
        config.onDeviceModelPath = modelFile().absolutePath
        config.isOnDeviceReady = true
    }
}
