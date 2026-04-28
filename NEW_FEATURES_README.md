# Streak Tracking & Notifications

Two independent features added without touching existing functionality.

---

## Streak tracking

`data/streak/StreakManager.kt`

Tracks consecutive days of activity — a day counts if the user logs food or tracks steps. The streak is stored in SharedPreferences with three values: current streak, best streak, and total active days.

The streak check runs once per day (on first app open after midnight) and compares today's date to the last active date. If the gap is more than one day, the streak resets.

**Accessing from other screens:**
```kotlin
val streakManager = StreakManager(context)
streakManager.recordActivity()   // call this when user logs food/steps
val current = streakManager.currentStreak
```

The Streak screen is reachable from the Dashboard and shows current streak, best streak, start date, and a short motivational note that changes based on streak length.

---

## Notifications

`data/notifications/`

Three reminder types, all opt-in and configurable:

- **Meal reminders** — breakfast / lunch / dinner at user-set times
- **Water reminders** — interval-based (every N hours between wake and sleep)
- **Streak reminder** — fires in the evening if the user hasn't logged anything that day

All notifications use the user's nickname from `UserPreferences`. Scheduled with `WorkManager` so they survive device restarts.

To stop a specific reminder type the user can toggle it off in the notification settings screen — this cancels the corresponding WorkManager task by tag.

---

## 1. Streak Tracking System 🔥

### What It Does
- Tracks consecutive days of user activity (food logging, step tracking, etc.)
- Maintains current streak, best streak, and total active days
- Beautiful UI with animated fire emoji and circular progress indicator
- Motivational messages based on streak length

### Features
- **Current Streak**: Shows how many consecutive days the user has been active
- **Best Streak**: Displays the longest streak ever achieved
- **Total Active Days**: Counts all days the user has logged activity
- **Activity Status**: Shows if user is active today with visual indicators
- **Streak Start Date**: Displays when the current streak began

### How It Works
- User's streak increases when they log food, track steps, or perform other activities
- Streak continues if activity is logged at least once per day
- If a day is skipped, the streak resets to 0
- All data is persisted using SharedPreferences

### Access
- Navigate from Dashboard → "My Streak" card (🔥 icon)
- View detailed statistics and motivational tips

### Integration with Existing Features
To automatically track streak when users perform activities, add these calls to your existing screens:

```kotlin
import com.example.foodtracker.utils.StreakHelper

// In AddFoodScreen after saving food entry
StreakHelper.recordFoodLog(context)

// In StepsTrackerScreen when steps are recorded
StreakHelper.recordStepsActivity(context)

// In WaterTrackingScreen when water is logged
StreakHelper.recordWaterTracking(context)

// In WeightTrackerScreen when weight is logged
StreakHelper.recordWeightTracking(context)
```

---

## 2. Notifications & Reminders 🔔

### What It Does
- Sends scheduled notifications to help users maintain healthy habits
- Fully customizable reminder times
- Persists across app restarts and device reboots

### Types of Reminders

#### Meal Reminders
- **Breakfast**: Default 8:00 AM
- **Lunch**: Default 12:00 PM
- **Dinner**: Default 7:00 PM
- Customize each meal time independently

#### Water Reminders
- Repeating reminders to stay hydrated
- Configurable interval (1-6 hours)
- Default: Every 2 hours

#### Activity Reminders
- **Daily Steps Check**: Default 8:00 PM - Reminds users to reach step goals
- **Streak Reminder**: Default 9:00 PM - Warns users if they haven't been active today

### Features
- ✅ Enable/disable each reminder independently
- ⏰ Custom time picker for all reminders
- 🔄 Automatic restoration after device reboot
- 📊 Visual toggle switches and time displays
- 💡 Tips and information cards

### Access
- Navigate from Dashboard → "Reminders" card (🔔 icon)
- Configure all reminder settings in one place

### Notification Channels
The app uses three notification channels:
1. **Meal Reminders** - For food and water reminders
2. **Streak Notifications** - For streak achievements and warnings
3. **Daily Goals** - For step and activity goals

---

## Technical Implementation

### New Files Created

#### Streak System
```
app/src/main/java/com/example/foodtracker/
├── data/
│   └── streak/
│       └── StreakManager.kt          # Core streak tracking logic
├── ui/
│   └── screen/
│       └── StreakScreen.kt           # Streak display UI
└── utils/
    └── StreakHelper.kt               # Integration helper functions
```

#### Notification System
```
app/src/main/java/com/example/foodtracker/
└── data/
    └── notifications/
        ├── NotificationHelper.kt      # Creates and displays notifications
        ├── ReminderScheduler.kt       # Schedules alarms using AlarmManager
        ├── ReminderReceiver.kt        # BroadcastReceiver for handling alarms
        └── BootReceiver.kt            # Restores reminders after reboot
```

#### UI Screens
```
app/src/main/java/com/example/foodtracker/ui/screen/
├── StreakScreen.kt                   # Streak statistics and display
└── RemindersScreen.kt                # Reminder configuration UI
```

### Modified Files

#### AndroidManifest.xml
Added permissions and broadcast receivers:
```xml
<!-- New Permissions -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Broadcast Receivers -->
<receiver android:name=".data.notifications.ReminderReceiver" ... />
<receiver android:name=".data.notifications.BootReceiver" ... />
```

#### AppNav.kt
Added navigation routes:
```kotlin
data object Streak : Dest("streak", "Streak")
data object Reminders : Dest("reminders", "Reminders")
```

