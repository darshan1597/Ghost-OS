# Ghost 1.3.2 automation setup

The selected default is now the automation flavor. It includes the optional
WhatsApp Accessibility service and automatic Send controls. This restores the
chosen functionality; it does not resolve or bypass the reported Play Protect
block. Keep Play Protect enabled.

## First device milestone

Connect the Vivo by USB, enable USB debugging in Developer options, and approve
the computer on the phone. Check `adb devices`. Use normal Flutter/Android Studio
deployment for development:

```powershell
flutter run --flavor automation
```

If Android or Play Protect blocks installation/access, stop at that finding and
review it. Google's developer guidance includes an appeal process. Deployment
through a development tool is not a guarantee that protection will accept an app.

## Enable on the phone

1. Settings > WhatsApp contacts: allow phone contacts.
2. Turn on Send through WhatsApp. Android opens Accessibility settings.
3. Enable Ghost WhatsApp Send after reviewing the system permission disclosure.
4. Enable local listening. For launches outside Ghost, enable Hands-free app
   launches and grant Display over other apps.
5. Test with an unlocked phone and your own spare number or a consenting contact.

Say `message Harsh on WhatsApp with hi`. If several contacts match, say a full
name or an ordinal. Ghost opens the numbered chat, verifies its title and the
exact composer text, and attempts Send once. It stops when it cannot verify.
Check WhatsApp for actual delivery. Locked requests wait for unlock/launch
eligibility and expire; Ghost never enters a PIN or bypasses authentication.

After installation/setup, the laptop is not needed. Speech/contact matching run
locally; message delivery uses WhatsApp's network connection. Phone behaviour and
the current WhatsApp layout still need real-device validation.

## Artifacts and fallback

Automation APK: `build/app/outputs/flutter-apk/app-automation-debug.apk`.
The standard source variant remains available with `--flavor standard`; it uses
drafts and manual Send. Variants share an app ID/signing setup, so one replaces
the other when its version is compatible. Rebuild a fallback at the current
version instead of installing an older APK over a newer version.

References:

- https://developer.android.com/studio/run/device
- https://developers.google.com/android/play-protect/warning-dev-guidance
- https://support.google.com/googleplay/android-developer/answer/10964491
