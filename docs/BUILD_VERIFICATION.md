# Ghost OS 1.7.0 neural voice verification

2026-10-08: Flutter analysis clean; 21 Flutter tests and 42 Kotlin tests passed.
ARM64 debug APK built and installed on Vivo V30. Actual airplane-mode voice,
interruption, fallback and Vosk/background-service checks passed after correcting
the Kotlin 2.4 JNI callback signature. Full evidence and limits:
[Offline neural voice report](OFFLINE_NEURAL_VOICE.md).

# Initial build verification — 2026-10-08

## Ghost Link 0.3.0 connection bubble

- Native circular branded overlay with green approved-session/red waiting status,
  pulsing rings on validated requests, saved edge docking, 40/48/56 dp choices and
  optional status-dot minimization after 15 seconds.
- Tap/hold quick controls: Open Ghost Link, local Check connection, Bluetooth
  settings and Disconnect. No reverse phone commands, microphone or new permission.
- Overlay hides in Link, on locked/screen-off devices and when receiving stops.
  Foreground/background activity hooks and service disposal keep windows in sync.
  Existing app-launch eligibility still requires foreground Link or visible overlay.
- Build succeeded; all 38 existing native command/protocol unit tests passed.
  Final Link APK passed 16KB ZIP alignment and installed on Android 13 Vivo V2135.
- Waiting badge verified on the device using existing approved overlay permission.
  A quick-panel measurement issue found on phone was fixed using explicit tile-row
  heights. Final panel sizing, live connected/request states, dragging and idle
  minimization need a further device check. Receiving/bubble controls restored to
  their original off state after the temporary test. No permissions bypassed.
- Waiting preview: build/previews/vivo-link-bubble-waiting.png.


## Ghost 1.6.2 microphone consent

- Custom curved neon consent dialog with existing ghost artwork, privacy badges,
  local/screen-off/battery information, wake instructions and explicit choices.
  Scrollable on short screens; cancelling never dispatches startListening.
- Home control displays native OFF / STARTING / ON states, muted mic-off icon,
  active green outline/status, explicit Enable/Disable actions and Speak now.
  Disable still uses the existing native stopListening method. No new permissions.
- Static analysis passed. All 18 Flutter checks passed across widget (15), phone
  layout (1) and History (2) runs, including native state updates, disable action,
  both consent choices and a 320x600 popup layout. Navigation width corrected.
- Automation arm64 APK 1.6.2+12 built, 16KB ZIP alignment passed and installed
  successfully on the Vivo V2318. OFF control and consent popup visually inspected.
  Previews: build/previews/vivo-microphone-off.png and vivo-microphone-consent.png.
- Listening was not enabled during visual inspection; STARTING/ON/stop dispatch
  were verified through mocked native snapshot/method tests, not live capture.


## Ghost 1.6.1 and Ghost Link 0.2.0

- Empty History shares the Today divider and outlined waveform card structure with
  real activity. Clear all is disabled and no timestamp/action menu or completion
  claim appears for the empty placeholder.
- Ghost Link native screen redesigned with reference-based launcher branding,
  floating phone/ghost header, dark wave background, feature strip, actual service
  status, Bluetooth/permission controls, outlined approved-controller picker,
  receiving and bubble switches, privacy guidance, disconnect and first-test card.
- Existing receiver features preserved. Controller selection is disabled during
  receiving; stop first to choose another phone. No new permissions or remote
  capabilities. Bluetooth transport/service execution unchanged.
- Logo/hero are generated adaptations of the supplied reference, documented in
  assets/branding/ARTWORK.md. The original attachment file was not available locally.
- Flutter analysis passed and all 16 Flutter tests passed. Both APKs built and
  passed 16KB ZIP alignment. Ghost 1.6.1+11 installed on Vivo V2318 and Link
  0.2.0 installed on Vivo V2135 Android 13. Receiver UI visually inspected on phone.
- Bluetooth receiving/remote launch behavior was not exercised during this UI update.


## Ghost 1.6.0 bubble and History

- Reference-inspired native overlay with cyan/purple ghost, listening rings,
  processing orbit, response indicator, draggable edge docking and quick controls.
  Open Ghost and Settings expand into the activity; the overlay hides in Ghost,
  on the lock screen and when the screen is off. Observer released on service stop.
