package com.example.foodtracker.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    padding: PaddingValues,
    onLogout: () -> Unit = {},
    onNavigateToAISettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val userPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Get userId from UserPreferences
                val userId = userPrefs.userId
                
                if (userId > 0) {
                    // Logged in user - load from database
                    val user = db.userDao().getById(userId)
                    userName = user?.name ?: "User"
                    userEmail = user?.email ?: ""
                } else {
                    // Guest user
                    userName = "Guest"
                    userEmail = "Not logged in"
                }
            } catch (e: Exception) {
                userName = "User"
                userEmail = ""
            } finally {
                isLoading = false
            }
        }
    }
    
    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (userName.isNotEmpty()) userName.first().uppercase() else "U",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        fontSize = 14.sp,
                        color = AppTextSecondary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // User Stats
        if (userPrefs.isProfileComplete) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppSurface
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Your Stats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    ProfileStatRow("Nickname", userPrefs.nickname)
                    ProfileStatRow("BMI", "%.1f".format(userPrefs.bmi))
                    ProfileStatRow("Daily Calorie Goal", "${userPrefs.calorieGoal} cal")
                    ProfileStatRow("Protein Goal", "${userPrefs.proteinGoal}g")
                    ProfileStatRow("Activity Level", userPrefs.activityLevel)
                    ProfileStatRow("Fitness Goal", userPrefs.fitnessGoal)
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
        
        // App Settings Section
        Text(
            "App Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppSurface)
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your reminders"
                ) { onNavigateToReminders() }
                
                HorizontalDivider()
                
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = "Update your information"
                ) { onNavigateToEditProfile() }
                
                HorizontalDivider()
                
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "AI Settings",
                    subtitle = "Configure AI provider, Ollama, on-device model"
                ) { onNavigateToAISettings() }
                
                HorizontalDivider()
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App version 1.0.0"
                ) { showAboutDialog = true }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Logout Button
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
        
        Spacer(Modifier.height(16.dp))
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Health Tracker") },
            text = {
                Column {
                    Text("Version 1.0.0")
                    Spacer(Modifier.height(8.dp))
                    Text("An AI-powered health and nutrition tracking app.")
                    Spacer(Modifier.height(8.dp))
                    Text("Features: meal tracking, water tracking, weight tracking, AI health insights, and personalized nutrition goals.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = AppTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Go"
        )
    }
}
