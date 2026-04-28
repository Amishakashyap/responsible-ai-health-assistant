package com.example.foodtracker.ui.screen

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.domain.ai.AIConfig
import com.example.foodtracker.domain.ai.AIProvider
import com.example.foodtracker.domain.ai.OnDeviceModelManager
import com.example.foodtracker.domain.health.DailySummaryScheduler
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aiConfig = remember { AIConfig(context) }
    val scope = rememberCoroutineScope()
    
    var isEnabled by remember { mutableStateOf(aiConfig.isEnabled) }
    var apiKey by remember { mutableStateOf(aiConfig.openAIKey.ifEmpty { aiConfig.geminiKey }) }
    var ollamaUrl by remember { mutableStateOf(aiConfig.ollamaUrl) }
    var ollamaModel by remember { mutableStateOf(aiConfig.ollamaModel) }
    var ollamaTestResult by remember { mutableStateOf("") }
    var summaryHour by remember { mutableStateOf(aiConfig.summaryHour) }
    var summaryMinute by remember { mutableStateOf(aiConfig.summaryMinute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    val activeProvider by remember { derivedStateOf { aiConfig.getActiveProvider() } }
    
    // On-device model state
    val modelManager = remember { OnDeviceModelManager(context) }
    val downloadState by modelManager.state.collectAsState()
    var isModelDownloaded by remember { mutableStateOf(modelManager.isModelDownloaded()) }
    var modelSizeMb by remember { mutableStateOf(modelManager.modelSizeMb()) }
    var hfToken by remember { mutableStateOf(aiConfig.huggingFaceToken) }
    
    // Ollama network scan state
    var scanResult by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Health Assistant Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Enable/Disable AI Features
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "AI Health Insights",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Get personalized recommendations",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            aiConfig.isEnabled = it
                            if (it) {
                                DailySummaryScheduler.scheduleDailySummary(context)
                            } else {
                                DailySummaryScheduler.cancelDailySummary(context)
                            }
                        }
                    )
                }
            }
            
            if (isEnabled) {
                // Current AI Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Active AI",
                                fontSize = 12.sp,
                                color = AppTextSecondary
                            )
                            Text(
                                activeProvider,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key Input (Optional - for premium AI)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "AI API Key (Optional)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add OpenAI or Gemini key for premium AI. Works without key using smart rules.",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Key") },
                            placeholder = { Text("sk-... or AIza...") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "💡 OpenAI: platform.openai.com | Gemini: makersuite.google.com",
                            fontSize = 10.sp,
                            color = AppTextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Ollama (Local LLM) Configuration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🦙 Ollama (PC → Phone over WiFi)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Connect to Ollama running on your PC. Both devices must be on the same WiFi network.",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = ollamaUrl,
                            onValueChange = { ollamaUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ollama Server URL") },
                            placeholder = { Text("http://192.168.x.x:11434") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Find Ollama on network button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isScanning = true
                                    scanResult = "🔍 Scanning your WiFi network..."
                                    val found = scanForOllama(context)
                                    if (found != null) {
                                        ollamaUrl = found
                                        scanResult = "✅ Found Ollama at $found"
                                    } else {
                                        scanResult = "❌ Ollama not found on this network. Make sure:\n• Ollama is running on your PC\n• PC and phone are on same WiFi\n• Run on PC: setx OLLAMA_HOST 0.0.0.0"
                                    }
                                    isScanning = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isScanning
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isScanning) "Scanning..." else "Find Ollama on WiFi")
                        }
                        
                        if (scanResult.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                scanResult,
                                fontSize = 12.sp,
                                color = if (scanResult.startsWith("✅")) Color(0xFF2E7D32) else if (scanResult.startsWith("❌")) Color(0xFFB71C1C) else Color(0xFF666666)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ollamaModel,
                            onValueChange = { ollamaModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model Name") },
                            placeholder = { Text("llama3.2:3b") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ollamaTestResult = "⏳ Testing..."
                                    try {
                                        val url = ollamaUrl.trimEnd('/').ifEmpty { "http://10.0.2.2:11434" }
                                        val result = withContext(Dispatchers.IO) {
                                            val client = okhttp3.OkHttpClient.Builder()
                                                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                                .build()
                                            val req = okhttp3.Request.Builder().url("$url/api/tags").get().build()
                                            client.newCall(req).execute().use { resp ->
                                                if (resp.isSuccessful) resp.body?.string() ?: "" else "HTTP ${resp.code}"
                                            }
                                        }
                                        if (result.contains("models")) {
                                            ollamaTestResult = "✅ Connected! Ollama is running."
                                        } else {
                                            ollamaTestResult = "❌ Failed: $result"
                                        }
                                    } catch (e: Exception) {
                                        ollamaTestResult = "❌ Cannot reach Ollama: ${e.message?.take(60)}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                        if (ollamaTestResult.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                ollamaTestResult,
                                fontSize = 12.sp,
                                color = if (ollamaTestResult.startsWith("✅")) Color(0xFF2E7D32) else Color(0xFFB71C1C)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "📋 Setup: Install Ollama on PC → Run: setx OLLAMA_HOST 0.0.0.0 → Restart Ollama → Tap 'Find Ollama on WiFi' above",
                            fontSize = 10.sp,
                            color = AppTextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // On-Device AI Model
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📱 On-Device AI Model",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Download Gemma 1B AI model (~555 MB) to run directly on your phone. No internet needed after download.",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // HuggingFace token (required for gated Gemma model)
                        OutlinedTextField(
                            value = hfToken,
                            onValueChange = { hfToken = it },
                            label = { Text("HuggingFace Token") },
                            placeholder = { Text("hf_...") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            "Required: 1) Accept license at huggingface.co/litert-community/Gemma3-1B-IT  2) Get token at huggingface.co/settings/tokens",
                            fontSize = 11.sp,
                            color = AppTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (val state = downloadState) {
                            is OnDeviceModelManager.DownloadState.Idle -> {
                                if (isModelDownloaded) {
                                    // Model ready
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8F5E9)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "Model ready ✓",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    "${modelSizeMb} MB on device",
                                                    fontSize = 12.sp,
                                                    color = AppTextSecondary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            modelManager.deleteModel()
                                            isModelDownloaded = false
                                            modelSizeMb = 0
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFB71C1C)
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete Model (free ${modelSizeMb} MB)")
                                    }
                                } else {
                                    // Not downloaded
                                    Button(
                                        onClick = {
                                            // Save HF token before download
                                            aiConfig.huggingFaceToken = hfToken.trim()
                                            scope.launch {
                                                val ok = modelManager.downloadModel()
                                                isModelDownloaded = ok
                                                modelSizeMb = modelManager.modelSizeMb()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        enabled = hfToken.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download AI Model (~555 MB)")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "⚠️ WiFi recommended. One-time download.",
                                        fontSize = 11.sp,
                                        color = AppTextSecondary
                                    )
                                }
                            }
                            is OnDeviceModelManager.DownloadState.Downloading -> {
                                Text(
                                    "Downloading... ${state.progressPercent}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { state.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { modelManager.cancelDownload() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel")
                                }
                            }
                            is OnDeviceModelManager.DownloadState.Completed -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE8F5E9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Download complete! Model is ready.",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                            is OnDeviceModelManager.DownloadState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFEBEE)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFB71C1C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Error: ${state.message}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFB71C1C)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val ok = modelManager.downloadModel()
                                            isModelDownloaded = ok
                                            modelSizeMb = modelManager.modelSizeMb()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Download")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Daily Summary Time
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Daily Summary Time",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "When to generate your daily health insights",
                            fontSize = 12.sp,
                            color = AppTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${String.format("%02d", summaryHour)}:${String.format("%02d", summaryMinute)}")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save Button
                Button(
                    onClick = {
                        // Auto-detect and save API key to correct provider
                        when {
                            apiKey.startsWith("sk-") -> {
                                aiConfig.openAIKey = apiKey
                                aiConfig.geminiKey = ""
                            }
                            apiKey.startsWith("AIza") -> {
                                aiConfig.geminiKey = apiKey
                                aiConfig.openAIKey = ""
                            }
                            apiKey.isEmpty() -> {
                                aiConfig.openAIKey = ""
                                aiConfig.geminiKey = ""
                            }
                        }
                        
                        // Save Ollama config
                        aiConfig.ollamaUrl = ollamaUrl.trim()
                        if (ollamaModel.isNotBlank()) aiConfig.ollamaModel = ollamaModel.trim()
                        aiConfig.huggingFaceToken = hfToken.trim()
                        
                        aiConfig.isEnabled = isEnabled
                        aiConfig.summaryHour = summaryHour
                        aiConfig.summaryMinute = summaryMinute
                        
                        // Reschedule worker
                        if (isEnabled) {
                            DailySummaryScheduler.scheduleDailySummary(context)
                        }
                        
                        val providerUsed = when {
                            apiKey.startsWith("sk-") -> "OpenAI GPT-4"
                            apiKey.startsWith("AIza") -> "Google Gemini"
                            ollamaUrl.isNotBlank() -> "Ollama (${aiConfig.ollamaModel})"
                            else -> "Smart Rules Engine"
                        }
                        
                        saveMessage = "✓ Saved! Using $providerUsed • Daily insights at ${String.format("%02d:%02d", summaryHour, summaryMinute)}"
                        
                        scope.launch {
                            kotlinx.coroutines.delay(3000)
                            saveMessage = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Settings")
                }
                
                if (saveMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                saveMessage,
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Test Generate Button
                OutlinedButton(
                    onClick = {
                        DailySummaryScheduler.generateNow(context)
                        saveMessage = "Generating summary now... Check the Daily Insights screen in a moment."
                        scope.launch {
                            kotlinx.coroutines.delay(3000)
                            saveMessage = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Summary Now (Test)")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            aiConfig.getStatusMessage(),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        // Simple time input dialog (you can replace with a proper time picker)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Daily Summary Time") },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = summaryHour.toString(),
                            onValueChange = { 
                                it.toIntOrNull()?.let { hour ->
                                    if (hour in 0..23) summaryHour = hour
                                }
                            },
                            label = { Text("Hour") },
                            modifier = Modifier.width(80.dp)
                        )
                        Text(":")
                        OutlinedTextField(
                            value = summaryMinute.toString(),
                            onValueChange = { 
                                it.toIntOrNull()?.let { minute ->
                                    if (minute in 0..59) summaryMinute = minute
                                }
                            },
                            label = { Text("Minute") },
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Scans the local WiFi network for an Ollama server on port 11434.
 * Gets the phone's WiFi IP, determines the subnet, and checks each IP in parallel.
 */
private suspend fun scanForOllama(context: Context): String? = withContext(Dispatchers.IO) {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    val wifiInfo = wifiManager?.connectionInfo
    val ipInt = wifiInfo?.ipAddress ?: 0
    if (ipInt == 0) return@withContext null

    // Convert int IP to parts (Android stores it in little-endian)
    val ip0 = ipInt and 0xFF
    val ip1 = (ipInt shr 8) and 0xFF
    val ip2 = (ipInt shr 16) and 0xFF
    val subnet = "$ip0.$ip1.$ip2"

    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(800, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(800, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    // Scan IPs 1-254 in parallel batches
    val results = (1..254).map { i ->
        async {
            val ip = "$subnet.$i"
            try {
                val req = okhttp3.Request.Builder()
                    .url("http://$ip:11434/api/tags")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful && (resp.body?.string() ?: "").contains("models")) {
                        "http://$ip:11434"
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }.awaitAll()

    results.firstOrNull { it != null }
}
