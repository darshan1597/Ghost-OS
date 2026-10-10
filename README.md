# Ghost

> Clone this repository with [Git LFS](https://git-lfs.com/) installed. The bundled offline ONNX voice model is stored through LFS; run `git lfs pull` after cloning if your Git client does not fetch it automatically.

### Hands-free Phase 1

- The low-power wake grammar runs by itself while Ghost waits; the full Vosk decoder starts only after a wake hit.
- Wake sensitivity and end-of-speech pause are adjustable in Settings.
- Recognition preserves Vosk confidence and alternatives and asks for recovery when unknown speech is uncertain.
- Conversational replies can continue into a second turn without repeating the wake phrase.
- Acoustic echo cancellation supports “Ghost stop”, “stop talking”, and “be quiet” while Ghost speaks.
- Wake detection, speech recognition, turn state, and replies remain on-device.

The wake phrase is still **Hey Ghost**. A personalized acoustic wake model requires recorded samples and device tuning; the current release uses an adjustable offline Vosk grammar.

### Safe automatic actions: Phase 2

- Lock-safe media commands cover play, pause, next, previous, volume, mute, and unmute.
- Saved-contact calls require Contacts, Phone, and call-control permission plus an explicit “yes” while unlocked.
- “Answer”, “reject”, and “hang up” use Android Telecom only after the user grants call-control access.
- Notification reading is opt-in, limited to an unlocked phone, and keeps recent content in process memory only.
- Replyable Android notifications support “reply to NAME with MESSAGE”; Ghost binds the exact `RemoteInput`, reads it back, and requires “yes” while unlocked.
- Reviewed actions accept “yes” or “no” by voice; a locked phone never approves them.

### Local routines and driving: Phase 3

- Create, list, run, and delete named routines entirely offline. Example: "create a routine called commute with start driving mode then play music and then open Maps".
- Routine definitions are stored on the phone and limited to app opening, media controls, and driving-mode changes. Sensitive actions cannot be placed in a routine.
- Driving mode forces concise spoken replies and can prefer a connected Bluetooth headset or car communication device for microphone capture.
- Settings shows saved routine steps, tap-to-run and delete controls, driving mode, Bluetooth preference, and current routing status.
- Running a routine requires the phone to be unlocked. Missing or ambiguous apps stop the routine rather than guessing.

### Context, recoverable tasks and custom wake names: Phase 4

- Local conversation context understands “open it again”, “message the same person with …”, “call them again”, and “delete the last reminder”. Context stays in process memory.
- Routine execution records each dispatched step. A failed step pauses the task and accepts “retry step”, “skip step”, or cancel instead of continuing blindly.
- Changing the assistant name also rebuilds the small offline wake grammar for “Hey NAME”. Short English names supported by the bundled Vosk vocabulary work best, and “Hey Ghost” remains a fallback.

### Optional offline conversation AI

Ghost includes a tiny built-in conversational layer that works without a model. A broader
Qwen3 0.6B model is an optional, Wi-Fi-only download from Settings. It is never bundled in
the APK and can be removed without affecting phone commands. llama.cpp runs it locally; no
prompt is uploaded.

For a compact Vivo V30 installer, build an ARM64 release with code/resource shrinking:

```powershell
flutter build apk --release --flavor automation --target-platform android-arm64
```

For Play distribution, use an Android App Bundle so Play delivers only the phone's ABI.

### Version 1.7.0: bundled offline cinematic male voice

Ghost Cinematic Monster Male uses a locally bundled Ryan neural model through
sherpa-onnx. Select it in Voice character; Android offline TTS and Monster Woman
remain available. No runtime download or server is needed.
[Implementation, license constraints, measured device tests and limitations](docs/OFFLINE_NEURAL_VOICE.md).


### Bubble updates: Ghost OS 1.6.3 and Ghost Link 0.3.1

Both bubbles keep their size and optionally fade to 45% opacity after 15 seconds
of inactivity. Touching the bubble restores brightness. Ghost OS settings now
include Small/Medium/Large sizes under Floating assistant bubble.

### Ghost Link 0.3.1: connection bubble

Circular cyan/purple phone-and-ghost bubble with an actual connection indicator:
green for an approved active session, red while waiting, and pulsing rings when
a validated request arrives. Enable receiving and Connection bubble, approve
Android overlay access, then leave Ghost Link to see it. It hides inside Link,
while locked, with the screen off, and when receiving is stopped.

Drag to either edge; the position is saved. Choose 40/48/56 dp sizes and optional
Fade when idle to reduce opacity after 15 seconds. Tap or hold for
Open Ghost Link, Check connection (local status), Bluetooth settings or Disconnect.
Ghost Link remains a receiver; it does not issue commands back to the main phone.


### Version 1.6.2: microphone consent and status

Reference-inspired neon consent popup with local-processing/privacy information,
ghost artwork and explicit Not now / Enable listening choices. The home microphone
card follows native state: OFF, STARTING while the offline model loads, and ON.
Enable microphone opens consent; Disable microphone stops listening. Speak now
remains available once listening is ready. The popup scrolls on short screens.


### Version 1.6.1 / Ghost Link 0.2.0

Empty History keeps the Today divider and the same outlined waveform card layout
as actual activity, without inventing commands, timestamps or completed actions.
Ghost Link uses reference-based branding and a native dark wave/card design.
All existing receiver features remain: approved controller selection, explicit
receiving toggle, permission refresh, Bluetooth settings, bubble enable/hide,
connection status, disconnect and setup guidance. No remote messaging or alarms
were added. Stop receiving before selecting a different approved controller.


### Version 1.6.0: bubble, History and curved dock

Reference-inspired cyan/purple ghost bubble with listening pulses, processing
orbit and new-response indicator. Tap or hold for Open Ghost, Stop listening
and Settings; drag to dock at either edge. It hides inside Ghost and while
locked or the screen is off. Enable listening and hands-free access to use it.

History groups actual in-memory command results by day, offers Copy details
and Clear all, and uses the dark outlined cards from the supplied design.
Response recorded means Ghost logged a response, not proof another app
completed the action. Activity still resets when the process ends.
All three pages share the curved dark dock and raised gradient waveform button.


### Version 1.5.1: glass Settings and navigation

The command field contains text and a send control; microphone controls live in
the microphone section. Bottom navigation now uses translucent glass styling.
Settings uses the supplied design with the existing Ghost Link, hands-free,
installed-app, voice, contacts, assistant-name and privacy features.

### Version 1.5.0: reference-inspired home

New home layout with luminous ghost artwork, gradient greeting, command bar,
microphone card and curved navigation. Change **Settings → Assistant name** to
update “Hey, I’m Ghost”; the name is saved locally and defaults to Ghost.
The wake phrase remains “Hey Ghost”. The command cards open WhatsApp, request
an alarm time and open Gmail; email summarization is not implemented.

### Version 1.4.0: Ghost Link

Ghost Link is a separate native receiver for your second Android phone. The Vivo
listens and routes explicit remote app-opening requests over Bluetooth.
[Setup and current limits](docs/GHOST_LINK.md). Remote alarms and messaging are
future work; two-phone behavior still needs device testing.

Standalone Android assistant with a Flutter interface and native Kotlin services.
English first. No server or API key required. The speech model ships in the APK.

### Version 1.3.2: automation selected

The default build is now automation, as requested. It includes user-enabled
WhatsApp Send assistance: find contacts, ask only when ambiguous, open the selected
chat, verify the exact recipient/message and tap Send once. Installation and
Accessibility access must first be validated on the Vivo with Play Protect enabled.
This is not a fix or bypass for the previously reported installation block.
See [automation setup and device milestone](docs/AUTOMATION_SETUP.md).

The standard flavor remains available for drafts/manual Send.

### Version 1.3.1: previous standard build

The 1.3.1 standard APK does not declare or includes an Accessibility service. This
addresses the likely permission trigger for the reported Play Protect warning;
installation acceptance is not yet verified. Keep Play Protect enabled.
WhatsApp name lookup and duplicate choices remain, but you press Send in the
opened draft. Offline voice, bubble, alarms, app opening and appearance remain.
The separate automation flavor preserves the Send-assistance implementation.
See [Play Protect and build variants](docs/PLAY_PROTECT.md).

### Version 1.3

A dedicated wake grammar and free command decoder now share one microphone.
Inline wake commands are preserved; a Speak now button bypasses the wake phrase.
Alarm/contact clarification listens for your answer without another wake phrase.
Actual recognition and screen-off reliability still need testing on the Vivo.

The interface has a neon orbital core, dark glass panels and a grid backdrop.
The animated ghost bubble hides inside Ghost, opens it with a scale-up transition,
and returns outside the app while listening. Voice settings offer deeper Monster
man / lighter Monster woman characters plus an installed offline English voice
picker. The base voice depends on the phone's TTS engine; gender isn't universally
labelled. Preview voices rather than assuming a preset is a new trained voice.

Manual contact aliases have been removed. Allow phone contacts for name lookup.
A unique name opens the WhatsApp chat directly; multiple matches ask for the full
name or an ordinal. Optional user-enabled Android Accessibility taps Send once
only after checking the chat title and exact message. Unsupported layouts leave
a draft for manual sending. Delivery is never claimed as verified.
See [1.3 device checks](docs/V4_DEVICE_CHECKS.md) for setup and limitations.

Speech remains offline; temporary reply WAV files are removed after playback.
The exact logo can be added at `assets/branding/ghost_logo.png`; the existing
code-drawn mascot remains the fallback until that file is supplied.

## Run

Use the installed Flutter and Android SDK toolchains:

```powershell
flutter pub get
flutter analyze
flutter test
flutter build apk --debug --flavor automation
```

APK: `build/app/outputs/flutter-apk/app-automation-debug.apk`.
Install it on your own phone or connect the phone for `flutter run`.
The laptop is not needed after installation. This is a development build, not a
signed production release.

## Use

1. Open Ghost. Typed commands work immediately.
2. Settings: select Off, Brief, or Full voice. Cinematic Male is bundled and
   offline. Android fallback and Monster Woman need an installed offline English
   Android TTS voice; network-only voices are refused.
3. Enable listening and grant microphone/notification access. First use unpacks
   the bundled model and can take several seconds.
4. Say **Hey Ghost**, wait for **Hey boss**, then give a supported command.
5. Stop with the app button or notification. Re-enable manually after reboot or
   force-stop. Continuous listening uses battery.

Supported examples:

- `open WhatsApp`, `open Settings`, `open YouTube`.
- `message Harsh on WhatsApp with hi` after allowing phone contacts. Say a full
  name or ordinal if several match. In the standard build, press Send manually in the opened draft.
  Send assistance is only included in the separate automation development flavor.
- `set an alarm for 6 am`, `set an alarm for six thirty pm`, `set an alarm for 18:30`.
  No Ghost confirmation: request automatic setting, then show the alarm list if
  the clock supports it. Ghost cannot verify that the alarm was saved.
- `scan apps`, `list apps`: refresh the local launchable-app catalog; browse and
  search it in Settings > Installed apps. Exact names and unique word matches
  are supported; ambiguous names require a full name or explicit selection.
- `unlock phone`: request Android authentication. From the background, tap the
  unlock notification to open the system prompt. No PIN/password bypass.
- `help`, `cancel`, `stop listening`.

For hands-free background launches, enable Settings > Floating assistant bubble
and grant Android's Display over other apps permission. While listening, a visible
Ghost bubble enables supported launches: tap to open Ghost, hold to stop listening.
Without it, use the continuation notification. Alarms and app launches wait while
locked and resume when unlocked and launch capability is available. Deferred
app/alarm actions expire after 10 minutes and remain cancellable. WhatsApp
automatic Send requests awaiting launch expire after two minutes. No lock-screen or Android permission bypass.
WhatsApp delivery and online apps still need internet. Unsupported commands fail
with an explanation rather than being guessed.

## Your custom greeting

Place the selected recording at:

`android/app/src/main/assets/voice/greeting.mp3`

Then rebuild. No file is included yet; Ghost generates the greeting using the
selected offline engine. Use a short clip (around 1–2 seconds). Voice Off silences it.
This recording supplies the fixed greeting only, not a generative monster voice
for arbitrary responses. Those use the bundled neural voice or selected Android fallback.

## Structure and verification

- [Architecture](docs/ARCHITECTURE.md)
- [Vivo device acceptance checklist](docs/DEVICE_TESTING.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- Flutter UI: `lib/app`, `lib/core`, `lib/features/assistant`.
- Android core: `android/app/src/main/kotlin/com/ghostos/ghost_os/core`.
- Android voice: `android/app/src/main/kotlin/com/ghostos/ghost_os/voice`.

Pure Kotlin parser tests are under `android/app/src/test`. Run
`android/gradlew.bat -p android :app:testAutomationDebugUnitTest` with the Android Studio JDK
configured as JAVA_HOME. Flutter tests cover consent, local dispatch, bound draft
approval, and voice settings.

Screen-off wake reliability, battery usage, microphone interruptions and actual
app results must be checked on the Vivo V30. Compilation alone does not verify
those behaviours. Custom wake names, Kannada/Hindi, OpenRouter, assistant-role
integration and automation outside the supported WhatsApp Send flow are future work.
