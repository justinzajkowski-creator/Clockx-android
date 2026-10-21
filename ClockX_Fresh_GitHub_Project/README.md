# ClockX Dual Time for Android

Personal-use Android analog dual-time clock, based on the user's approved blue daytime and night-lume artwork.

## Features
- Resizable round home-screen widget with transparent corners.
- Live wallpaper for home or lock screen, with animated seconds when visible.
- Full-screen watch on black or watch over a selected photo.
- Independently configurable local and home/GMT time zones and corrections.
- Working date pointer, AM/PM indicator and 6 PM–6 AM automatic lume.
- Adjustable size and placement for the Samsung lock screen.

## Build an installable APK
This repository includes `.github/workflows/build-apk.yml`.
After pushing project files onto the `main` branch:
1. Open **GitHub → Actions → Build ClockX APK**.
2. Open the successful run.
3. Download the `ClockX-Installable-APK` artifact.
4. Extract the downloaded artifact ZIP on your Samsung and open `app-debug.apk`.

Alternatively, open this project in Android Studio with Java 17 / Android SDK 35 and run `gradle :app:assembleDebug`.

## Use on Samsung
- Open **ClockX Dual Time**.
- Press **ADD ROUND HOME-SCREEN WIDGET**, or long-press the Home screen → Widgets → ClockX Dual Time.
- Press **APPLY FULL-SCREEN / LOCK-SCREEN CLOCK** for the live wallpaper picker, then choose lock screen if offered.
- Enable the photo option and choose an image for a clock-over-wallpaper experience.
- The wallpaper is behind Samsung's lock/unlock controls. The Always On Display is separate.

## Notes
This is a source-only project. APK compilation and actual Samsung-device behavior have not been verified.
Standard Android RemoteViews widgets cannot guarantee a continuously sweeping seconds hand; the live wallpaper can draw animated seconds while it is visible.
