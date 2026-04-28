package com.example.foodtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.screen.*

sealed class Dest(val route: String, val label: String = "") {
    data object Splash : Dest("splash")
    data object GoalSelection : Dest("goalSelection")
    data object InitialOptions : Dest("initialOptions")
    data object Login : Dest("login")
    data object Register : Dest("register")
    data object GuestSetup : Dest("guestSetup")
    data object ProfileSetup : Dest("profileSetup")
    data object Dashboard : Dest("dashboard", "Home")
    data object AddFood : Dest("addFood", "Food")
    data object Analytics : Dest("analytics", "Analytics")
    data object Summary : Dest("summary", "Summary")
    data object BMI : Dest("bmi", "BMI")
    data object WaterTracking : Dest("water", "Water")
    data object News : Dest("news", "News")
    data object Calories : Dest("calories")
    data object MealTracker : Dest("mealTracker", "Meal Tracker")
    data object Muscle : Dest("muscle")
    data object DietPlan : Dest("dietPlan", "Diet Plan")
    data object WeightTracker : Dest("weightTracker", "Weight Tracker")
    data object StepsTracker : Dest("stepsTracker", "Steps Tracker")
    data object Profile : Dest("profile", "Profile")
    
    // New features: Streak and Reminders
    data object Streak : Dest("streak", "Streak")
    data object Reminders : Dest("reminders", "Reminders")
    
    // New BMI-based features
    data object EnhancedProfileSetup : Dest("enhancedProfileSetup")
    data object PersonalizedCalories : Dest("personalizedCalories", "My Nutrition")
    
    // AI Health Assistant features
    data object AIHealthInsights : Dest("aiHealthInsights", "Health Insights")
    data object AISettings : Dest("aiSettings", "AI Settings")

    object ChatBot : Dest("chatbot", "Chat")
}

