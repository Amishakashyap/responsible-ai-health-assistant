package com.example.foodtracker.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.User
import com.example.foodtracker.data.db.UserProfile
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import com.example.foodtracker.ui.theme.AppPrimary
import com.example.foodtracker.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    onRegisterSuccess: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val userPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    // ── Step state ─────────────────────────────────────────────────────────
    var step by remember { mutableIntStateOf(1) }   // 1 = basics, 2 = body, 3 = lifestyle

    // ── Step 1 fields ──────────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    // ── Step 2 fields ──────────────────────────────────────────────────────
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    var genderExpanded by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("") }
    val genderOptions = listOf("Male", "Female", "Other")

    var bloodGroupExpanded by remember { mutableStateOf(false) }
    var bloodGroup by remember { mutableStateOf("") }
    val bloodGroupOptions = listOf("A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−", "Don't know")

    // ── Step 3 fields ──────────────────────────────────────────────────────
    var activityLevelExpanded by remember { mutableStateOf(false) }
    var activityLevel by remember { mutableStateOf("") }
    val activityOptions = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")

    var fitnessGoal by remember { mutableStateOf("") }
    val fitnessGoalOptions = listOf("Lose Weight", "Maintain Weight", "Build Muscle", "Improve Fitness", "Eat Healthier")

    var dietType by remember { mutableStateOf("") }
    val dietTypeOptions = listOf("Omnivore", "Vegetarian", "Vegan", "Keto", "Paleo", "Mediterranean")

    var allergies by remember { mutableStateOf("") }
    var foodPreferences by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when (step) { 1 -> "Create Account"; 2 -> "Body Metrics"; else -> "Lifestyle" }) },
                navigationIcon = {
                    TextButton(onClick = { if (step == 1) onNavigateBack() else step -= 1 }) {
                        Text(if (step == 1) "Login" else "← Back", color = AppPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Progress bar ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Step $step of 3", fontSize = 12.sp, color = AppTextSecondary)
                    Text(when (step) { 1 -> "Personal Info"; 2 -> "Body Metrics"; else -> "Lifestyle" }, fontSize = 12.sp, color = AppPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (i < step) AppPrimary else AppSurface)
                        )
                    }
                }
            }

            // ── Step content (animated slide) ──────────────────────────────
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    else
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                label = "step"
            ) { currentStep ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentStep == 1) {
                        // ── STEP 1: Personal Info ──────────────────────────
                        SectionCard {
                            Text("👤  Who are you?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text("Full Name *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                            OutlinedTextField(
                                value = email, onValueChange = { email = it },
                                label = { Text("Email *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                enabled = !isLoading
                            )
                            OutlinedTextField(
                                value = nickname, onValueChange = { nickname = it },
                                label = { Text("Nickname") },
                                placeholder = { Text("What should we call you?") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank() || email.isBlank()) {
                                    errorMsg = "Name and email are required"
                                } else {
                                    errorMsg = ""
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Text("Next →", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }

                    } else if (currentStep == 2) {
                        // ── STEP 2: Body Metrics ───────────────────────────
                        SectionCard {
                            Text("⚖️  Body Metrics", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = weight, onValueChange = { weight = it },
                                    label = { Text("Weight (kg) *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    enabled = !isLoading
                                )
                                OutlinedTextField(
                                    value = height, onValueChange = { height = it },
                                    label = { Text("Height (cm) *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    enabled = !isLoading
                                )
                            }
                            OutlinedTextField(
                                value = targetWeight, onValueChange = { targetWeight = it },
                                label = { Text("Target Weight (kg)") },
                                placeholder = { Text("Optional — leave blank to keep current") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isLoading
                            )
                        }

                        SectionCard {
                            Text("📋  About You (optional)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = age, onValueChange = { age = it },
                                label = { Text("Age") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isLoading
                            )

                            // Gender dropdown
                            ExposedDropdownMenuBox(
                                expanded = genderExpanded,
                                onExpandedChange = { genderExpanded = !genderExpanded }
                            ) {
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Gender") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !isLoading
                                )
                                ExposedDropdownMenu(
                                    expanded = genderExpanded,
                                    onDismissRequest = { genderExpanded = false }
                                ) {
                                    genderOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = { gender = opt; genderExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Blood group dropdown
                            ExposedDropdownMenuBox(
                                expanded = bloodGroupExpanded,
                                onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded }
                            ) {
                                OutlinedTextField(
                                    value = bloodGroup,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Blood Group") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !isLoading
                                )
                                ExposedDropdownMenu(
                                    expanded = bloodGroupExpanded,
                                    onDismissRequest = { bloodGroupExpanded = false }
                                ) {
                                    bloodGroupOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = { bloodGroup = opt; bloodGroupExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val weightF = weight.toFloatOrNull()
                                val heightF = height.toFloatOrNull()
                                if (weightF == null || heightF == null) {
                                    errorMsg = "Weight and height are required"
                                    return@Button
                                }
                                errorMsg = ""
                                step = 3
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Text("Next →", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }

                    } else {
                        // ── STEP 3: Lifestyle & Preferences ───────────────
                        SectionCard {
                            Text("🏃  Activity & Goals", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))

                            // Activity level dropdown
                            ExposedDropdownMenuBox(
                                expanded = activityLevelExpanded,
                                onExpandedChange = { activityLevelExpanded = !activityLevelExpanded }
                            ) {
                                OutlinedTextField(
                                    value = activityLevel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Activity Level") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !isLoading
                                )
                                ExposedDropdownMenu(
                                    expanded = activityLevelExpanded,
                                    onDismissRequest = { activityLevelExpanded = false }
                                ) {
                                    activityOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = { activityLevel = opt; activityLevelExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Fitness goal chips
                            Text("Fitness Goal", fontSize = 13.sp, color = AppTextSecondary)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fitnessGoalOptions.forEach { option ->
                                    FilterChip(
                                        selected = fitnessGoal == option,
                                        onClick = { fitnessGoal = if (fitnessGoal == option) "" else option },
                                        label = { Text(option, fontSize = 13.sp) }
                                    )
                                }
                            }
                        }

                        SectionCard {
                            Text("🥗  Diet & Preferences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(12.dp))

                            // Diet type chips
                            Text("Diet Type", fontSize = 13.sp, color = AppTextSecondary)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dietTypeOptions.forEach { option ->
                                    FilterChip(
                                        selected = dietType == option,
                                        onClick = { dietType = if (dietType == option) "" else option },
                                        label = { Text(option, fontSize = 13.sp) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = allergies, onValueChange = { allergies = it },
                                label = { Text("Allergies") },
                                placeholder = { Text("e.g. peanuts, gluten, dairy") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )

                            OutlinedTextField(
                                value = foodPreferences, onValueChange = { foodPreferences = it },
                                label = { Text("Food Preferences") },
                                placeholder = { Text("e.g. Indian, spicy, no beef") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                errorMsg = ""
                                scope.launch {
                                    try {
                                        val weightF = weight.toFloat()
                                        val heightF = height.toFloat()
                                        val ageInt = age.toIntOrNull() ?: 0
                                        val targetWeightD = targetWeight.toDoubleOrNull() ?: weightF.toDouble()

                                        val user = User(
                                            email = email.trim(),
                                            name = name.trim(),
                                            city = "",
                                            gender = gender,
                                            age = ageInt,
                                            bloodGroup = bloodGroup
                                        )
                                        val userId = db.userDao().insert(user)

                                        userPrefs.userId = userId
                                        userPrefs.nickname = if (nickname.isNotBlank()) nickname.trim() else name.trim().split(" ").firstOrNull() ?: "User"
                                        userPrefs.weight = weightF
                                        userPrefs.height = heightF
                                        if (ageInt > 0) userPrefs.age = ageInt
                                        if (gender.isNotBlank()) userPrefs.gender = gender
                                        if (activityLevel.isNotBlank()) userPrefs.activityLevel = activityLevel
                                        if (fitnessGoal.isNotBlank()) userPrefs.fitnessGoal = fitnessGoal
                                        if (dietType.isNotBlank()) userPrefs.dietType = dietType.lowercase()
                                        if (allergies.isNotBlank()) userPrefs.allergies = allergies.trim()
                                        if (foodPreferences.isNotBlank()) userPrefs.foodPreferences = foodPreferences.trim()
                                        userPrefs.calculateAndSaveGoals()

                                        val goal = when {
                                            targetWeightD < weightF.toDouble() - 1.0 -> "weightloss"
                                            targetWeightD > weightF.toDouble() + 1.0 -> "gain"
                                            else -> "maintain"
                                        }
                                        db.profileDao().upsert(
                                            UserProfile(
                                                userId = userId,
                                                goal = goal,
                                                weightKg = weightF.toDouble(),
                                                heightCm = heightF.toDouble(),
                                                targetWeightKg = targetWeightD,
                                                exerciseFreq = activityLevel.ifBlank { "moderate" }
                                            )
                                        )
                                        onRegisterSuccess(userId)
                                    } catch (e: Exception) {
                                        errorMsg = "Registration failed: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Create Account 🎉", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }

                        Text(
                            "You can update all details later from Profile",
                            fontSize = 11.sp,
                            color = AppTextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

