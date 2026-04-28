package com.example.foodtracker.data.chat

import android.content.Context
import com.example.foodtracker.ui.screen.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

class ChatHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MESSAGES = "messages"
        private const val MAX_MESSAGES = 200
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val capped = if (messages.size > MAX_MESSAGES) messages.takeLast(MAX_MESSAGES) else messages
        val json = JSONArray()
        capped.forEach { msg ->
            json.put(JSONObject().apply {
                put("id", msg.id)
                put("text", msg.text)
                put("isUser", msg.isUser)
                put("timestamp", msg.timestamp)
            })
        }
        prefs.edit().putString(KEY_MESSAGES, json.toString()).apply()
    }

    fun loadMessages(): List<ChatMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ChatMessage(
                    id        = obj.getString("id"),
                    text      = obj.getString("text"),
                    isUser    = obj.getBoolean("isUser"),
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }

    fun hasHistory(): Boolean = prefs.contains(KEY_MESSAGES)
}