@Composable
fun AppNav() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences(context) }
    val navController = rememberNavController()
    var currentUserId by remember { mutableStateOf<Long?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom bar should be shown
    val showBottomBar = currentRoute !in listOf(
        Dest.Splash.route,
        Dest.GoalSelection.route,
        Dest.Login.route,
        Dest.Register.route,
        Dest.GuestSetup.route,
        Dest.EnhancedProfileSetup.route
    )
    
    // Clear guest data on app startup
    LaunchedEffect(Unit) {
        if (userPrefs.isGuest()) {
            userPrefs.clearGuestData()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Splash.route
        ) {
            composable(Dest.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        // Check if user is logged in
                        val userId = userPrefs.userId
                        if (userId > 0) {
                            // User is logged in, go to Dashboard
                            navController.navigate(Dest.Dashboard.route) {
                                popUpTo(Dest.Splash.route) { inclusive = true }
                            }
                        } else {
                            // Not logged in, go to Goal Selection
                            navController.navigate(Dest.GoalSelection.route) {
                                popUpTo(Dest.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Dest.GoalSelection.route) {
                GoalSelectionScreen(
                    onComplete = {
                        navController.navigate(Dest.Login.route)
                    }
                )
            }
            composable(Dest.InitialOptions.route) {
                InitialOptionsScreen(navController)
            }
            composable(Dest.Login.route) {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        currentUserId = userId
                        navController.navigate(Dest.Dashboard.route) {
                            popUpTo(Dest.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Dest.Register.route) },
                    onNavigateToGuestSetup = { navController.navigate(Dest.GuestSetup.route) }
                )
            }
            composable(Dest.GuestSetup.route) {
                GuestSetupScreen(
                    onSetupComplete = {
                        currentUserId = 0L
                        navController.navigate(Dest.Dashboard.route) {
                            popUpTo(0) { inclusive = true }  // Clear entire back stack
                        }
                    }
                )
            }
            composable(Dest.Register.route) {
                RegistrationScreen(
                    onRegisterSuccess = { userId ->
                        currentUserId = userId
                        navController.navigate(Dest.EnhancedProfileSetup.route) {
                            popUpTo(Dest.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Dest.ProfileSetup.route) {
                ProfileSetupScreen(
                    userId = currentUserId ?: 1L,
                    onComplete = {
                        navController.navigate(Dest.Dashboard.route) {
                            popUpTo(Dest.ProfileSetup.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Dest.EnhancedProfileSetup.route) {
                EnhancedProfileSetupScreen(
                    onComplete = {
                        navController.navigate(Dest.Dashboard.route) {
                            popUpTo(Dest.EnhancedProfileSetup.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Dest.Dashboard.route) {
                DashboardScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    padding = padding,
                    onLogout = {
                        navController.navigate(Dest.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Dest.AddFood.route) { AddFoodScreen(padding, currentUserId ?: 1L) }
            composable(Dest.Analytics.route) { AnalyticsScreen(padding) }
            composable(Dest.Summary.route) { SummaryScreen(padding) }
            composable(Dest.BMI.route) { BMICalculatorScreen(padding) }
            composable(Dest.Calories.route) { 
                CaloriesCountScreen(
                    padding = padding,
                    onNavigateToMealTracker = { navController.navigate(Dest.MealTracker.route) },
                    onNavigateToAddFood = { navController.navigate(Dest.AddFood.route) }
                )
            }
            composable(Dest.MealTracker.route) { MealTrackerScreen(padding) }
            composable(Dest.WaterTracking.route) { WaterTrackingScreen(padding) }
            composable(Dest.Muscle.route) { MuscleGainScreen(padding) }
            composable(Dest.DietPlan.route) { DietPlanScreen(padding) }
            composable(Dest.WeightTracker.route) { WeightTrackerScreen(padding) }
            composable(Dest.StepsTracker.route) { StepsTrackerScreen(padding) }
            composable(Dest.News.route) { NewsScreen(padding) }
            composable(Dest.Profile.route) { 
                ProfileScreen(
                    padding = padding,
                    onLogout = {
                        navController.navigate(Dest.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAISettings = {
                        navController.navigate(Dest.AISettings.route)
                    },
                    onNavigateToEditProfile = {
                        navController.navigate(Dest.EnhancedProfileSetup.route)
                    },
                    onNavigateToReminders = {
                        navController.navigate(Dest.Reminders.route)
                    }
                )
            }
            
            // New features navigation
            composable(Dest.Streak.route) { 
                StreakScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.Reminders.route) { 
                RemindersScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.PersonalizedCalories.route) {
                PersonalizedCaloriesScreen(
                    padding = padding,
                    onNavigateToAddFood = { navController.navigate(Dest.AddFood.route) }
                )
            }
            
            // AI Health Assistant screens
            composable(Dest.AIHealthInsights.route) {
                DailyHealthSummaryScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.AISettings.route) {
                AISettingsScreen(onBack = { navController.popBackStack() })
            }
            
            composable("chatbot") {
                ChatBotLauncher(
                    padding = padding,
                    onOpenSettings = { navController.navigate(Dest.AISettings.route) },
                    onNavigateToHome = {
                        navController.navigate(Dest.Dashboard.route) {
                            popUpTo(Dest.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val items = listOf(Dest.Dashboard, Dest.ChatBot, Dest.AddFood, Dest.Profile, Dest.News)
    val scheme = MaterialTheme.colorScheme
    
    NavigationBar(
        containerColor = scheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { dest ->
            val isSelected = currentRoute == dest.route
            
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(Dest.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(if (dest == Dest.AddFood) 56.dp else 48.dp)
                            .background(
                                color = when {
                                    dest == Dest.AddFood -> scheme.primary
                                    isSelected -> scheme.primary.copy(alpha = 0.14f)
                                    else -> Color.Transparent
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (dest) {
                                Dest.Dashboard -> Icons.Default.Home
                                Dest.AddFood -> Icons.Default.Add
                                Dest.ChatBot -> Icons.Default.Star
                                Dest.Profile -> Icons.Default.Person
                                Dest.News -> Icons.Default.Notifications
                                else -> Icons.Default.Home
                            },
                            contentDescription = dest.label,
                            tint = when {
                                dest == Dest.AddFood -> scheme.onPrimary
                                isSelected -> scheme.primary
                                else -> scheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(
                                if (dest == Dest.AddFood) 28.dp else 26.dp
                            )
                        )
                    }
                },
                label = {
                    if (dest != Dest.AddFood) {
                        Text(
                            text = when (dest) {
                                Dest.Dashboard -> "Home"
                                Dest.ChatBot -> "AI Trainer"
                                Dest.Profile -> "Profile"
                                Dest.News -> "News"
                                else -> dest.label
                            },
                            fontSize = 11.sp,
                            color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.primary,
                    selectedTextColor = scheme.primary,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, padding: PaddingValues) {
    Column(
        Modifier.padding(padding).padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("This feature is under development.")
    }
}
