# Build & Setup Guide

## Requirements

- Android Studio Giraffe (2022.3.1) or newer — [download here](https://developer.android.com/studio)
- JDK 17 (included with recent Android Studio builds — no separate install needed)
- Android SDK with API 34 and API 24 platforms installed
- A device or emulator running Android 7.0 (API 24) or higher

Python is **not required** — the food database is pre-built and bundled in the repository at `app/src/main/assets/databases/food_catalog.db`.

---

## Cloning and opening the project

```bash
git clone https://github.com/Amishakashyap/responsible-ai-health-assistant.git
```

1. Open Android Studio
2. Choose **Open** (not "New Project")
3. Navigate to the cloned folder and select it
4. Wait for the initial Gradle sync — this downloads dependencies and may take a few minutes on first run
5. If Android Studio prompts you to upgrade the Gradle plugin, click **Don't remind me again** unless you intentionally want to upgrade

---

## SDK setup

If you get SDK-related errors after sync, go to **File → Project Structure → SDK Location** and confirm the Android SDK path is set. Then open **Tools → SDK Manager** and make sure:

- **SDK Platforms:** Android 14.0 (API 34) and Android 7.0 (API 24)
- **SDK Tools:** Android SDK Build-Tools 34, Platform-Tools, Emulator

---

## Running the app

Select a connected device or emulator from the toolbar dropdown and hit **Run**. 

To connect a physical device:
1. Go to **Settings → About Phone** and tap **Build Number** seven times to enable Developer Options
2. Turn on **USB Debugging** under Developer Options  
3. Plug in via USB and accept the debugging prompt on the phone

To create an emulator if you don't have one:
1. **Tools → Device Manager → Create Device**
2. Pick Pixel 5 (or similar), then select the API 34 system image
3. The emulator takes a couple of minutes to boot on first launch

---

## Optional: AI chat API keys

The AI assistant works without any API keys — it uses a local rule-based engine by default. If you want it to call OpenAI GPT-4 or Google Gemini instead, create a `local.properties` file in the project root (it's in `.gitignore` so it won't be committed) and add:

```
OPENAI_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
```

If both are missing, the app falls back to the offline rules engine automatically.

---

## Common issues

**Gradle sync fails — "Could not resolve..."**  
Usually a network issue on first sync. Check your internet connection and try **File → Sync Project with Gradle Files** again.

**Compose preview not showing**  
The preview tab can lag behind indexing. Give it a minute after sync completes. Running the app on a device works regardless.

**KSP errors on first build**  
Room uses KSP for code generation. If you see errors referencing `_Impl` files, do a clean build: **Build → Clean Project**, then **Build → Rebuild Project**.

**"SDK location not found"**  
The `local.properties` file is in `.gitignore`. Android Studio should create it automatically when you open the project, but if not, create it manually with:
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

1. Open PowerShell in the project root (`d:\food_tracking`)
2. Run the database builder script:
```powershell
python .\scripts\build_food_db.py --csv .\cleaned_nutrition_dataset_per100g.csv --out .\android-app\app\src\main\assets\databases\food_catalog.db
```
3. Verify success - you should see: `Built DB at android-app\app\src\main\assets\databases\food_catalog.db with 3454 foods.`

## Step 2: Open Project in Android Studio

1. Launch **Android Studio**
2. Click **Open** (or File → Open)
3. Navigate to `d:\food_tracking\android-app`
4. Click **OK**
5. Wait for Gradle sync to complete (this may take 2-5 minutes on first load)
   - Watch the bottom status bar for "Gradle sync in progress..."
   - If prompted to upgrade Gradle plugin, click **Don't remind me again**

## Step 3: Configure Android SDK

1. If you see SDK errors, go to **File → Project Structure → SDK Location**
2. Ensure Android SDK is installed at default location (usually `C:\Users\<YourName>\AppData\Local\Android\Sdk`)
3. Go to **Tools → SDK Manager**
4. Under **SDK Platforms** tab, ensure these are checked:
   - Android 14.0 (API 34) - for compileSdk
   - Android 7.0 (API 24) - for minSdk
5. Under **SDK Tools** tab, ensure these are checked:
   - Android SDK Build-Tools 34
   - Android SDK Platform-Tools
   - Android Emulator
6. Click **Apply** and wait for installation

## Step 4: Verify Project Structure

In Android Studio's Project view (left sidebar), verify these folders exist:
```
android-app/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── assets/
│           │   └── databases/
│           │       └── food_catalog.db  ← Should exist after Step 1
│           ├── java/com/example/foodtracker/
│           │   ├── MainActivity.kt
│           │   ├── data/db/
│           │   │   ├── AppDatabase.kt
│           │   │   ├── Daos.kt
│           │   │   └── Entities.kt
│           │   ├── domain/
│           │   │   └── Targets.kt
│           │   └── ui/
│           │       ├── AppNav.kt
│           │       ├── screen/
│           │       │   ├── LoginScreen.kt
│           │       │   ├── RegistrationScreen.kt
│           │       │   ├── ProfileSetupScreen.kt
│           │       │   ├── DashboardScreen.kt
│           │       │   ├── AddFoodScreen.kt
│           │       │   ├── AnalyticsScreen.kt
│           │       │   ├── SummaryScreen.kt
│           │       │   ├── BMICalculatorScreen.kt
│           │       │   ├── CaloriesCountScreen.kt
│           │       │   ├── WaterTrackingScreen.kt
│           │       │   ├── MuscleGainScreen.kt
│           │       │   ├── DietPlanScreen.kt
│           │       │   ├── NewsScreen.kt
│           │       │   └── ProfileScreen.kt
│           │       └── theme/
│           │           └── Theme.kt
│           └── res/
│               └── values/
│                   ├── strings.xml
│                   └── themes.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## Step 5: Sync Gradle and Build

1. Click **File → Sync Project with Gradle Files**
2. Wait for sync to complete (watch bottom status bar)
3. If you see errors:
   - **"Cannot resolve symbol..."** → Wait for indexing to complete (bottom right status)
   - **KSP errors** → The gradle files should auto-download KSP plugin
   - **Compose errors** → Check that `composeOptions` block exists in `app/build.gradle.kts`

## Step 6: Set Up Emulator or Device

### Option A: Use Physical Device (Recommended for Testing)
1. Enable **Developer Options** on your Android phone:
   - Go to Settings → About Phone → Tap "Build Number" 7 times
2. Enable **USB Debugging**:
   - Settings → Developer Options → USB Debugging → ON
3. Connect phone via USB cable
4. Accept "Allow USB Debugging" prompt on phone
5. In Android Studio, phone should appear in device dropdown (top toolbar)

### Option B: Create Virtual Device (Emulator)
1. Click **Tools → Device Manager**
2. Click **Create Device**
3. Select **Phone → Pixel 5** (or any recent phone)
4. Click **Next**
5. Select **System Image → API 34 (Android 14)** - download if needed
6. Click **Next** → **Finish**
7. Wait for emulator to boot (first launch takes 2-3 minutes)

## Step 7: Run the App

1. Ensure device/emulator is selected in the device dropdown (top toolbar)
2. Click the green **Run** button (▶️) or press **Shift+F10**
3. Wait for Gradle build (1-3 minutes first time)
4. App will install and launch on device/emulator

### What You Should See:
1. **Login Screen** - First screen with email/password fields and Register link
2. Click **Register** → Fill form → User saved to database
3. **Profile Setup Screen** → Fill medical history, goals, measurements
4. **Dashboard** → Grid of 8 feature cards
5. Tap any card to navigate to that feature

## Step 8: Test the App Flow

### Test Registration & Login:
1. Click **Register**
2. Fill in:
   - Email: `test@example.com`
   - Password: `password123`
   - Name: `Test User`
   - City: `Mumbai`
   - Gender: `Male`
   - Age: `25`
   - Blood Group: `O+`
3. Click **Register** → Should navigate to Profile Setup
4. Fill profile:
   - Medical History: `None`
   - Goal: Select **weightloss**
   - Weight: `70`
   - Height: `175`
   - Target Weight: `65`
   - Exercise: Select **regular**
5. Click **Save & Continue** → Should navigate to Dashboard

### Test Features:
1. **BMI Calculator** → Enter weight 70, height 175 → Calculate
2. **Water Tracking** → Click + to add glasses
3. **Calories Count** → Shows 0 (no entries yet)
4. **Analytics** → Shows empty state
5. **Add Food** → Search UI (not wired yet)

## Step 9: View Database (Optional)

To inspect the SQLite database:
1. In Android Studio, open **View → Tool Windows → App Inspection**
2. Run the app on an emulator (API 26+)
3. Select **Database Inspector** tab
4. You'll see tables: `user`, `user_profile`, `food`, `entry`
5. Click on any table to view data

## Step 10: Common Issues and Fixes

### Issue: Gradle Sync Failed
**Solution:**
1. Go to **File → Invalidate Caches → Invalidate and Restart**
2. Wait for re-indexing
3. Retry sync

### Issue: "SDK not found"
**Solution:**
1. **File → Project Structure → SDK Location**
2. Set Android SDK location to: `C:\Users\<YourName>\AppData\Local\Android\Sdk`
3. Click **Apply**

### Issue: KSP annotation processor errors
**Solution:**
1. Check `app/build.gradle.kts` has:
```kotlin
plugins {
    id("com.google.devtools.ksp")
}
```
2. Clean and rebuild: **Build → Clean Project** then **Build → Rebuild Project**

### Issue: Emulator is slow
**Solution:**
1. Use a physical device instead, OR
2. Enable hardware acceleration:
   - Intel: Enable Intel HAXM in SDK Manager
   - AMD: Enable Windows Hypervisor Platform in Windows Features

### Issue: App crashes on launch
**Solution:**
1. Check **Logcat** (bottom panel in Android Studio)
2. Look for red error lines
3. Common causes:
   - Database file missing in assets → Re-run Step 1
   - Room schema error → Clean and rebuild project

### Issue: "food_catalog.db not found"
**Solution:**
1. Verify file exists at: `android-app\app\src\main\assets\databases\food_catalog.db`
2. If missing, re-run the Python script from Step 1
3. Do **Build → Clean Project** and **Build → Rebuild Project**

## Step 11: Making Changes and Rebuilding

When you modify code:
1. **Save All** (Ctrl+S or Ctrl+Shift+S)
2. Android Studio auto-detects changes
3. Click **Run** again (no need to clean build usually)
4. For major changes (new dependencies, gradle changes):
   - **File → Sync Project with Gradle Files**
   - Then **Run**

## Step 12: View Logs

To debug issues:
1. Open **Logcat** (bottom panel)
2. Filter by selecting your app package: `com.example.foodtracker`
3. Set log level to **Debug** or **Verbose**
4. Run app and watch logs in real-time

## Next Steps

Once the app runs successfully:
1. Wire the **Add Food search** to FoodDao
2. Connect **Summary Screen** to show targets vs consumed
3. Add data persistence for water tracking
4. Implement logout functionality
5. Add password hashing for security

## Quick Command Reference

### PowerShell (in `d:\food_tracking`):
```powershell
# Build database
python .\scripts\build_food_db.py --csv .\cleaned_nutrition_dataset_per100g.csv --out .\android-app\app\src\main\assets\databases\food_catalog.db

# Verify database created
Test-Path .\android-app\app\src\main\assets\databases\food_catalog.db
```

### Android Studio Shortcuts:
- **Run app**: Shift+F10
- **Stop app**: Ctrl+F2
- **Build project**: Ctrl+F9
- **Sync Gradle**: Ctrl+Shift+O
- **Find file**: Ctrl+Shift+N
- **Search everywhere**: Double Shift

## Support

If you encounter issues not covered here:
1. Check the **Logcat** output for error details
2. Verify all files exist in the correct locations
3. Ensure Gradle sync completed successfully
4. Try **File → Invalidate Caches → Invalidate and Restart**
