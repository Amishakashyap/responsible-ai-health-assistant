package com.example.foodtracker.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.user.UserPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class HealthDataCard(
    val title: String,
    val value: String,
    val unit: String,
    val icon: String,
    val color: Color,
    val route: String,
    val isPinned: Boolean = false
)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    padding: PaddingValues = PaddingValues(0.dp),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val db = remember { com.example.foodtracker.data.db.AppDatabase.get(context) }
    
    var totalCaloriesToday by remember { mutableStateOf(0.0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Load calories for the current user
    LaunchedEffect(Unit) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entries = db.entryDao().getByDateAndUser(today, userPrefs.userId)
            var sum = 0.0
            entries.forEach { entry ->
                val food = db.foodDao().getById(entry.foodId)
                if (food != null) {
                    val factor = entry.quantityG / 100.0
                    sum += (food.calories ?: 0.0) * factor
                }
            }
            totalCaloriesToday = sum
        } catch (e: Exception) {
            android.util.Log.e("DashboardScreen", "Error loading calories", e)
        }
    }
    
    // Get greeting based on time of day
    val greeting = remember {
        val hour = LocalDateTime.now().hour
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    
    // Get username
    val username = if (userPrefs.userId > 0L) {
        userPrefs.nickname.ifEmpty { "User" }
    } else {
        "Guest"
    }
    
    // Quick Access Cards
    val quickAccessCards = remember {
        listOf(
            HealthDataCard("AI Health Insights", "", "", "🤖", Color(0xFF7C3AED), "aiHealthInsights"),
            HealthDataCard("AI Settings", "", "", "⚙️", Color(0xFF6366F1), "aiSettings"),
            HealthDataCard("Reminders", "", "", "🔔", Color(0xFFEC4899), "reminders"),
            HealthDataCard("My Streak", "", "", "🔥", Color(0xFFF97316), "streak"),
            HealthDataCard("Food Analysis", "", "", "🍽️", Color(0xFF0EA5E9), "analytics"),
            HealthDataCard("Calories Count", "", "", "🔥", Color(0xFFEF4444), "personalizedCalories"),
            HealthDataCard("Meal Tracker", "", "", "📋", Color(0xFF10B981), "mealTracker"),
            HealthDataCard("Diet Plan", "", "", "🥗", Color(0xFF22C55E), "dietPlan"),
            HealthDataCard("Weight Tracker", "", "", "⚖️", Color(0xFF3B82F6), "weightTracker"),
            HealthDataCard("Steps Tracker", "", "", "👟", Color(0xFF8B5CF6), "stepsTracker"),
            HealthDataCard("Water Tracker", "", "", "💧", Color(0xFF06B6D4), "water")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── PREMIUM HERO HEADER ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D0D0D),
                            Color(0xFF141B2E),
                            Color(0xFF003A32)
                        )
                    )
                )
                .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Column {
                // Top row: avatar + name + logout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                username.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(
                                "$greeting 👋",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                username,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Big calorie number
                Text(
                    "TODAY'S CALORIES",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${totalCaloriesToday.toInt()} kcal",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Your fitness journey starts here",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(22.dp))

                // View Stats CTA
                OutlinedButton(
                    onClick = { onNavigate("analytics") },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("View Statistics", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─── DATE CARD ───────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            "TODAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE")),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ─── QUICK ACCESS HEADER ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "QUICK ACCESS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Text(
                "${quickAccessCards.size} tools",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        // ─── QUICK ACCESS GRID ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickAccessCards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowCards.forEach { card ->
                        QuickAccessCard(
                            card = card,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun CalendarView(
    modifier: Modifier = Modifier
) {
    val currentDate = LocalDate.now()
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Column(modifier = modifier) {
        // Month and Year
        Text(
            text = currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5B9BD5),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Calendar grid (simplified)
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = currentDate.lengthOfMonth()
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > daysInMonth) break
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (day in 0..6) {
                        val shouldShowDay = (week == 0 && day >= startDayOfWeek) || (week > 0)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    color = if (shouldShowDay && dayCounter == currentDate.dayOfMonth && dayCounter <= daysInMonth) 
                                        Color(0xFF5B9BD5) 
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shouldShowDay && dayCounter <= daysInMonth) {
                                Text(
                                    text = dayCounter.toString(),
                                    fontSize = 14.sp,
                                    color = if (dayCounter == currentDate.dayOfMonth) 
                                        Color.White 
                                    else Color(0xFF2D2D2D),
                                    fontWeight = if (dayCounter == currentDate.dayOfMonth) 
                                        FontWeight.Bold 
                                    else FontWeight.Normal
                                )
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    card: HealthDataCard,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clickable { onNavigate(card.route) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle gradient background tinted with card color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.surface, card.color.copy(alpha = 0.14f))
                        )
                    )
            )
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        card.color,
                        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(card.color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(card.icon, fontSize = 22.sp)
                }
                Column {
                    Text(
                        card.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Open",
                            fontSize = 11.sp,
                            color = card.color,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = card.color,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HealthDataCardItem(
    card: HealthDataCard,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(150.dp)
            .clickable { onNavigate(card.route) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.icon,
                    fontSize = 28.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Column {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    color = card.color,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = card.value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = card.unit,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
