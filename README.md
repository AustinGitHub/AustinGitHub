# Gym Companion

A minimal Android starter app to help people stay on track with their gym routine. It ships with:

- A Compose-based home screen tailored for motivation.
- Daily notification reminders powered by WorkManager and NotificationCompat.
- A Google Play Billing entry point so you can sell premium workout content.

## Running locally

1. Open the project folder in Android Studio Flamingo or newer.
2. Let Gradle sync; all dependencies are defined via the version catalog in `gradle/libs.versions.toml`.
3. Run the **app** configuration on a device or emulator running Android 7.0 (API 24) or newer.

## Customizing for release

- Replace the package name (`com.example.gymcompanion`) and icons before publishing.
- Update the subscription product ID `gym_companion_pro` in `MainActivity` and match it to a real Play Console product.
- Add your own workout lessons, schedules, and premium checks after confirming purchases in `onPurchasesUpdated`.
- Adjust the reminder cadence in `MainActivity.scheduleReminders` (currently 24 hours).

## Notes

The billing flow is intentionally lightweight: it initializes the client, fetches the subscription product, and starts the purchase UI. You should persist entitlement state, acknowledge purchases, and lock premium-only screens before shipping to production.
