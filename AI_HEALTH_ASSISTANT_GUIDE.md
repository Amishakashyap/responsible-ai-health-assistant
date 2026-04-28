# AI Health Assistant — How It Works

The AI assistant is designed around one guiding principle: give advice that's based on what the user has actually logged, not generic nutrition tips from the internet. Here's how the different layers fit together.

---

## Architecture overview

The system has three layers that run in sequence whenever the assistant generates a response:

```
User data + today's log
        ↓
  Health Rules Engine   ← runs first, catches anything dangerous
        ↓
  AI Reasoning Layer    ← OpenAI / Gemini / local fallback
        ↓
  Structured Response   ← parsed and displayed in the chat UI
```

The rules engine always runs before any AI call. If it flags a critical issue (e.g. under 1000 kcal/day), that warning is surfaced regardless of what the AI says.

---

## Layer 1: User health memory

The assistant has access to the user's full context, persisted in Room:

- **UserProfile** — height, weight, age, gender, activity level, fitness goal, diet type, allergies
- **DailyNutritionLog** — per-day calories, protein, carbs, fat, water intake, meal count
- **DailyHealthSummary** — AI-generated summaries cached locally so they're available offline
- **Recipe** — a set of verified recipes with known macros, filtered by diet type and remaining calories

This data is passed directly into the AI prompt so the model isn't guessing about the user's situation.

---

## Layer 2: Rule-based safety engine

`domain/health/HealthRulesEngine.kt`

The rules engine checks the user's logged data against hard thresholds before anything reaches the AI. This is intentional — I didn't want the AI to be the only thing standing between a user and genuinely bad advice.

Checks it runs:

| Condition | Threshold | Level |
|---|---|---|
| Daily calories | < 1000 kcal | CRITICAL |
| Daily calories | < 1200 kcal | WARNING |
| Protein intake | < 70% of target | CAUTION |
| Water intake | < 1000 ml | WARNING |
| BMI | < 16 or > 30 | CRITICAL |
| Fat as % of macros | < 15% | CAUTION |

Risk levels: `SAFE`, `CAUTION`, `WARNING`, `CRITICAL`. Anything `WARNING` or above is shown in the UI with an explanation, and included verbatim in the AI prompt so the model knows about it.

---

## Layer 3: AI reasoning

`domain/ai/AIHealthAdvisor.kt`

Supports three backends:

- **OpenAI GPT-4** — set `OPENAI_API_KEY` in `local.properties`
- **Google Gemini** — set `GEMINI_API_KEY` in `local.properties`
- **Local rule-based fallback** — always available, no API key needed

The prompt passed to the AI includes:

```
User profile: [nickname, age, gender, BMI, activity level, goal, diet type]
Today's nutrition: [calories, protein, carbs, fat, water, meal count]
Weekly averages: [7-day trends]
Health warnings from rules engine: [any flags from Layer 2]
Available recipes: [filtered by diet type, remaining calories, protein needs]

Task: generate a daily summary, next meal suggestion, recipe recommendation,
one habit tip, and a short motivational note.
```

The response is parsed into named sections so each piece can be displayed separately in the UI rather than as a wall of text.

---

## Recipe filtering

Recipes are stored in the local Room database with verified nutrition data. When building the prompt, the app queries for recipes matching:

- Diet type (vegetarian / vegan / keto / paleo / Mediterranean / omnivore)
- Remaining calorie budget for the day
- Protein gap (how far the user is from their protein target)
- Meal type (breakfast / lunch / dinner / snack) based on time of day

This way the AI is recommending from a known, accurate set rather than hallucinating ingredients.

---

## Adding a new AI provider

Create a class implementing the `AIProvider` interface in `domain/ai/`:

```kotlin
interface AIProvider {
    suspend fun getHealthAdvice(prompt: String): String
}
```

Register it in `AIHealthAdvisor.kt` and add a corresponding `local.properties` key. The rest of the pipeline doesn't need to change.

## Architecture

### **Three-Layer Design**

1. **Data Layer** (Foundation)
   - Persistent user health memory
   - Daily nutrition logs
   - Weekly trend analysis
   - Verified recipe database

2. **Rules Engine** (Safety Rails)
   - Health safety checks
   - Calorie minimum/maximum validation
   - Protein/water intake warnings
   - BMI-based cautions
   - Macro balance validation

3. **AI Reasoning Layer** (Intelligence)
   - Interprets data in human-friendly way
   - Provides personalized suggestions
   - Recommends recipes
   - Offers habit tips
   - Motivational messaging

## Key Features

### 1. **Persistent User Health Memory**

**Files:** 
- `data/db/Entities.kt` - Enhanced data models
- `data/db/Daos.kt` - Database access objects
- `data/user/UserPreferences.kt` - User profile management

