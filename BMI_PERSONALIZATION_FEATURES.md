# BMI & Personalisation

## How targets are calculated

Once the user completes the profile setup screen, the app calculates their daily targets and stores them in `UserPreferences`.

**BMR (Mifflin-St Jeor):**
```
Male:   10 × weight(kg) + 6.25 × height(cm) − 5 × age + 5
Female: 10 × weight(kg) + 6.25 × height(cm) − 5 × age − 161
```

**TDEE** = BMR × activity multiplier (1.2 – 1.9 depending on activity level)

**Calorie goal** = TDEE adjusted for fitness goal:
- Lose weight: −500 kcal
- Maintain: TDEE
- Gain muscle: +300 kcal

**Protein target** = 1.2 – 2.0 g/kg depending on activity level. The upper end (2.0–2.2 g/kg) is used on the Muscle Gain screen.

**Macros split:**
- Protein: target × 4 kcal/g
- Fat: 25–30% of total calories ÷ 9
- Carbs: remainder ÷ 4

BMI is recalculated whenever weight or height changes in the profile. The BMI category (Underweight / Normal / Overweight / Obese) is shown on the dashboard and in the personalized calories screen.

---

## Profile setup screen

Collected during onboarding:
- Nickname
- Weight (kg) and height (cm)
- Age and gender
- Activity level (Sedentary → Extra Active, 5 levels)
- Fitness goal (Lose Weight / Maintain / Gain Muscle)

The screen shows a live BMI preview as the user enters their measurements, including ideal weight range. This was more useful than showing BMI only after submission.

---

## Personalisation throughout the app

The nickname is used in dashboard greetings and in all notification messages (meal reminders, water reminders). It's a small thing but makes the app feel less generic. Stored in SharedPreferences via `UserPreferences.nickname`.

BMI category influences the health advice shown in the AI assistant prompt. The rules engine also uses BMI directly — BMI under 16 or over 30 triggers a CRITICAL flag.
- Step goal reminders: "{nickname}, Step Goal Reminder 👟"
- Streak warnings: "{nickname}, Don't Break Your Streak! ⚠️"

## Files Added

### Utilities
1. **BMICalculator.kt** (`utils/`)
   - `calculateBMI()`: Weight/height² calculation
   - `getBMICategory()`: Categorizes BMI (UNDERWEIGHT/NORMAL/OVERWEIGHT/OBESE)
   - `getBMICategoryDescription()`: Human-readable category
   - `getBMIEmoji()`: Visual emoji indicator
   - `getIdealWeightRange()`: Healthy weight range for height
   - `getHealthAdvice()`: Personalized health recommendations

2. **NutritionCalculator.kt** (`utils/`)
   - `calculateBMR()`: Basal Metabolic Rate using Mifflin-St Jeor equation
   - `calculateTDEE()`: Total Daily Energy Expenditure
   - `calculateDailyProtein()`: Activity-based protein needs
   - `calculateDailyCarbs()`: Carbohydrate allocation
   - `calculateDailyFats()`: Fat allocation

### Data Layer
3. **UserPreferences.kt** (`data/user/`)
   - Stores user profile data in SharedPreferences
   - Properties: nickname, weight, height, age, gender, activityLevel, fitnessGoal
   - Computed properties: bmi, bmr, tdee, calorieGoal, proteinGoal, carbGoal, fatGoal
   - `calculateAndSaveGoals()`: Integrates BMI and nutrition calculators
   - `isProfileComplete`: Validates required data

### UI Screens
4. **EnhancedProfileSetupScreen.kt** (`ui/screen/`)
   - Comprehensive onboarding flow
   - Real-time BMI calculation preview
   - FilterChips for gender/activity/goal selection
   - Input validation and error handling
   - Calls `userPrefs.calculateAndSaveGoals()` on completion

5. **PersonalizedCaloriesScreen.kt** (`ui/screen/`)
   - BMI display card with category indicator
   - Ideal weight range recommendation
   - Nutrition goal cards with progress bars
   - Personalized messages using nickname
   - Color-coded progress indicators

## Files Modified

### Navigation
6. **AppNav.kt** (`ui/`)
   - Added `Dest.EnhancedProfileSetup` route
   - Added `Dest.PersonalizedCalories` route
   - Updated registration flow to navigate to EnhancedProfileSetup
   - Added composable definitions for new screens

### Screens
7. **RegistrationScreen.kt** (`ui/screen/`)
   - Added nickname TextField
   - Saves nickname to UserPreferences on registration
   - Falls back to first name if nickname not provided

8. **DashboardScreen.kt** (`ui/screen/`)
   - Added personalized greeting card with nickname
   - Displays BMI with emoji and category
   - Shows daily calorie goal
   - Updated "Calories Count" card to navigate to PersonalizedCalories

