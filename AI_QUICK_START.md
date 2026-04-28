# AI Health Assistant — Quick Reference

## What is wired up

- Daily nutrition logging feeding into AI context
- Rule-based safety checks (run before every AI call)
- OpenAI GPT-4 support (optional API key)
- Google Gemini support (optional API key)
- Offline fallback using the local rules engine
- Weekly trend analysis (7-day rolling averages)
- Recipe recommendations filtered to the user diet and remaining macros
- WorkManager background job for scheduled daily summaries
- AI settings screen for configuring schedule and provider

## How to trigger the assistant manually

From anywhere in the app, navigate to the Health Insights screen. It loads the latest cached summary immediately and offers a refresh button to generate a new one.

## Adding the AI screens to navigation

In `ui/AppNav.kt`, add two destinations:

```kotlin
// Route definitions
data object AIHealthInsights : Dest("aiHealthInsights", "Health Insights")
data object AISettings      : Dest("aiSettings", "AI Settings")

// In NavHost
composable(Dest.AIHealthInsights.route) {
    DailyHealthSummaryScreen(onBack = { navController.popBackStack() })
}
composable(Dest.AISettings.route) {
    AISettingsScreen(onBack = { navController.popBackStack() })
}
```

Then add a card on the dashboard that navigates to `Dest.AIHealthInsights.route`.

## Triggering a summary programmatically

```kotlin
val advisor = AIHealthAdvisor(context)
val summary = advisor.generateDailySummary()  // suspend function
```

This runs the rules engine, picks a provider based on available API keys, builds the prompt, and returns a `DailyHealthSummary` object. The result is persisted automatically so it is available offline.

## Background scheduling

```kotlin
AIScheduler.scheduleDailySummary(context, hour = 20, minute = 0)
```

Schedules a WorkManager task that runs daily at the specified time. The user can change this from the AI settings screen. Cancel with `AIScheduler.cancel(context)`.

## Fallback behaviour

No API key -> `LocalRulesProvider` is used. It generates a structured response from the rules engine output directly, without any network call. The format matches the AI response so the UI does not need to handle it differently.
