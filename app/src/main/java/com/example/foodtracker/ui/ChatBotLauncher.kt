package com.example.foodtracker.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.example.foodtracker.ui.screen.AITrainerChatScreen

@Composable
fun ChatBotLauncher(
    padding: PaddingValues = PaddingValues(),
    onOpenSettings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    AITrainerChatScreen(padding, onOpenSettings, onNavigateToHome)
}
