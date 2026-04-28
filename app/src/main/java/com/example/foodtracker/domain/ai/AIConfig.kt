package com.example.foodtracker.domain.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * AI Configuration - Manages AI provider settings and API keys
 */
class AIConfig(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_OPENAI_KEY = "openai_api_key"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_OLLAMA_URL = "ollama_url"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_ENABLED = "ai_enabled"
        private const val KEY_SUMMARY_TIME_HOUR = "summary_time_hour"
        private const val KEY_SUMMARY_TIME_MINUTE = "summary_time_minute"
        private const val KEY_ON_DEVICE_MODEL_PATH = "on_device_model_path"
        private const val KEY_ON_DEVICE_READY = "on_device_ready"
        private const val KEY_PREFERRED_PROVIDER = "preferred_provider"
        private const val KEY_HF_TOKEN = "huggingface_token"
    }
    
    /**
     * AI Provider selection.
     * Respects the user's explicit [preferredProvider]; auto-detects best
     * available when no preference is saved.
     */
    val aiProvider: AIProvider
        get() {
            val preferred = preferredProvider
            if (preferred.isNotEmpty()) {
                when (preferred) {
                    "ON_DEVICE" -> if (isOnDeviceReady && onDeviceModelPath.isNotEmpty()) return AIProvider.ON_DEVICE
                    "OLLAMA"    -> if (ollamaUrl.isNotEmpty()) return AIProvider.OLLAMA
                    "OPENAI"    -> if (openAIKey.isNotEmpty()) return AIProvider.OPENAI
                    "GEMINI"    -> if (geminiKey.isNotEmpty()) return AIProvider.GEMINI
                }
            }
            return when {
                openAIKey.isNotEmpty() -> AIProvider.OPENAI
                geminiKey.isNotEmpty() -> AIProvider.GEMINI
                ollamaUrl.isNotEmpty() -> AIProvider.OLLAMA
                isOnDeviceReady && onDeviceModelPath.isNotEmpty() -> AIProvider.ON_DEVICE
                else -> AIProvider.LOCAL
            }
        }

    /** Explicitly chosen provider (empty = auto-detect). */
    var preferredProvider: String
        get() = prefs.getString(KEY_PREFERRED_PROVIDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PREFERRED_PROVIDER, value).apply()

    /** True when the on-device model has been downloaded and is ready. */
    val isOnDeviceAvailable: Boolean get() = isOnDeviceReady && onDeviceModelPath.isNotEmpty()

    /** True when an Ollama server URL has been configured. */
    val isOllamaAvailable: Boolean get() = ollamaUrl.isNotEmpty()
    
    /**
     * OpenAI API Key
     */
    var openAIKey: String
        get() = prefs.getString(KEY_OPENAI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENAI_KEY, value).apply()
    
    /**
     * Google Gemini API Key
     */
    var geminiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()
    
    /**
     * Ollama server URL (e.g. http://10.0.2.2:11434 for emulator)
     */
    var ollamaUrl: String
        get() = prefs.getString(KEY_OLLAMA_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OLLAMA_URL, value).apply()
    
    /**
     * Ollama model name (e.g. llama3.2:3b)
     */
    var ollamaModel: String
        get() = prefs.getString(KEY_OLLAMA_MODEL, "llama3.2:3b") ?: "llama3.2:3b"
        set(value) = prefs.edit().putString(KEY_OLLAMA_MODEL, value).apply()
    
    /**
     * AI features enabled/disabled
     */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    
    /**
     * Daily summary generation time (hour)
     */
    var summaryHour: Int
        get() = prefs.getInt(KEY_SUMMARY_TIME_HOUR, 7) // 7 AM default
        set(value) = prefs.edit().putInt(KEY_SUMMARY_TIME_HOUR, value).apply()
    
    /**
     * Daily summary generation time (minute)
     */
    var summaryMinute: Int
        get() = prefs.getInt(KEY_SUMMARY_TIME_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_SUMMARY_TIME_MINUTE, value).apply()

    var onDeviceModelPath: String
        get() = prefs.getString(KEY_ON_DEVICE_MODEL_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ON_DEVICE_MODEL_PATH, value).apply()

    var isOnDeviceReady: Boolean
        get() = prefs.getBoolean(KEY_ON_DEVICE_READY, false)
        set(value) = prefs.edit().putBoolean(KEY_ON_DEVICE_READY, value).apply()

    /** HuggingFace API token for downloading gated models (e.g. Gemma). */
    var huggingFaceToken: String
        get() = prefs.getString(KEY_HF_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HF_TOKEN, value).apply()
    
    fun isConfigured(): Boolean = isEnabled

    /** True when at least one external AI provider (non-LOCAL) is available. */
    fun isExternalProviderAvailable(): Boolean =
        openAIKey.isNotBlank() || geminiKey.isNotBlank() || isOllamaAvailable || isOnDeviceAvailable
    
    /**
     * Get current AI provider name for display
     */
    fun getActiveProvider(): String {
        return when (aiProvider) {
            AIProvider.ON_DEVICE -> "📱 On-Device AI (Gemma 1B)"
            AIProvider.OPENAI -> "🤖 OpenAI GPT-4"
            AIProvider.GEMINI -> "🤖 Google Gemini"
            AIProvider.OLLAMA -> "🤖 Ollama ($ollamaModel)"
            AIProvider.LOCAL -> "🤖 Smart Rules Engine"
        }
    }
    
    /**
     * Get configuration status message
     */
    fun getStatusMessage(): String {
        if (!isEnabled) return "AI features disabled"
        
        return when (aiProvider) {
            AIProvider.ON_DEVICE -> "Using On-Device AI (Gemma 1B — runs on your phone)"
            AIProvider.OPENAI -> "Using OpenAI GPT-4 (Premium AI)"
            AIProvider.GEMINI -> "Using Google Gemini (Free AI)"
            AIProvider.OLLAMA -> "Using Ollama ${ollamaModel} (Local LLM)"
            AIProvider.LOCAL -> "Using Smart Rules (No API needed)"
        }
    }
}

/**
 * Supported AI providers
 */
enum class AIProvider {
    ON_DEVICE,  // On-device LLM via MediaPipe (Gemma3 1B-IT)
    OPENAI,     // OpenAI GPT-4
    GEMINI,     // Google Gemini
    OLLAMA,     // Ollama local LLM (LLaMA, Mistral, etc.)
    LOCAL       // Rule-based fallback (always available)
}