**What It Stores:**
- User Profile: height, weight, age, gender, activity level, goal, diet type, allergies
- Daily Logs: calories, protein, carbs, fat, water intake, meal count
- Health Summaries: AI-generated insights stored for offline access
- Recipes: Verified nutrition data for meal suggestions

**Database Tables:**
```
- user_profile: Extended with diet_type, allergies, food_preferences
- daily_nutrition_log: Tracks all daily nutrition metrics
- recipe: Verified recipes with known nutrition values
- daily_health_summary: AI-generated insights cached locally
```

### 2. **Rule-Based Safety Layer**

**File:** `domain/health/HealthRulesEngine.kt`

**Safety Checks:**
- ⚠️ **CRITICAL**: Calories < 1000/day → Medical warning
- ⚠️ **WARNING**: Calories < 1200/day → Health concern
- 🟡 **CAUTION**: Protein < 70% of goal → Adjustment needed
- 🔴 **CRITICAL BMI**: < 16 or > 30 → Professional consultation recommended
- 💧 **Water**: < 1000ml/day → Dehydration risk
- 🥗 **Macros**: Fat < 15% → Essential nutrient warning

**Risk Levels:**
```kotlin
enum class RiskLevel {
    SAFE,       // All good
    CAUTION,    // Minor concerns
    WARNING,    // Moderate concerns
    CRITICAL    // Medical advice needed
}
```

### 3. **AI Integration Layer**

**File:** `domain/ai/AIHealthAdvisor.kt`

**Supported Providers:**
- **OpenAI GPT-4**: Requires API key from platform.openai.com
- **Google Gemini**: Requires API key from makersuite.google.com
- **Local (Rule-Based)**: Always available, no API needed

**Structured Prompt Format:**
```
USER PROFILE: nickname, age, gender, BMI, activity, goal, diet type
TODAY'S NUTRITION: calories, protein, carbs, fat, water, meals
WEEKLY TREND: averages and patterns
HEALTH WARNINGS: from rules engine
AVAILABLE RECIPES: filtered by diet/calories/protein
TASK: Generate personalized summary, meal suggestion, recipe recommendation, habit tip, motivation
```

**AI Response Parsing:**
Extracts structured sections:
- Daily Summary
- Next Meal Suggestion
- Recipe Recommendation
- Habit Tip
- Motivational Message

### 4. **Verified Recipe System**

**File:** `data/db/Entities.kt` (Recipe entity)

**Recipe Filtering:**
```kotlin
// Find recipes suitable for user's needs
findSuitableRecipes(
    mealType = "breakfast|lunch|dinner|snack",
    dietType = "vegetarian|vegan|keto|paleo|mediterranean|omnivore",
    minCalories = remaining/3,
    maxCalories = remaining + buffer,
    limit = 5
)
```

**Recipe Data:**
- Name, description, ingredients, instructions
- Calories, protein, carbs, fat per serving
- Prep time, cook time, servings
- Diet type, meal type, tags (high-protein, low-carb, quick)

### 5. **Trend Analysis**

**File:** `domain/health/WeeklyTrendAnalyzer.kt`

**Pattern Detection:**
- Calorie consistency (±10% of goal = excellent)
- Protein intake patterns (< 70% = low pattern detected)
- Hydration trends (< 60% = chronic dehydration risk)
- Meal frequency (< 2/day = skipping meals pattern)
- Day-to-day variability (> 30% = inconsistent)

**Weekly Insights:**
```kotlin
data class NutritionTrends(
    val averageCalories: Int,
    val averageProtein: Int,
    val consistency: ConsistencyLevel,
    val patterns: List<String>,      // Detected patterns
    val improvements: List<String>    // Actionable suggestions
)
```

### 6. **Health Advisor Service**

**File:** `domain/health/HealthAdvisorService.kt`

**Daily Flow:**
```
1. Fetch user profile + today's log
2. Apply health rules engine → Get warnings/suggestions
3. Get recent 7-day logs → Analyze trends
4. Find suitable recipes (filtered by diet, calories, protein)
5. Build structured prompt with all context
6. Send to AI (or use local fallback)
7. Parse AI response into structured format
8. Store in database for offline access
9. Display in UI
```

**Key Methods:**
```kotlin
// Generate daily summary
suspend fun generateDailySummary(): DailySummaryResult

// Update nutrition log
suspend fun updateDailyNutritionLog(
    caloriesConsumed, proteinConsumed, carbsConsumed, 
    fatConsumed, waterConsumedMl, mealCount
)

// Get today's cached summary
suspend fun getTodaysSummary(): DailyHealthSummary?
```

### 7. **Background Scheduling**

**File:** `domain/health/DailySummaryWorker.kt`

**WorkManager Integration:**
- Schedules daily summary generation at user-configured time (default: 7 AM)
- Requires network connection for AI providers
- Retries on failure
- Can be triggered manually for testing

