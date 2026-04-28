package com.example.foodtracker.domain.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Wraps MediaPipe LlmInference for on-device text generation using the
 * **async** API (`generateResponseAsync`).
 *
 * Uses Gemma3-1B-IT (int4 quantized) with the Gemma chat template.
 *
 * The synchronous `generateResponse()` triggers a JNI abort (SIGABRT) when
 * the native TFLite inference fails because `nativePredictSync` calls
 * `NewByteArray` with a pending exception.  The async path returns a
 * `ListenableFuture` and delivers errors through `ExecutionException`,
 * which we can catch normally.
 */
class OnDeviceLlmEngine private constructor(
    private val inference: LlmInference,
    private val context: Context,
    private val modelPath: String
) {
    companion object {
        private const val TAG = "OnDeviceLlm"

        // Gemma3-1B-IT has a 2048-token KV-cache; budget 512 tokens for output
        private const val MAX_TOKENS = 2048
        // Characters of user content included before wrapping with the chat template.
        // Gemma3-1B can handle much longer inputs than SmolLM-135M.
        private const val MAX_USER_CHARS = 1200

        /** Strip characters that can crash the tokenizer but keep common Unicode. */
        private fun sanitize(text: String): String =
            text.replace(Regex("[\\x00-\\x1F&&[^\\n\\r\\t]]"), "")

        /**
         * Wrap user content with the Gemma-Instruct chat template.
         * Gemma uses <start_of_turn>user / <start_of_turn>model tokens.
         */
        private fun buildPrompt(userContent: String): String {
            val condensed = if (userContent.length > MAX_USER_CHARS)
                userContent.take(MAX_USER_CHARS)
            else userContent
            return "<start_of_turn>user\n" +
                   "You are a health and nutrition assistant. Answer ONLY health, nutrition, and fitness questions using the user data provided. Give specific numbers. Be concise (80-150 words). If asked anything unrelated to health, politely redirect.\n\n" +
                   "$condensed\n<end_of_turn>\n" +
                   "<start_of_turn>model\n"
        }

        /**
         * Remove Gemma template tokens that may appear in the model output.
         */
        private fun cleanOutput(raw: String): String =
            raw.replace(Regex("<(start_of_turn|end_of_turn)>\\w*\\n?"), "")
               .replace("<end_of_turn>", "")
               .replace("<start_of_turn>", "")
               .replace(Regex("<\\|im_(start|end)\\|>\\w*\\n?"), "")
               .trim()

        /** Create an engine; returns null if the device can't load the model. */
        fun create(context: Context, modelPath: String): OnDeviceLlmEngine? {
            return try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
                val llm = LlmInference.createFromOptions(context, options)
                OnDeviceLlmEngine(llm, context, modelPath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create LLM engine: ${e.message}", e)
                null
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "Not enough RAM to load model", oom)
                null
            }
        }
    }

    /**
     * Generate a response via the async API on IO dispatcher.
     * Wraps the prompt with Gemma's chat template so the model generates
     * only the model turn instead of predicting arbitrary next tokens.
     * Returns empty string on failure or timeout.
     */
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val wrapped = sanitize(buildPrompt(prompt))
        try {
            // Use the async API – it returns a ListenableFuture instead of
            // going through the buggy nativePredictSync → NewByteArray path.
            val future = inference.generateResponseAsync(wrapped)
            val raw = future.get(90, TimeUnit.SECONDS) ?: ""
            cleanOutput(raw)
        } catch (e: TimeoutException) {
            Log.w(TAG, "Generation timed out after 90s"); ""
        } catch (e: ExecutionException) {
            Log.e(TAG, "Generation failed: ${e.cause?.message}", e.cause); ""
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e); ""
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM during generation", oom); ""
        }
    }

    /** Recreate the engine (e.g. after a native error). */
    fun recreate(): OnDeviceLlmEngine? {
        close()
        return create(context, modelPath)
    }

    /** Release native resources. */
    fun close() {
        try {
            inference.close()
        } catch (_: Exception) { }
    }
}
