# Steps Tracker

## Overview

The step tracker uses the device's accelerometer sensor (`TYPE_ACCELEROMETER`) rather than `TYPE_STEP_COUNTER`. This was a deliberate choice — `TYPE_STEP_COUNTER` is more accurate but requires a dedicated hardware pedometer chip that not all budget devices have. The accelerometer approach works on any Android device at the cost of slightly lower precision.

---

## Data persistence

Steps are saved to SharedPreferences in real time as the sensor fires. Using SharedPreferences rather than Room here because:
- Step data is simple (date → count, no relations)
- Writes are frequent (every few steps) and Room transactions would be overkill
- No need to query across tables

**Keys used:**
- `step_count_yyyy-MM-dd` — daily step count, keyed by date string
- `daily_goal` — user's step goal (default: 8000)
- `steps_entries_json` — JSON array of historical entries for the history list

**History entry format:**
```json
{
  "timestamp": 1704700000000,
  "steps": 8500,
  "date": "2024-01-08"
}
```

---

## Known issue: sensor resets on device reboot

The accelerometer step count resets when the device reboots. The app saves a snapshot to SharedPreferences on each sensor event, so data up to the last event is preserved — but the in-memory counter starts from zero after a reboot. The weekly history chart can show a visible drop on the day of a reboot.

A proper fix would use `TYPE_STEP_COUNTER` with a saved baseline, but for now the current behaviour is at least consistent and doesn't lose historical data.

---

## Screen layout

The screen is scrollable (`verticalScroll`) so the full history list is reachable without being cut off on smaller screens. The history itself uses a regular `Column` rather than `LazyColumn` because the list is bounded (7 days) and nesting a lazy list inside a scroll container causes conflicting scroll gestures.
4. **History Section** - View past entries with achievement indicators

#### History Indicators:
- ✓ (Green checkmark) = Daily goal achieved
- ○ (Circle) = Goal not yet reached

### 5. **File Modified**
`/app/src/main/java/com/example/foodtracker/ui/screen/StepsTrackerScreen.kt`

#### Key Imports Added:
```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

### 6. **Benefits**

✅ **Better UX**: Users can scroll to see all content without cutting off important information
✅ **Data Persistence**: Steps automatically saved without manual save button required
✅ **Real-time Updates**: History updates immediately as steps are detected
✅ **Offline Support**: All data saved locally, works without internet
✅ **Date-based Tracking**: Each day's steps tracked separately

### 7. **How Data is Saved**

1. **Sensor Detection**: Accelerometer detects steps continuously
2. **Real-time Persistence**: Each detected step is immediately saved
3. **Entry Consolidation**: Multiple saves on the same day update the same entry
4. **Manual Save Option**: "Save Today's Steps" button also available for explicit save

### 8. **Testing the Feature**

To verify everything works:
1. Build and run the app
2. Open the Steps Tracker screen
3. Walk around to accumulate steps
4. Scroll down to see the history and complete UI
5. Exit the app and reopen - steps should persist
6. Update the daily goal - it should be saved and persist

---

**All implementations follow Material Design 3 guidelines and are fully compatible with the existing codebase.**
