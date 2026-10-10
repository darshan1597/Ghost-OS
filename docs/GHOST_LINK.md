# Ghost Link — nearby Android receiver prototype

The Vivo runs the existing offline assistant. Ghost Link is a separate native
Android APK for the second phone (Android 8+, including Funtouch OS 13 / Android
13). It has no Flutter engine, speech model, microphone permission, contacts
permission, internet permission or Accessibility service. Version 0.1 opens apps
only; remote alarms and WhatsApp sending are not implemented.

## Install and pair

1. Install the updated Ghost automation APK on the Vivo. Install
   `build/ghost-link/outputs/apk/debug/ghost-link-debug.apk` on the second phone.
   Keep Play Protect enabled. Installation acceptance still depends on the phone.
2. Turn Bluetooth on and pair both phones in Android Bluetooth settings. Confirm
   the system pairing prompt on both. App scanning/discoverability is deliberately
   not used, so no Bluetooth scan/location permissions are required.
3. On the second phone, open Ghost Link. Tap **Allow nearby devices and refresh**,
   select the Vivo under **Approved controller**, then **Enable receiving**.
   Stop receiving before selecting a different controller.
4. On the Vivo, open Ghost Settings → **Ghost Link**. Tap **Allow and refresh
   paired phones**, then **Connect** beside the second phone.
5. Leave Ghost Link open and the receiving phone unlocked for the first test.
   On the Vivo, type or say **open Chrome on my other phone**. Choose an installed
   app's exact name if Chrome is absent. Local commands without an explicit remote target
   remain local; remote unsupported commands are refused, never run locally.
6. To test with the receiver outside its screen, enable **connection bubble**
   and review Android's display-over-other-apps permission. The visible bubble is
   required for background launches; Android/vendor behavior still needs testing.
   Tap it to return to Ghost Link, hold it to stop receiving. It may obscure a
   small area of other apps; hide it whenever remote launches are unnecessary.

Disconnect from either app or its notification. Receiver restarts require a new
explicit Enable receiving action. The receiving phone refuses locked/screen-off
launches and does not store delayed actions. The controller reports the receiver's
result through Ghost history and its existing spoken-response setting. A launch
result means Android was asked to launch the app, not verified screen contents.

## Protocol and boundaries

- Secure Bluetooth Classic RFCOMM, bonded-device address allowlist, one connection
  at a time. Both ends use a connected-device foreground service and visible
  notification. The receiver accepts only the controller selected locally.
- A fresh random session token is sent in the receiver hello. Commands use a
  version, session, UUID request ID, `open_app` operation and app name. Receiver
  validates each and refuses duplicate IDs within that session. No shell commands,
  intents, arbitrary packages or raw audio are executed from the wire.
- Four-byte length framing, maximum 8192 bytes before allocation; names limited
  to 120 characters. Invalid frames close the connection. At 256 requests, start
  a fresh connection rather than evicting replay IDs.
- The receiving phone scans launchable apps locally, requires an exact/unique
  name match, and launches only the package resolved from that scan. Ambiguity
  returns candidate names; give a fresh command with the full name.
- One outstanding controller action, 15-second result timeout, 20-second connect
  timeout and a five-second receiver execution window. No automatic retries or
  reconnects. Lost responses have an unknown outcome; inspect the receiver before
  repeating an action. Bluetooth bonding authorizes the device, not individual
  applications on that device; an app on the approved Vivo could speak this
  protocol. A separate application credential is needed before expanding to
  sensitive remote operations such as sending messages.
- No backend, laptop or internet needed after installation. Bluetooth range and
  battery/background restrictions still apply. No claims of tested radio range,
  screen-off reliability, vendor compatibility or successful pairing until two
  real devices have been tested.

## Build and verify

```powershell
flutter build apk --debug --flavor automation --target-platform android-arm64 --no-pub
cd android
.\gradlew.bat :ghost-link:assembleDebug :app:testAutomationDebugUnitTest
```

Controller tests include explicit remote routing, framing, Unicode/back-to-back
packets, oversized frames and duplicate/invalid/unsupported request refusal.
Device acceptance must also cover unauthorized bonded peers, disconnect during a
request, missing/ambiguous apps, locked receiver, bubble disabled/enabled and
offline operation with the laptop shut down.

Android references: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions),
[RFCOMM connection](https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices),
[foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types),
[background activity starts](https://developer.android.com/guide/components/activities/secure-bal).


## Connection bubble (0.3.0)

Receiving must be enabled, Connection bubble switched on and Android display-over-
other-apps permission approved. Leaving Link shows a circular branded status badge.
Green indicates a completed handshake with the approved controller; red indicates
no active session. Rings pulse on validated commands, with a short received-event
pulse so fast app launches are still visible. No idle animation runs otherwise.

Drag to dock left/right; saved placement stays clear of the status/navigation
areas. Settings offers Small (40 dp), Medium (48 dp), Large (56 dp), and optional
idle fading to 45% opacity after 15 seconds, without changing size. Tap/hold opens quick controls. Check connection
reads current local Bluetooth/session state; it does not add a network ping or retry.
Open Ghost Link opens this receiver's own controls. Reverse phone commands remain
unsupported. Disconnect stops receiving and removes all overlays. Screen-off/lock
and foreground Link hide the overlay; returning to unlocked background restores it.