**Usage:**
```kotlin
// Schedule daily summaries
DailySummaryScheduler.scheduleDailySummary(context)

// Cancel scheduling
DailySummaryScheduler.cancelDailySummary(context)

// Test immediate generation
DailySummaryScheduler.generateNow(context)
```

### 8. **User Interface**

**Files:**
- `ui/screen/DailyHealthSummaryScreen.kt` - Main insights display
- `ui/screen/AISettingsScreen.kt` - AI configuration

**DailyHealthSummaryScreen Features:**
- Beautiful gradient header with personalized greeting
- Today's progress cards (calories, protein, water with progress bars)
- Daily summary card with AI insights
- Meal suggestion card
- Recipe recommendation
- Habit tip card
- Health warnings (if any)
- Motivational message
- Regenerate button

**AISettingsScreen Features:**
- Enable/disable AI features
- Provider selection (OpenAI, Gemini, Local)
- API key configuration
- Daily summary time scheduler
- Save and test buttons
- Status indicator

## Setup Instructions

### 1. **Add to Navigation**

Update `ui/AppNav.kt`:

```kotlin
sealed class Dest(val route: String, val label: String = "") {
    // ... existing routes ...
    data object AIHealthInsights : Dest("aiHealthInsights", "Health Insights")
    data object AISettings : Dest("aiSettings", "AI Settings")
}

// In NavHost:
composable(Dest.AIHealthInsights.route) {
    DailyHealthSummaryScreen(onBack = { navController.popBackStack() })
}

composable(Dest.AISettings.route) {
    AISettingsScreen(onBack = { navController.popBackStack() })
}
```

### 2. **Add to Dashboard**

Update `DashboardScreen.kt` to add navigation cards:

```kotlin
HealthDataCard(
    title = "Health Insights",
    icon = "🤖",
    color = Color(0xFF9C27B0),
    route = "aiHealthInsights"
)

HealthDataCard(
    title = "AI Settings",
    icon = "⚙️",
    color = Color(0xFF607D8B),
    route = "aiSettings"
)
```

### 3. **Initialize WorkManager**

In `MainActivity.onCreate()`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize AI scheduling
    val aiConfig = AIConfig(this)
    if (aiConfig.isEnabled) {
        DailySummaryScheduler.scheduleDailySummary(this)
    }
}
```

### 4. **Populate Sample Recipes**

Create a database callback in `AppDatabase.kt`:

```kotlin
override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    // Insert sample recipes
    CoroutineScope(Dispatchers.IO).launch {
        insertSampleRecipes(context)
    }
}

private suspend fun insertSampleRecipes(context: Context) {
    val db = AppDatabase.get(context)
    val recipeDao = db.recipeDao()
    
    // High-protein breakfast
    recipeDao.insert(Recipe(
        name = "Greek Yogurt Protein Bowl",
        description = "High-protein breakfast with Greek yogurt, berries, and nuts",
        caloriesPerServing = 320,
        proteinPerServing = 28,
        carbsPerServing = 35,
        fatPerServing = 8,
        servings = 1,
        prepTimeMinutes = 5,
        cookTimeMinutes = 0,
        dietType = "vegetarian",
        mealType = "breakfast",
        ingredients = "200g Greek yogurt\n1/2 cup mixed berries\n30g granola\n15g almonds",
        instructions = "1. Add Greek yogurt to bowl\n2. Top with berries\n3. Sprinkle granola and almonds",
        tags = "high-protein,quick,no-cook"
    ))
    
    // Add more recipes...
}
```

## Usage Flow

### **For Users:**

1. **Initial Setup**
   - Complete profile with diet preferences (Settings → Profile)
   - Configure AI provider (Settings → AI Settings)
   - Set daily summary time (default 7 AM)

2. **Daily Usage**
   - Track meals as usual in the app
   - Open "Health Insights" to see AI summary
   - Click "Generate Insights" if not auto-generated
   - Follow meal suggestions and recipe recommendations
   - Implement habit tips

3. **AI Recommendations**
   - Daily summary explains progress warmly
   - Meal suggestion based on remaining macros
   - Recipe recommendation fits diet type and needs
   - Habit tip for long-term improvement
   - Motivational message for encouragement

### **For Developers:**

```kotlin
// Manual summary generation
val healthAdvisor = HealthAdvisorService(context)
val result = healthAdvisor.generateDailySummary()

if (result.success) {
    val summary = result.summary
    // Display summary
} else {
    // Handle error: result.error
}

// Update nutrition log
healthAdvisor.updateDailyNutritionLog(
    caloriesConsumed = 1500,
    proteinConsumed = 80,
    carbsConsumed = 180,
    fatConsumed = 50,
    waterConsumedMl = 2000,
    mealCount = 3
)