- History displays real process-memory results grouped by local date, timestamps,
  Copy details and Clear all. Response recorded does not imply external delivery.
- Shared dark curved dock, raised gradient button, rounded waveform and active dot.
- Flutter analysis passed; 15 Flutter checks passed, including narrow layouts,
  actual history grouping, clear action and existing settings/command behavior.
- Automation arm64 APK 1.6.0+10 built and installed on the connected Vivo V2318.
  APK ZIP alignment passed. Device screenshot confirmed the shared dock in Settings.
- Live bubble animation, dragging and background quick-control behavior still need
  a device check with listening enabled. No permission settings were bypassed.


## Ghost 1.5.1 glass Settings

- Removed the microphone button from the command field and moved it into the
  microphone section. Text submission and direct speak-now action remain working.
- Replaced the bottom navigation's cyan/gradient fills with translucent glass,
  blur and neutral borders. Settings now uses reference-inspired glass cards,
  icon badges and blue/purple controls with responsive narrow-screen branding.
- Preserved Ghost Link pairing/disconnect, hands-free toggle, app browser/rescan,
  voice mode/character/engine/effects/preview, contacts/optional automatic Send,
  assistant naming and privacy/battery settings. No new permissions or native
  execution changes in this UI update.
- Flutter analysis: no issues. Flutter checks: 14 passed, including narrow phone
  layout, the moved speak-now control and compact hands-free switch.
- Automation APK built successfully (1.5.1+9); ZIP alignment passed. Installed
  successfully and visually inspected home/Settings on the Vivo.
- Previews: build/previews/vivo-glass-home.png and vivo-glass-settings.png.


## Ghost 1.5.0 home redesign

- Reference-inspired Flutter home with bundled transparent mascot art, gradient
  greeting, command bar, microphone card, command cards and curved navigation.
- Assistant name defaults to Ghost, saves in Android private preferences, and is
  included in every snapshot to update the greeting. Wake phrase remains Hey Ghost.
- Flutter static analysis: no issues; Flutter checks: 13 passed, including saved
  name events/save request, microphone consent, text dispatch and phone layout.
- Automation APK built successfully (1.5.0+8) and ZIP alignment passed.
- Updated APK installed and MainActivity opened on the connected Vivo.
- Real-device visual preview: build/previews/vivo-reference-ui-final.png.
- Artwork provenance and final built-in imagegen prompt: assets/branding/ARTWORK.md.
- Email summarization and general reminders from the reference are not implemented;
  home cards instead expose app opening and alarm requests.


## Ghost 1.4.0 / Ghost Link 0.1.0

- Updated Ghost automation APK and separate native receiver APK built successfully.
- Flutter analysis: no issues; Flutter checks: 12 passed, including selection of
  the exact paired device for connection.
- Native tests: 38 passed, zero failures/errors. Shared protocol tests cover
  Unicode/back-to-back frames, truncation/size refusal, replay/invalid operation
  rejection, explicit remote routing before/after an action and punctuation.
- Both apps share the existing tested app matcher and bounded frame protocol.
- Companion package inspection passed: no microphone, contacts, internet,
  Accessibility, SMS or notification-listener permissions; no Flutter runtime,
  speech model or native libraries. Debug companion size: 926,247 bytes.
- Both APK ZIP alignment checks passed.
- Ghost 1.4.0 installed successfully on the authorized Vivo using normal USB
  developer installation. Play Protect was not disabled by this workflow.
- Android 13 second phone was detected but USB debugging was unauthorized at
  verification time; companion installation and two-phone Bluetooth tests pending.
- App-opening only: remote alarms, messaging and application-specific
  authentication are not implemented. Pairing authorizes the approved device.
- See GHOST_LINK.md for setup, security boundaries and device acceptance cases.


## Ghost 1.3.2 automation / Vivo installation

- Automation APK built successfully, version 1.3.2+6; selected as default flavor.
- Automation native unit tests passed; existing Flutter checks remain applicable
  (no Flutter behavior changes in this update).
- Packaged Accessibility service is restricted to WhatsApp and declares
  isAccessibilityTool=false. No SMS or notification-listener permissions.
- APK ZIP alignment passed.
- Authorized USB developer installation on the connected Vivo succeeded with
  adb install -r; MainActivity launch succeeded. This is not Google approval
  or proof that every installation channel will accept the APK.
