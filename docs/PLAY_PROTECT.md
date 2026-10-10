# Play Protect and Ghost builds

> The user selected automation for 1.3.2. It is now the default flavor; standard remains a manual-Send alternative. The automation variant may still be blocked. See [AUTOMATION_SETUP.md](AUTOMATION_SETUP.md).


The reported warning matches Google's category for Internet-sideloaded apps
declaring sensitive capabilities, including Accessibility. Ghost 1.3 declared
an Accessibility service for WhatsApp Send; this is the likely trigger, not a
confirmed device diagnostic. See [Google's developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

## Standard 1.3.1

The standard build excludes the service declaration, service code and
Accessibility resources. It also hides the Send-assistance controls and clears
any saved enabled preference. Name lookup and duplicate choices remain: Ghost
opens the matched WhatsApp chat with text ready; the user presses Send.

Offline listening, TTS effects, bubble, app opening and alarms remain available.
Microphone, contacts, notification, overlay and foreground-service permissions
remain for those features. There is no SMS reader or notification listener.

Build:

```powershell
flutter build apk --debug --flavor standard --target-platform android-arm64
```

Install `build/app/outputs/flutter-apk/app-standard-debug.apk`. Keep Play Protect
enabled. Permission removal is not a guarantee of approval; if the new APK is
still blocked, investigate the current finding and use Google's review/appeal
process rather than disabling protection.

## Automation source

The separate automation flavor retains user-enabled WhatsApp Send. Its service
and resources are under `android/app/src/automation`. It may still receive the
same block; it is not an installation workaround or a reviewed Play release.
Both variants use the same app ID/signing configuration, so installing one
replaces the other. To build this development variant explicitly:

```powershell
flutter build apk --debug --flavor automation --target-platform android-arm64
```

The standard packaged manifest and DEX are checked by `scripts/verify_standard_apk.py`.
Actual installation acceptance still requires the Vivo; no device is connected.