#### DashboardScreen.kt
Added two new cards to the dashboard:
- "My Streak" card with 🔥 icon
- "Reminders" card with 🔔 icon

---

## Data Storage

### Streak Data (SharedPreferences: "streak_prefs")
- `current_streak`: Int - Current consecutive days
- `best_streak`: Int - Best streak ever achieved
- `last_activity_date`: String - Date of last activity (yyyy-MM-dd)
- `total_active_days`: Int - Total active days
- `streak_start_date`: String - When current streak started

### Reminder Settings (SharedPreferences: "reminder_prefs")
- `breakfast_enabled`: Boolean
- `breakfast_hour`: Int
- `breakfast_minute`: Int
- `lunch_enabled`: Boolean
- `lunch_hour`: Int
- `lunch_minute`: Int
- `dinner_enabled`: Boolean
- `dinner_hour`: Int
- `dinner_minute`: Int
- `water_enabled`: Boolean
- `water_interval_hours`: Int
- `steps_enabled`: Boolean
- `steps_hour`: Int
- `steps_minute`: Int
- `streak_warning_enabled`: Boolean
- `streak_warning_hour`: Int
- `streak_warning_minute`: Int

---

## User Permissions

### Android 13+ (API 33+)
The app will request notification permission at runtime. Users can:
- Allow or deny notifications
- Manage notification settings in system settings
- Control which notification channels are active

### Exact Alarms
The app uses exact alarms for precise reminder timing. On Android 12+ (API 31+), this may require user approval in system settings for some devices.

---

## Usage Guide

### For Users

#### Setting Up Reminders
1. Open the app and navigate to Dashboard
2. Tap "Reminders" card
3. Toggle on the reminders you want
4. Tap "Change" to set custom times
5. Adjust water reminder interval using the slider

#### Tracking Your Streak
1. Navigate to Dashboard → "My Streak"
2. View your current streak and statistics
3. Check if you're active today
4. Read tips on maintaining your streak

#### Maintaining Your Streak
- Log at least one meal per day
- Track your steps
- Log water intake
- Any tracked activity counts toward your streak

### For Developers

#### Integrating Streak Tracking
Add these calls to existing activity screens:

```kotlin
// Example: In AddFoodScreen after saving food
val context = LocalContext.current
StreakHelper.recordFoodLog(context)

// Example: In StepsTrackerScreen
StreakHelper.recordStepsActivity(context)
```

#### Manual Streak Management
```kotlin
val streakManager = StreakManager(context)

// Get streak statistics
val stats = streakManager.getStreakStats()

// Check if active today
val isActive = streakManager.isActiveToday()

// Reset streak (for testing)
streakManager.resetStreak()
```

#### Scheduling Custom Reminders
```kotlin
val scheduler = ReminderScheduler(context)

// Schedule a meal reminder
scheduler.scheduleMealReminder(
    type = ReminderScheduler.TYPE_BREAKFAST,
    hour = 8,
    minute = 30,
    enabled = true
)

// Cancel all reminders
scheduler.cancelAllReminders()
```

#### Showing Custom Notifications
```kotlin
val notificationHelper = NotificationHelper(context)

// Show meal reminder
notificationHelper.showMealReminder("Breakfast", NotificationHelper.NOTIFICATION_ID_BREAKFAST)

// Show streak achievement
notificationHelper.showStreakNotification(streakDays = 7)

// Show streak warning
notificationHelper.showStreakWarning(streakDays = 15)
```

---

## Testing

### Testing Streak System
1. Open Streak screen - should show 0 days
2. Navigate to Add Food and log a meal
3. Call `StreakHelper.recordFoodLog(context)`
4. Return to Streak screen - should show 1 day
5. Check "Active Today" indicator

### Testing Reminders
1. Open Reminders screen
2. Enable Breakfast reminder for 1 minute from now
3. Wait for notification to appear
4. Verify notification opens the app when tapped

### Testing Boot Persistence
1. Enable some reminders
2. Restart device
3. Verify reminders still work after reboot

---

## Known Limitations

1. **Exact Alarms**: On some Android 12+ devices, users may need to manually enable exact alarms in system settings
2. **Battery Optimization**: Aggressive battery savers may delay notifications
3. **Doze Mode**: In deep sleep, notifications may be delayed until next wake window
4. **Notification Icons**: Currently uses default Android icons; custom icons can be added to drawable resources

---

## Future Enhancements (Suggestions)

1. **Streak Rewards**: Add badges or achievements for milestone streaks
2. **Social Sharing**: Share streak achievements on social media
3. **Custom Reminder Messages**: Let users customize notification text
4. **Smart Reminders**: Adjust reminder times based on user behavior
5. **Weekly Reports**: Send summary notifications with weekly progress
6. **Streak Freeze**: Allow one "cheat day" per week without breaking streak
7. **Custom Activities**: Let users define what counts as activity
8. **Notification Sounds**: Custom sounds for different reminder types

---

## Support

If you encounter any issues:
1. Check notification permissions in system settings
2. Verify alarms are not being blocked by battery optimization
3. Ensure the app has background execution permissions
4. Check Android version compatibility (minimum Android 8.0)

---

## Credits

Features developed for Swasth Vision Android App
- Streak Tracking System ✅
- Notifications & Reminders ✅
- Zero modifications to existing functionality ✅

All features are modular and can be easily extended or customized.
