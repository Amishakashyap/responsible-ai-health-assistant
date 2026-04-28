package com.example.foodtracker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.DailyNutritionLog
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppPrimary
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ProteinColor = Color(0xFFFF6B6B)
private val CarbsColor   = Color(0xFFFFD93D)
private val FatColor     = Color(0xFF6BCB77)
private val WaterColor   = Color(0xFF4D96FF)
private val FiberColor   = Color(0xFFAB87FF)

data class MealSummary(
    val mealType: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val sodium: Double
)

data class FoodEntry(
    val name: String,
    val quantityG: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Composable
fun AnalyticsScreen(padding: PaddingValues = PaddingValues(0.dp)) {
    val context   = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db        = remember { AppDatabase.get(context) }
    val scope     = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var mealItems    by remember { mutableStateOf<Map<String, List<FoodEntry>>>(emptyMap()) }
    var mealTotals   by remember { mutableStateOf<List<MealSummary>>(emptyList()) }
    var dailyLog     by remember { mutableStateOf<DailyNutritionLog?>(null) }
    var weeklyLogs   by remember { mutableStateOf<List<DailyNutritionLog>>(emptyList()) }
    var isLoading    by remember { mutableStateOf(true) }
    var expandedMeal by remember { mutableStateOf<String?>(null) }

    val calorieGoal = userPrefs.calorieGoal
    val proteinGoal = userPrefs.proteinGoal
    val carbsGoal   = userPrefs.carbsGoal
    val fatGoal     = userPrefs.fatGoal
    val waterGoal   = userPrefs.waterGoal

    LaunchedEffect(selectedDate) {
        isLoading = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val entries = db.entryDao().getByDateAndUser(dateStr, userPrefs.userId)

                    val itemsByMeal  = mutableMapOf<String, MutableList<FoodEntry>>()
                    val totalsByMeal = mutableMapOf<String, DoubleArray>() // [cal, prot, fat, carbs, fiber, sodium]

                    entries.forEach { entry ->
                        val food = db.foodDao().getById(entry.foodId) ?: return@forEach
                        val f    = entry.quantityG / 100.0
                        val cal  = (food.calories  ?: 0.0) * f
                        val prot = (food.proteinG  ?: 0.0) * f
                        val fat  = (food.fatG      ?: 0.0) * f
                        val carb = (food.carbsG    ?: 0.0) * f
                        val fib  = (food.fiberG    ?: 0.0) * f
                        val sod  = (food.sodiumMg  ?: 0.0) * f
                        itemsByMeal.getOrPut(entry.mealType) { mutableListOf() }
                            .add(FoodEntry(food.name, entry.quantityG, cal, prot, carb, fat))
                        val arr = totalsByMeal.getOrPut(entry.mealType) { DoubleArray(6) }
                        arr[0] += cal; arr[1] += prot; arr[2] += fat; arr[3] += carb; arr[4] += fib; arr[5] += sod
                    }

                    val summaries = listOf("breakfast", "lunch", "dinner", "snack").mapNotNull { m ->
                        val arr = totalsByMeal[m] ?: return@mapNotNull null
                        MealSummary(m.replaceFirstChar { it.uppercase() }, arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])
                    }

                    val log    = db.dailyNutritionLogDao().getByUserAndDate(userPrefs.userId, dateStr)
                    val recent = db.dailyNutritionLogDao().getRecentLogs(userPrefs.userId, 7)

                    mealItems  = itemsByMeal
                    mealTotals = summaries
                    dailyLog   = log
                    weeklyLogs = recent
                } catch (_: Exception) {}
            }
            isLoading = false
        }
    }

    val totalCal    = mealTotals.sumOf { it.calories }
    val totalProt   = mealTotals.sumOf { it.protein }
    val totalCarbs  = mealTotals.sumOf { it.carbs }
    val totalFat    = mealTotals.sumOf { it.fat }
    val totalFiber  = mealTotals.sumOf { it.fiber }
    val totalSodium = mealTotals.sumOf { it.sodium }
    val totalWater  = dailyLog?.waterConsumedMl ?: 0
    val isEmpty     = mealTotals.isEmpty()

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Title ─────────────────────────────────────────────────────────
        Text(
            "Food Analysis",
            style  = MaterialTheme.typography.headlineMedium,
            color  = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // ── Date navigator ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors   = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous day", tint = AppPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (selectedDate == LocalDate.now()) "Today"
                        else if (selectedDate == LocalDate.now().minusDays(1)) "Yesterday"
                        else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                        color = AppTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = { if (selectedDate < LocalDate.now()) selectedDate = selectedDate.plusDays(1) },
                    enabled = selectedDate < LocalDate.now()
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next day",
                        tint = if (selectedDate < LocalDate.now()) AppPrimary else AppTextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppPrimary)
            }
            return@Column
        }

        if (isEmpty) {
            // ── Empty state ───────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🍽️", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No meals logged", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap the + button to add food entries and track your nutrition.",
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // ── Calorie ring + macro numbers ──────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ring chart
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val calProgress = (totalCal / calorieGoal).coerceIn(0.0, 1.0).toFloat()
                        val ringStroke = 14.dp
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val stroke = ringStroke.toPx()
                            val inset  = stroke / 2
                            val oval   = Size(size.width - stroke, size.height - stroke)
                            // Track
                            drawArc(
                                color      = AppBackground,
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                style      = Stroke(width = stroke),
                                topLeft    = Offset(inset, inset), size = oval
                            )
                            // Progress
                            drawArc(
                                color      = AppPrimary,
                                startAngle = -90f, sweepAngle = calProgress * 360f, useCenter = false,
                                style      = Stroke(width = stroke, cap = StrokeCap.Round),
                                topLeft    = Offset(inset, inset), size = oval
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                totalCal.roundToInt().toString(),
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 22.sp
                            )
                            Text("kcal", color = AppTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.width(20.dp))

                    // Calorie stats + remaining
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calories", color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        CalorieStat("Goal", calorieGoal.toString(), "kcal")
                        CalorieStat("Consumed", totalCal.roundToInt().toString(), "kcal")
                        val remaining = (calorieGoal - totalCal.roundToInt()).coerceAtLeast(0)
                        CalorieStat("Remaining", remaining.toString(), "kcal",
                            valueColor = if (remaining == 0) AppPrimary else Color.White)
                        if (totalCal > calorieGoal) {
                            Text(
                                "+${(totalCal - calorieGoal).roundToInt()} over goal",
                                color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Macros card ────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Macronutrients", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(14.dp))
                    MacroProgressRow("Protein",  totalProt,  proteinGoal.toDouble(), ProteinColor, "g")
                    Spacer(Modifier.height(12.dp))
                    MacroProgressRow("Carbs",    totalCarbs, carbsGoal.toDouble(),   CarbsColor,   "g")
                    Spacer(Modifier.height(12.dp))
                    MacroProgressRow("Fat",      totalFat,   fatGoal.toDouble(),     FatColor,     "g")
                    Spacer(Modifier.height(12.dp))
                    MacroProgressRow("Fiber",    totalFiber, 30.0,                   FiberColor,   "g")
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Water card ────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("💧 Water", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${totalWater}ml / ${waterGoal}ml", color = WaterColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    val waterPct = (totalWater.toFloat() / waterGoal).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress    = { waterPct },
                        modifier    = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color       = WaterColor,
                        trackColor  = AppBackground
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (totalWater >= waterGoal) "Daily water goal met! 🎉"
                        else "${waterGoal - totalWater}ml more to reach your goal",
                        color    = AppTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Meal breakdown ────────────────────────────────────────────
            Text(
                "Meal Breakdown",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))

            listOf("breakfast", "lunch", "dinner", "snack").forEach { meal ->
                val summary = mealTotals.firstOrNull { it.mealType.lowercase() == meal }
                val items   = mealItems[meal] ?: emptyList()
                if (summary != null) {
                    MealAccordion(
                        summary    = summary,
                        items      = items,
                        isExpanded = expandedMeal == meal,
                        onToggle   = { expandedMeal = if (expandedMeal == meal) null else meal }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Sodium total ──────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🧂 Sodium", color = AppTextSecondary, fontSize = 14.sp)
                    Text(
                        "${totalSodium.roundToInt()}mg",
                        color = if (totalSodium > 2300) Color(0xFFFF6B6B) else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── Weekly trend (always shown if recent logs exist) ──────────────
        if (weeklyLogs.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("This Week", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Daily calorie intake vs goal", color = AppTextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    WeeklyBarChart(logs = weeklyLogs, calorieGoal = calorieGoal)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ── Calorie stat row ──────────────────────────────────────────────────────────
@Composable
private fun CalorieStat(label: String, value: String, unit: String, valueColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = AppTextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(" $unit", color = AppTextSecondary, fontSize = 12.sp)
    }
}

// ── Macro progress bar row ────────────────────────────────────────────────────
@Composable
private fun MacroProgressRow(label: String, value: Double, goal: Double, color: Color, unit: String) {
    val pct = if (goal > 0) (value / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                "${value.roundToInt()}$unit / ${goal.roundToInt()}$unit",
                color = AppTextSecondary, fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress   = { pct },
            modifier   = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
            color      = color,
            trackColor = AppBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${(pct * 100).roundToInt()}% of goal",
            color = if (pct >= 1f) color else AppTextSecondary,
            fontSize = 11.sp
        )
    }
}

// ── Meal accordion ────────────────────────────────────────────────────────────
@Composable
private fun MealAccordion(
    summary: MealSummary,
    items: List<FoodEntry>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val mealEmoji = when (summary.mealType.lowercase()) {
        "breakfast" -> "🌅"
        "lunch"     -> "☀️"
        "dinner"    -> "🌙"
        "snack"     -> "🍎"
        else        -> "🍽️"
    }
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(),
        colors    = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mealEmoji, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(summary.mealType, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${items.size} item${if (items.size != 1) "s" else ""}", color = AppTextSecondary, fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${summary.calories.roundToInt()} kcal",
                            color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                        )
                        Text(
                            "P:${summary.protein.roundToInt()}g  C:${summary.carbs.roundToInt()}g  F:${summary.fat.roundToInt()}g",
                            color = AppTextSecondary, fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded food list
            AnimatedVisibility(visible = isExpanded) {
                Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    HorizontalDivider(color = AppBackground, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))
                    items.forEach { food ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(food.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${food.quantityG.roundToInt()}g", color = AppTextSecondary, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${food.calories.roundToInt()} kcal",
                                    color = AppPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "P:${food.protein.roundToInt()} C:${food.carbs.roundToInt()} F:${food.fat.roundToInt()}",
                                    color = AppTextSecondary, fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Weekly bar chart ──────────────────────────────────────────────────────────
@Composable
private fun WeeklyBarChart(logs: List<DailyNutritionLog>, calorieGoal: Int) {
    if (logs.isEmpty()) return
    // Sort logs by date ascending (most recent last)
    val sorted = logs.sortedBy { it.date }
    val maxCal  = maxOf(calorieGoal.toFloat(), sorted.maxOf { it.caloriesConsumed.toFloat() }, 1f)

    val barColor   = AppPrimary
    val goalColor  = Color.White.copy(alpha = 0.3f)
    val labelColor = AppTextSecondary

    Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
        val n          = sorted.size
        val chartW     = size.width
        val chartH     = size.height - 24.dp.toPx()  // leave room for day labels
        val barW       = (chartW / n) * 0.5f
        val gap        = (chartW / n) * 0.5f / 2f

        // Goal line
        val goalY = chartH * (1f - calorieGoal.toFloat() / maxCal)
        drawLine(
            color       = goalColor,
            start       = Offset(0f, goalY),
            end         = Offset(chartW, goalY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect  = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        )

        sorted.forEachIndexed { i, log ->
            val slotCenter = (i + 0.5f) * (chartW / n)
            val x          = slotCenter - barW / 2f
            val barH       = chartH * (log.caloriesConsumed.toFloat() / maxCal)
            val y          = chartH - barH

            // Bar
            drawRoundRect(
                color    = if (log.caloriesConsumed >= calorieGoal) barColor else barColor.copy(alpha = 0.5f),
                topLeft  = Offset(x, y),
                size     = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }

    // Day labels below chart
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        sorted.forEach { log ->
            val date = runCatching { LocalDate.parse(log.date) }.getOrNull()
            Text(
                text     = date?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.take(3) ?: "-",
                color    = AppTextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        sorted.forEach { log ->
            Text(
                text      = if (log.caloriesConsumed > 0) "${log.caloriesConsumed}" else "-",
                color     = if (log.caloriesConsumed >= calorieGoal) AppPrimary else AppTextSecondary,
                fontSize  = 9.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.weight(1f)
            )
        }
    }
}