// Get health check without AI
val userPrefs = UserPreferences(context)
val dailyLog = healthAdvisor.getTodaysLog()
val healthCheck = HealthRulesEngine.runHealthChecks(userPrefs, dailyLog)

// Analyze trends
val recentLogs = db.dailyNutritionLogDao().getRecentLogs(userId, 7)
val trends = WeeklyTrendAnalyzer.analyzeTrends(recentLogs)
```

## API Key Setup

### **OpenAI (Recommended)**

1. Go to [platform.openai.com](https://platform.openai.com)
2. Create account and add payment method
3. Generate API key
4. In app: Settings → AI Settings → Select OpenAI → Enter API key
5. Cost: ~$0.01-0.03 per daily summary (using GPT-4)

### **Google Gemini (Free Tier Available)**

1. Go to [makersuite.google.com](https://makersuite.google.com)
2. Get free API key
3. In app: Settings → AI Settings → Select Gemini → Enter API key
4. Cost: Free for limited usage, then pay-as-you-go

### **Local (No API Needed)**

- Rule-based recommendations always available
- No cost, no network needed
- Less personalized but still helpful

## Privacy & Safety

### **Data Storage:**
- All data stored locally in Room database
- API keys stored in encrypted SharedPreferences
- User data never shared without consent
- AI prompts include only aggregated nutrition data

### **Health Safety:**
- Rule engine provides safety rails
- Critical health warnings trigger immediately
- App does NOT replace medical professionals
- Warnings include "consult healthcare provider" for serious concerns

### **AI Safety:**
- Structured prompts prevent harmful advice
- AI told explicitly: "You are NOT a doctor"
- Responses parsed and validated
- Fallback to safe local recommendations if AI fails

## Testing

### **Test Scenarios:**

1. **Low Calorie Warning**
   ```kotlin
   updateDailyNutritionLog(caloriesConsumed = 800, ...)
   // Should trigger CRITICAL warning
   ```

2. **High Protein Success**
   ```kotlin
   updateDailyNutritionLog(proteinConsumed = 120, proteinGoal = 100, ...)
   // Should show positive feedback
   ```

3. **Dehydration Alert**
   ```kotlin
   updateDailyNutritionLog(waterConsumedMl = 500, waterGoalMl = 2000, ...)
   // Should trigger hydration warning
   ```

4. **Trend Analysis**
   ```kotlin
   // Add 7 days of consistent logs
   // Should detect patterns and give insights
   ```

## Dependencies Added

```gradle
// WorkManager for background tasks
implementation("androidx.work:work-runtime-ktx:2.9.0")

// Gson for JSON parsing (AI responses)
implementation("com.google.code.gson:gson:2.10.1")

// OkHttp already included via Retrofit
```

## Files Created

### Domain Layer
- `domain/ai/AIConfig.kt` - AI provider configuration
- `domain/ai/AIHealthAdvisor.kt` - AI integration layer
- `domain/health/HealthRulesEngine.kt` - Safety rules
- `domain/health/HealthAdvisorService.kt` - Orchestration service
- `domain/health/WeeklyTrendAnalyzer.kt` - Pattern detection
- `domain/health/DailySummaryWorker.kt` - Background scheduling

### Data Layer
- Enhanced `data/db/Entities.kt` - New entities
- Enhanced `data/db/Daos.kt` - New DAOs
- Enhanced `data/user/UserPreferences.kt` - Diet preferences

### UI Layer
- `ui/screen/DailyHealthSummaryScreen.kt` - Main insights screen
- `ui/screen/AISettingsScreen.kt` - Configuration screen

## Benefits

✅ **Personalized**: Uses actual user data and patterns
✅ **Safe**: Rule-based safety checks prevent harmful advice
✅ **Offline-First**: Summaries cached locally
✅ **Flexible**: Works with or without AI (local fallback)
✅ **Privacy-Focused**: All data stored locally
✅ **Cost-Effective**: Local mode free, AI mode < $1/month
✅ **Educational**: Explains "why" not just "what"
✅ **Motivational**: Supportive, non-judgmental tone
✅ **Actionable**: Specific recipes and habit tips
✅ **Evidence-Based**: Uses BMR, TDEE, BMI calculations

## Future Enhancements

1. **Photo Food Logging**: AI vision to identify meals
2. **Voice Assistant**: Ask questions about nutrition
3. **Smart Notifications**: Context-aware reminders
4. **Social Features**: Share recipes and progress
5. **Wearable Integration**: Heart rate, sleep, steps
6. **Meal Planning**: Week-long meal plans with shopping lists
7. **Recipe Creation**: AI generates custom recipes from ingredients
8. **Progress Predictions**: Forecast goal achievement timeline

---

**Remember:** This AI assistant augments your app's existing features. It doesn't replace tracking, calculations, or user control. It makes the data meaningful, actionable, and human.
