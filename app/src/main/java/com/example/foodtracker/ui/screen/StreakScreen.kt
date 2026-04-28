package com.example.foodtracker.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.streak.StreakManager
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    val streakStats by remember { mutableStateOf(streakManager.getStreakStats()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Streak") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Streak Display
            StreakCircle(
                currentStreak = streakStats.currentStreak,
                isActiveToday = streakStats.isActiveToday
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Streak Message
            Text(
                text = getStreakMessage(streakStats.currentStreak, streakStats.isActiveToday),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    label = "Best Streak",
                    value = streakStats.bestStreak.toString(),
                    color = Color(0xFFFFD700)
                )
                
                StatCard(
                    modifier = Modifier.weight(1f),
                    iconText = "📅",
                    label = "Total Days",
                    value = streakStats.totalActiveDays.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Activity Status Card
            ActivityStatusCard(
                isActiveToday = streakStats.isActiveToday,
                lastActivityDate = streakStats.lastActivityDate
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Streak Start Info
            if (streakStats.currentStreak > 0 && streakStats.streakStartDate != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏁",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Streak Started",
                                fontSize = 14.sp,
                                color = AppTextSecondary
                            )
                            Text(
                                formatDate(streakStats.streakStartDate),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tips Card
            StreakTipsCard()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StreakCircle(currentStreak: Int, isActiveToday: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame alpha"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = if (currentStreak > 0) {
                        listOf(
                            Color(0xFFFF6B35).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
    ) {
        // Outer ring
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 16.dp.toPx()
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.1f),
                style = Stroke(width = strokeWidth)
            )
            
            if (currentStreak > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF6B35),
                            Color(0xFFFF8C42),
                            Color(0xFFFFA500),
                            Color(0xFFFF6B35)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * (currentStreak.coerceAtMost(30) / 30f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (currentStreak > 0) "🔥" else "💤",
                fontSize = 48.sp,
                modifier = if (currentStreak > 0) {
                    Modifier.alpha(flameAlpha)
                } else {
                    Modifier
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                currentStreak.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentStreak > 0) Color(0xFFFF6B35) else Color.Gray
            )
            
            Text(
                "Day${if (currentStreak != 1) "s" else ""}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            if (isActiveToday && currentStreak > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF4CAF50),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Active Today ✓",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconText: String? = null,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            } else if (iconText != null) {
                Text(
                    text = iconText,
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActivityStatusCard(isActiveToday: Boolean, lastActivityDate: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveToday) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color(0xFFFF9800).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isActiveToday) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isActiveToday) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    if (isActiveToday) "You're Active Today!" else "Not Active Yet Today",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    if (isActiveToday) 
                        "Keep up the great work!" 
                    else 
                        "Log food or steps to maintain your streak",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (lastActivityDate != null) {
                    Text(
                        "Last active: ${formatDate(lastActivityDate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StreakTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💡",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tips to Maintain Your Streak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TipItem("Log at least one meal per day")
            TipItem("Track your daily steps")
            TipItem("Set up reminders in the Reminders screen")
            TipItem("Check your progress regularly")
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "• ",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}

fun getStreakMessage(streak: Int, isActiveToday: Boolean): String {
    return when {
        streak == 0 && !isActiveToday -> "Start your streak today! 🎯"
        streak == 0 && isActiveToday -> "Great! You started your streak today! 🔥"
        streak == 1 && isActiveToday -> "One day down! Keep going! 💪"
        streak < 7 && isActiveToday -> "You're building momentum! 🚀"
        streak < 7 && !isActiveToday -> "Don't let it slip! Stay active today! ⚠️"
        streak < 30 && isActiveToday -> "Amazing consistency! Keep it up! 🌟"
        streak < 30 && !isActiveToday -> "Your ${streak}-day streak is at risk! 🔥"
        streak >= 30 && isActiveToday -> "You're a legend! $streak days strong! 👑"
        else -> "Don't lose your ${streak}-day streak! 🚨"
    }
}

fun formatDate(dateString: String?): String {
    if (dateString == null) return "Unknown"
    
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        if (date != null) outputFormat.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}
