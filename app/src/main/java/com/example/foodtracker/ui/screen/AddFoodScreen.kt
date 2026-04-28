package com.example.foodtracker.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.db.AppDatabase
import com.example.foodtracker.data.db.Entry
import com.example.foodtracker.data.db.Food
import com.example.foodtracker.data.user.UserPreferences
import com.example.foodtracker.ui.theme.AppBackground
import com.example.foodtracker.ui.theme.AppSurface
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddFoodScreen(padding: PaddingValues, @Suppress("UNUSED_PARAMETER") userId: Long) {
    val context = LocalContext.current
    val db = AppDatabase.get(context)
    val userPrefs = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    
    // Get actual userId from preferences
    val actualUserId = userPrefs.userId
    
    var query by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("100") }
    var selectedUnit by remember { mutableStateOf("grams") } // "grams" or "pieces"
    var gramsPerPiece by remember { mutableStateOf("50") } // default weight per piece
    var searchResults by remember { mutableStateOf<List<Food>>(emptyList()) }
    var selectedMeal by remember { mutableStateOf("breakfast") }
    var message by remember { mutableStateOf("") }
    var isInitializing by remember { mutableStateOf(true) }
    var foodCount by remember { mutableStateOf(0) }
    
    // Check if database is initialized on first load
    LaunchedEffect(Unit) {
        try {
            val count = db.foodDao().searchLike("%").size
            foodCount = count
            isInitializing = false
            android.util.Log.d("AddFoodScreen", "Database has $count foods")
            if (count == 0) {
                message = "⚠️ Database is empty. Please wait..."
            }
        } catch (e: Exception) {
            android.util.Log.e("AddFoodScreen", "Error checking food count", e)
            isInitializing = false
        }
    }
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val scrollState = rememberScrollState()
    
    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Add Food Entry", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search food (English)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        
        // Unit selector (Grams or Pieces)
        Text("Measurement Unit:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedUnit == "grams",
                onClick = { 
                    selectedUnit = "grams"
                    quantity = "100"
                },
                label = { Text("Grams (g)") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedUnit == "pieces",
                onClick = { 
                    selectedUnit = "pieces"
                    quantity = "1"
                    gramsPerPiece = "50"
                },
                label = { Text("Pieces/Numbers") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // GRAMS OPTION
        if (selectedUnit == "grams") {
            Text("Quantity (grams):", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Preset quantity buttons
                    Text("Quick Select:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50, 100, 150, 200, 250).forEach { preset ->
                            FilterChip(
                                selected = quantity == preset.toString(),
                                onClick = { quantity = preset.toString() },
                                label = { Text("${preset}g") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Custom quantity with +/- buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = quantity.toIntOrNull() ?: 100
                                quantity = (current - 10).coerceAtLeast(10).toString()
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                        }
                        
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    quantity = newValue
                                }
                            },
                            label = { Text("Custom Amount (g)") },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            singleLine = true
                        )
                        
                        IconButton(
                            onClick = {
                                val current = quantity.toIntOrNull() ?: 100
                                quantity = (current + 10).toString()
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Total: ${quantity.toIntOrNull() ?: 0}g",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // PIECES OPTION
        if (selectedUnit == "pieces") {
            Text("Number of Pieces:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Preset pieces buttons
                    Text("Quick Select:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 5).forEach { preset ->
                            FilterChip(
                                selected = quantity == preset.toString(),
                                onClick = { quantity = preset.toString() },
                                label = { Text("$preset") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Custom pieces with +/- buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = quantity.toIntOrNull() ?: 1
                                quantity = (current - 1).coerceAtLeast(1).toString()
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                        }
                        
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    quantity = newValue
                                }
                            },
                            label = { Text("Number of Pieces") },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            singleLine = true
                        )
                        
                        IconButton(
                            onClick = {
                                val current = quantity.toIntOrNull() ?: 1
                                quantity = (current + 1).toString()
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Grams per piece input
                    OutlinedTextField(
                        value = gramsPerPiece,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                gramsPerPiece = newValue
                            }
                        },
                        label = { Text("Weight per piece (grams)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Approx. weight of 1 piece") }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    val pieces = quantity.toIntOrNull() ?: 1
                    val gramsEach = gramsPerPiece.toIntOrNull() ?: 50
                    val totalGrams = pieces * gramsEach
                    
                    Text(
                        "Total: $pieces piece${if (pieces > 1) "s" else ""} × ${gramsEach}g = ${totalGrams}g",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Meal type selector
        Text("Meal Type:", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("breakfast", "lunch", "dinner", "snack").forEach { meal ->
                FilterChip(
                    selected = selectedMeal == meal,
                    onClick = { selectedMeal = meal },
                    label = { Text(meal.capitalize()) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        
        // Instant search - triggered when user types (with debounce)
        LaunchedEffect(query) {
            if (query.trim().length >= 2) {
                kotlinx.coroutines.delay(300) // Debounce for 300ms
                scope.launch {
                    try {
                        val searchQuery = query.trim().lowercase()
                        val results = db.foodDao().searchLike("%${searchQuery}%")
                        android.util.Log.d("AddFoodScreen", "Instant search for '$searchQuery' found ${results.size} results")
                        searchResults = results
                        message = when {
                            results.isEmpty() -> "No foods found for \"${query.trim()}\". Try: rice, dal, chapati, poha, idli"
                            results.size == 50 -> "Showing ${results.size} results (max limit). Try a more specific search."
                            else -> "✓ Found ${results.size} food item${if (results.size > 1) "s" else ""}"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AddFoodScreen", "Search error", e)
                        message = "Search error: ${e.message}"
                        searchResults = emptyList()
                    }
                }
            } else if (query.trim().isEmpty()) {
                searchResults = emptyList()
                message = ""
            } else {
                message = "Type at least 2 characters to search..."
            }
        }
        
        if (message.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        message.startsWith("✓") -> AppSurface
                        message.startsWith("⚠️") || message.startsWith("No foods") -> MaterialTheme.colorScheme.errorContainer
                        else -> AppSurface
                    }
                )
            ) {
                Text(
                    message, 
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (searchResults.isNotEmpty()) {
            Text("Tap a food to add:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        
        // Display search results
        searchResults.forEach { food ->
                // Calculate total grams based on unit
                val currentQuantity = quantity.toDoubleOrNull() ?: if (selectedUnit == "grams") 100.0 else 1.0
                val totalGrams = if (selectedUnit == "pieces") {
                    currentQuantity * (gramsPerPiece.toDoubleOrNull() ?: 50.0)
                } else {
                    currentQuantity
                }
                
                val factor = totalGrams / 100.0
                val adjustedCalories = ((food.calories ?: 0.0) * factor).toInt()
                val adjustedProtein = ((food.proteinG ?: 0.0) * factor).toInt()
                val adjustedFat = ((food.fatG ?: 0.0) * factor).toInt()
                val adjustedCarbs = ((food.carbsG ?: 0.0) * factor).toInt()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Check if user is guest
                            if (actualUserId == 0L) {
                                Toast.makeText(
                                    context,
                                    "⚠️ Guest users cannot save food. Please register to save your data.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@clickable
                            }
                            
                            scope.launch {
                                try {
                                    val entry = Entry(
                                        date = today,
                                        mealType = selectedMeal,
                                        foodId = food.id,
                                        quantityG = totalGrams,
                                        userId = actualUserId
                                    )
                                    db.entryDao().upsert(entry)
                                    
                                    val displayMsg = if (selectedUnit == "pieces") {
                                        "${food.name} (${currentQuantity.toInt()} pieces ≈ ${totalGrams.toInt()}g) added to $selectedMeal"
                                    } else {
                                        "${food.name} (${totalGrams.toInt()}g) added to $selectedMeal"
                                    }
                                    
                                    // Show toast notification
                                    Toast.makeText(
                                        context,
                                        "✅ $displayMsg",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    message = "✓ Food added successfully!"
                                    searchResults = emptyList()
                                    query = ""
                                    quantity = if (selectedUnit == "grams") "100" else "1"
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "❌ Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    message = "Error adding: ${e.message}"
                                }
                            }
                        },
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                food.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "$adjustedCalories cal",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Display based on selected unit
                        if (selectedUnit == "pieces") {
                            val pieces = currentQuantity.toInt()
                            val gramsEach = gramsPerPiece.toIntOrNull() ?: 50
                            
                            Text(
                                "For $pieces piece${if (pieces > 1) "s" else ""} × ${gramsEach}g = ${totalGrams.toInt()}g",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Text(
                                "Nutrition: Protein ${adjustedProtein}g | Fat ${adjustedFat}g | Carbs ${adjustedCarbs}g",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "For ${totalGrams.toInt()}g: Protein ${adjustedProtein}g | Fat ${adjustedFat}g | Carbs ${adjustedCarbs}g",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        
                        Text(
                            "Reference (per 100g): ${food.calories?.toInt() ?: 0} cal | P: ${food.proteinG?.toInt() ?: 0}g | F: ${food.fatG?.toInt() ?: 0}g | C: ${food.carbsG?.toInt() ?: 0}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            
            Spacer(Modifier.height(8.dp))
        }
        
        // Add bottom spacing for better scrolling
        Spacer(Modifier.height(16.dp))
    }
}

private fun String.capitalize() = replaceFirstChar { it.uppercase() }
