# Time locker

This is an Android app that enforces a strict timed lock (uses Device Owner + lockTask APIs). App label shown on device: "Time locker".

Important safety note
- Device Owner APIs can restrict device functionality (block uninstall, factory reset, safe boot). Test only on a spare device or emulator. Setting device owner typically requires a factory-reset device.

Quick build & run
1. Open this folder in Android Studio (File → Open).
2. Build: ./gradlew assembleDebug or use the Run button.
3. Install on device:
   - ./gradlew installDebug
   - or adb install -r app/build/outputs/apk/debug/app-debug.apk

Set as Device Owner (required for full behavior)
- WARNING: Usually requires a factory-reset test device.

1. Install the APK on the test device.
2. Run:
   adb shell dpm set-device-owner com.example.timelocker/.MyDeviceAdminReceiver

If successful, the app becomes device owner and lockTask / uninstall blocking will work.

Testing without Device Owner
- If not device owner, the timer UI and boot handling still work, but the actual system lockdown won't be applied. The app shows a warning Toast if it isn't device owner.