- Accessibility/Contacts setup and real WhatsApp sending, wake recognition,
  screen-off listening and voice sound still require device validation.
- Primary artifact: build/app/outputs/flutter-apk/app-automation-debug.apk.


## Ghost 1.3.1 standard / Play Protect response

- Standard APK built successfully, version 1.3.1+5, same app ID/debug signing setup.
- Flutter analysis: no issues. Flutter checks: 11 passed, including capability-gated
  Send settings and stale enabled-preference UI behaviour.
- Standard native tests: 33 passed, zero failures/errors.
- Preserved automation flavor: native compilation passed.
- Packaged standard manifest and DEX inspection: passed; no Accessibility service,
  WhatsAppAccessService implementation, SMS permissions, or notification listener.
- APK ZIP alignment: passed. Primary artifact: app-standard-debug.apk; app-debug.apk
  is a copy of this verified standard artifact for existing local download links.
- Prior automation APK retained as app-automation-1.3.0-debug.apk, not recommended
  as a response to the reported installation block.
- Actual Play Protect installation acceptance is unverified without the Vivo.
  This build removes the likely permission trigger, not a Google approval guarantee.


## Ghost 1.3

- Flutter analysis: no issues.
- Flutter regression/layout checks: 9 passed. Phone-sized previews generated with
  locally installed Roboto/Material icon fonts; layout had no overflow.
- Native checks: 33 passed, zero failures/errors. Includes wake tail routing,
  phone-contact matching/ambiguity, multiple phone numbers and unchanged composer
  verification. Short exact names still ask when longer matching names exist.
- Final debug APK build: passed (version 1.3.0+4, Android arm64 target).
- No Android device connected. Actual recognition accuracy, monster voice sound,
  background/bubble launch behaviour and WhatsApp layout compatibility are not
  proven by the automated checks. Follow V4_DEVICE_CHECKS.md on the Vivo.
- Android Accessibility assistance is optional; unsupported UI leaves a draft and
  no delivery is claimed. Exact logo remains unavailable as a local asset.


## English recognition and monster voice (1.2)

- Flutter static analysis: no issues.
- Flutter checks: 5 passed.
- Kotlin checks: 24 passed (10 parser, 4 matcher, 4 action policy, 4 audio DSP,
  2 WAV processing), zero failures/errors.
- Alarm regression includes the user's `set an alaram for 6pm` example.
- In-device wake latency, ASR accuracy and processed voice sound remain unverified.
- Exact supplied logo not locally available; colours applied and provisional
  code-drawn avatar/icon used. Asset hook: assets/branding/ghost_logo.png.

## Alarm, unlock and installed-app update

- Flutter static analysis: no issues.
- Flutter checks: 5 passed, including installed-app search.
- Kotlin checks: 15 passed (7 command parser, 4 app matcher, 4 action policy),
  with no failures/errors.
- Native compilation passed with optional assistant bubble and unlock activity.
- Updated phone acceptance checks: V2_DEVICE_CHECKS.md. No device attached during
  this update; OriginOS alarm handling, unlock UI and hands-free background
  launches require device verification.

## First build

- Flutter static analysis: no issues.
- Flutter widget checks: 4 passed (listening consent, local text dispatch,
  pending-action approval ID, voice mode).
- Kotlin parser checks: 6 passed; 0 failures or errors. Includes recipient/body
  preservation, incomplete messages, ambiguous/invalid times, spoken-number
  conversion, unsupported actions and cancellation.
- Final Android debug APK build: passed using
  `flutter build apk --debug --target-platform android-arm64 --no-pub`.
- APK ZIP alignment check: passed with Android build-tools `zipalign -c -P 16 4`.
  This is not a runtime or ELF compatibility test.
- APK path: `build/app/outputs/flutter-apk/app-debug.apk`.
- No Android device was attached. Voice accuracy, screen-off operation, battery,
  audio interruptions, TTS availability and real target-app behaviour remain
  unverified. Follow DEVICE_TESTING.md on the Vivo V30.

The bundled model and debug/native libraries make this initial APK large
(approximately 223 MB decimal). Release builds and ABI filtering can be evaluated
after real-device validation. The build has no shared cloud API credentials.
