# SwasthVision — AI-Powered Food & Health Tracker

SwasthVision is an Android application for tracking daily nutrition and getting personalised health guidance. It combines a local food database (Indian cuisine focused) with a rule-based safety engine and optional AI chat via OpenAI or Gemini. The goal was to build something I'd actually want to use — fast, offline-capable, and honest about calorie data.

## What it does

- Log meals by searching from a catalog of ~3,000 foods with per-100g nutrition data
- Tracks calories, protein, carbohydrates, fat, fibre, and sodium per day
- Personalised targets based on your weight, height, activity level, and fitness goal (cut / maintain / bulk)
- BMI calculator with trend tracking
- Step counter with weekly progress
- AI health assistant that gives contextual advice based on your actual logged data — not generic tips
- Rule-based safety layer that flags things like severe calorie deficits before they reach the AI

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Database:** Room with a bundled SQLite food catalog loaded from assets
- **Architecture:** Single-activity, ViewModel per screen, coroutines for all DB and network work
- **AI backends:** OpenAI GPT-4 and Google Gemini (both optional — the app works fully offline without either)
- **Build:** Gradle with Kotlin DSL, min SDK 24, target SDK 34

## Getting started

**Requirements**
- Android Studio Giraffe (2022.3.1) or newer
- JDK 17 (bundled with recent Android Studio versions)
- A device or emulator running Android 7.0+

**Clone and open**
```bash
git clone https://github.com/Amishakashyap/responsible-ai-health-assistant.git
```
Open the cloned folder in Android Studio, wait for the Gradle sync to finish, then run the `app` configuration on your device or emulator.

The food database is bundled inside `app/src/main/assets/databases/` so no extra setup is needed to get it running.

**Optional: AI chat**

If you want the AI assistant to use GPT-4 or Gemini rather than the local rule-based fallback, add your API key to `local.properties`:
```
OPENAI_API_KEY=your_key_here
GEMINI_API_KEY=your_key_here
```
The app reads from BuildConfig at runtime and falls back gracefully if neither key is present.

## Project structure

```
app/src/main/java/com/example/foodtracker/
├── data/
│   ├── db/          # Room entities, DAOs, database setup, sample recipes
│   ├── model/       # Shared data classes
│   ├── network/     # API clients for OpenAI and Gemini
│   ├── notifications/
│   ├── repository/
│   └── user/        # UserPreferences (SharedPreferences wrapper)
├── domain/
│   ├── ai/          # Prompt builder, response parser, AI provider abstraction
│   └── health/      # HealthRulesEngine — safety checks run before any AI call
├── ui/
│   ├── screen/      # One composable file per screen
│   └── theme/       # Colours, typography, shared styles
└── utils/
```

## Documentation

- [AI Health Assistant — how it works](AI_HEALTH_ASSISTANT_GUIDE.md)
- [Implementation notes](AI_IMPLEMENTATION_SUMMARY.md)
- [Setup and build details](SETUP_GUIDE.md)
- [BMI personalisation logic](BMI_PERSONALIZATION_FEATURES.md)
- [Step tracker](STEPS_TRACKER_IMPROVEMENTS.md)

## Notes

`local.properties` and `*.jks` keystore files are excluded from version control. The food database (`food_catalog.db`) is included in the repo since it's a static reference file.

## License

MIT