### Notifications
9. **NotificationHelper.kt** (`data/notifications/`)
   - Updated all notification methods to accept nickname parameter
   - `showMealReminder()`: Personalized meal notifications
   - `showWaterReminder()`: Personalized hydration reminders
   - `showStepGoalReminder()`: Personalized step reminders
   - `showStreakWarning()`: Personalized streak warnings
   - Default nickname is "User" if not provided

10. **ReminderReceiver.kt** (`data/notifications/`)
    - Fetches user nickname from UserPreferences
    - Passes nickname to all notification methods
    - Personalized notifications for all reminder types

## User Flow

### New User Registration
1. User enters name, email, password, **nickname**
2. Registration saves nickname to SharedPreferences
3. Navigate to **EnhancedProfileSetupScreen**
4. User enters: weight, height, age, gender, activity level, fitness goal
5. Real-time BMI calculation displayed
6. On "Complete Profile" → calculateAndSaveGoals() is called
7. Navigate to Dashboard with personalized greeting

### Daily Usage
1. Dashboard shows: "Hello, {nickname}! 🌟"
2. BMI card displays current BMI and category
3. Tap "Calories Count" → PersonalizedCaloriesScreen
4. View BMI-based nutrition goals with progress
5. Receive personalized notifications throughout the day

### Notifications
All scheduled reminders now use nickname:
- Morning: "{nickname}, Time for Breakfast! 🍳"
- Lunch: "{nickname}, Time for Lunch! 🍱"
- Dinner: "{nickname}, Time for Dinner! 🍽️"
- Water: "{nickname}, Stay Hydrated! 💧"
- Steps: "{nickname}, Step Goal Reminder 👟"
- Streak: "{nickname}, Don't Break Your Streak! ⚠️"

## Technical Details

### BMI Calculation
```kotlin
BMI = weight (kg) / height² (m²)
```

### BMR Calculation (Mifflin-St Jeor)
- **Men**: BMR = 10W + 6.25H - 5A + 5
- **Women**: BMR = 10W + 6.25H - 5A - 161
- **Other**: Average of both formulas

### TDEE Calculation (Harris-Benedict)
- Sedentary: BMR × 1.2
- Lightly Active: BMR × 1.375
- Moderately Active: BMR × 1.55
- Very Active: BMR × 1.725
- Extra Active: BMR × 1.9

### Macronutrient Distribution
- **Protein**: 1.2-2.0g/kg (based on activity)
- **Fats**: 0.8-1.0g/kg (20-35% of calories)
- **Carbs**: Remaining calories (4 cal/g)

### Data Storage
All user data stored in SharedPreferences with key "user_prefs":
- nickname
- weight_kg
- height_cm
- age
- gender
- activity_level
- fitness_goal
- profile_complete

## UI/UX Enhancements

### Dashboard
- Prominent personalized greeting card (primary container color)
- BMI badge with emoji and category
- Calorie goal display (🎯)

### Personalized Calories Screen
- BMI indicator with color-coded emoji:
  - 🟦 Underweight (< 18.5)
  - 📉 Underweight-Normal (18.5-20)
  - 📊 Normal (20-25)
  - 📈 Normal-Overweight (25-30)
  - 🔴 Obese (> 30)
- Ideal weight range card
- Nutrition cards with circular progress indicators
- Personalized messages: "{nickname}, {remaining} cal left today"

### Profile Setup
- Real-time BMI calculation preview
- Color-coded BMI indicator
- Category description (Underweight/Normal/Overweight/Obese)
- Input validation with error messages
- FilterChips for multi-select options

## Benefits

1. **Personalized Experience**: Users feel recognized and valued
2. **Scientific Accuracy**: Uses validated BMR/TDEE formulas
3. **Goal-Oriented**: Tailored nutrition based on fitness goals
4. **User-Friendly**: Real-time feedback and visual indicators
5. **Motivational**: Personalized notifications increase engagement
6. **Comprehensive**: Covers all aspects of nutrition tracking

## Compatibility

- No changes to existing features
- Backward compatible (default nickname "User")
- Preserves original navigation flows
- All existing screens remain functional

## Next Steps (Optional Enhancements)

1. Add BMI history tracking (chart over time)
2. Integrate with food logging for automatic macro tracking
3. Add weight progress tracking towards ideal range
4. Implement meal suggestions based on remaining macros
5. Add achievement badges for BMI milestones
6. Export nutrition reports (PDF/CSV)

---

**Note**: All formulas and calculations are based on established nutritional science and medical guidelines. Users should consult healthcare professionals for personalized medical advice.
