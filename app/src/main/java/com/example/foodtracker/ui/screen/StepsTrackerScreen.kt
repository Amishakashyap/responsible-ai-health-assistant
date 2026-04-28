package com.example.foodtracker.ui.screen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

private const val PREFS_NAME = "steps_tracker_prefs"
private const val PREF_ENTRIES_KEY = "steps_entries_json"
private const val PREF_STEP_COUNT_KEY = "step_count_"

data class StepsEntry(val timestamp: Long, val steps: Int, val date: String)

// Step detector using accelerometer
class AccelerometerStepDetector {
    private var lastMagnitude = 0.0
    var stepCount = 0
    private var isMoving = false
    
    // Thresholds for step detection
    private val ACCELERATION_THRESHOLD = 15.0 // m/s²
    private val MAGNITUDE_CHANGE_THRESHOLD = 5.0
    
    fun processAccelerometerData(x: Float, y: Float, z: Float): Int {
        // Calculate magnitude of acceleration
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        
        // Detect peak in acceleration (represents a step)
        val magnitudeDifference = Math.abs(magnitude - lastMagnitude)
        
        // Check if we're experiencing significant acceleration change
        if (magnitude > ACCELERATION_THRESHOLD) {
            if (!isMoving && magnitudeDifference > MAGNITUDE_CHANGE_THRESHOLD) {
                stepCount++
                isMoving = true
            }
        } else {
            isMoving = false
        }
        
        lastMagnitude = magnitude
        return stepCount
    }
    
    fun reset() {
        stepCount = 0
        lastMagnitude = 0.0
        isMoving = false
    }
}

@Composable
fun StepsTrackerScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentSteps by remember { mutableStateOf(0) }
    var dailyGoal by remember { mutableStateOf(10000) }
    var goalInput by remember { mutableStateOf("10000") }
    var entries by remember { mutableStateOf(loadStepsEntries(prefs)) }

    // Sensor management
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val accelerometerSensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val stepDetector = remember { AccelerometerStepDetector() }
    
    val sensorEventListener = remember {
        object : SensorEventListener {
            private var lastSaveTime = 0L
            private val SAVE_INTERVAL = 5000L
            
            override fun onSensorChanged(event: SensorEvent?) {
                try {
                    if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && event.values.size >= 3) {
                        val steps = stepDetector.processAccelerometerData(event.values[0], event.values[1], event.values[2])
                        currentSteps = steps
                        
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSaveTime > SAVE_INTERVAL) {
                            val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            prefs.edit().putInt(PREF_STEP_COUNT_KEY + today, steps).apply()
                            lastSaveTime = currentTime
                        }
                    }
                } catch (e: Exception) {
                    // Silently ignore
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    LaunchedEffect(Unit) {
        try {
            dailyGoal = prefs.getInt("daily_goal", 10000)
            goalInput = dailyGoal.toString()
            
            val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            currentSteps = prefs.getInt(PREF_STEP_COUNT_KEY + today, 0)
            stepDetector.stepCount = currentSteps
            entries = loadStepsEntries(prefs)
            
            if (sensorManager != null && accelerometerSensor != null) {
                sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                sensorManager?.unregisterListener(sensorEventListener)
            } catch (e: Exception) {
                // Silently ignore
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Steps Tracker", style = MaterialTheme.typography.headlineMedium)
            }

            item {
                // Steps Display Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progress = (currentSteps.toFloat() / dailyGoal.coerceAtLeast(1)).coerceAtMost(1f)
                        
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .background(
                                    color = AppSurface,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentSteps.toString(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "steps",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            progress = { progress },
                            color = if (progress >= 1f) Color.Green else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Goal: $dailyGoal steps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${((progress * 100).toInt())}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (currentSteps >= dailyGoal) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🎉 Goal achieved!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                // Save Steps Button
                Button(
                    onClick = {
                        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        saveStepsEntry(prefs, today, currentSteps)
                        entries = loadStepsEntries(prefs)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Steps saved: $currentSteps")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Today's Steps")
                }
            }

            item {
                // Daily Goal Input
                Column {
                    Text("Update Daily Goal", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = goalInput,
                            onValueChange = { goalInput = it },
                            label = { Text("Steps goal") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val goal = goalInput.toIntOrNull()
                            if (goal == null || goal <= 0) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid number")
                                }
                            } else {
                                dailyGoal = goal
                                prefs.edit().putInt("daily_goal", goal).apply()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Daily goal updated to $goal steps")
                                }
                            }
                        }) {
                            Text("Set")
                        }
                    }
                }
            }

            item {
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    Text(
                        text = "ℹ️ Steps are detected using your device's accelerometer in real-time. Keep your phone in your pocket or carry it normally. Algorithm analyzes motion patterns to count steps accurately.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            item {
                Text("History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            if (entries.isEmpty()) {
                item {
                    Text("No entries yet. Walk around to start counting steps!")
                }
            } else {
                items(entries) { entry ->
                    Card(
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(entry.date, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${entry.steps} steps",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            val goalAchieved = entry.steps >= dailyGoal
                            Text(
                                if (goalAchieved) "✓" else "○",
                                color = if (goalAchieved) Color.Green else MaterialTheme.colorScheme.outline,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

private fun saveStepsEntry(prefs: android.content.SharedPreferences, date: String, steps: Int) {
    try {
        val json = prefs.getString(PREF_ENTRIES_KEY, "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (t: Throwable) { JSONArray() }
        
        // Check if entry for this date already exists
        var updated = false
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                if (o.getString("date") == date) {
                    o.put("steps", steps)
                    o.put("timestamp", System.currentTimeMillis())
                    updated = true
                    break
                }
            } catch (_: Throwable) { }
        }
        
        if (!updated) {
            val obj = JSONObject()
            obj.put("timestamp", System.currentTimeMillis())
            obj.put("steps", steps)
            obj.put("date", date)
            arr.put(obj)
        }
        
        prefs.edit().putString(PREF_ENTRIES_KEY, arr.toString()).apply()
    } catch (e: Exception) {
        // Silently fail
    }
}

private fun loadStepsEntries(prefs: android.content.SharedPreferences): List<StepsEntry> {
    return try {
        val json = prefs.getString(PREF_ENTRIES_KEY, "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (t: Throwable) { JSONArray() }
        val list = mutableListOf<StepsEntry>()
        
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val ts = o.optLong("timestamp", -1L)
                val steps = o.optInt("steps", 0)
                val date = o.optString("date", "")
                
                if (ts != -1L && steps > 0 && date.isNotEmpty()) {
                    list.add(StepsEntry(ts, steps, date))
                }
            } catch (_: Throwable) { }
        }
        
        list.sortedByDescending { it.timestamp }
    } catch (e: Exception) {
        emptyList()
    }
}
