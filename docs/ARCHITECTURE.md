# Ghost Android assistant (1.3)

> Version 1.3.2 defaults to the automation flavor for the selected personal-device prototype. Accessibility and automatic Send exist only in that flavor. The standard flavor remains available for WhatsApp drafts. See [AUTOMATION_SETUP.md](AUTOMATION_SETUP.md).


Flutter provides the interface; Kotlin owns audio and actions. English only.
No account, backend, OpenRouter, telemetry, or runtime model download is needed.
Vosk ships in the APK and unpacks into private storage. Android provides installed
English offline TTS voices.

## Components

- Flutter: futuristic shell, assistant input, settings, contact choices, app browser
  and history. Platform channels communicate with the native coordinator.
- GhostRuntime: process-wide settings, commands, transient history and pending
  actions. Execution remains independent of the Flutter widget lifecycle.
- OfflineCapture / GhostListeningService: user-started microphone foreground
  service. One 16 kHz AudioRecord stream feeds a constrained wake grammar and a
  free English decoder. Stop releases the microphone and ordered cleanup closes
  decoders before their model. A partial CPU wake lock supports screen-off work.
- SpeechOutput / MonsterEffects / WaveProcessor: installed offline voice selection,
  character pitch, PCM bass/growl/echo, playback and temporary-file cleanup.
- PhoneContacts / ContactMatcher: permission-gated phone contacts; full-name/word
  matching and duplicate choices. No manual contact alias database is required.
- WhatsAppAccessService / WhatsAppSendGuard: optional user-enabled Accessibility
  service scoped to WhatsApp. One explicit message command arms one Send attempt.
- AssistantBubble: optional visible animated overlay outside Ghost; hidden while
  Ghost is foreground or the screen is locked/off. Tap opens with scale-up;
  hold stops listening.
- MainActivity: channels, activity visibility and Android permission requests.

## Recognition and speech

The wake grammar confirms Hey Ghost even if the free decoder mistranscribes the
name. Wake alone greets then starts a command window; an inline wake command
preserves the command tail and dispatches without an extra greeting. Speak now
bypasses wake detection. Commands have a 20-second window and a 650 ms grace period
for continued speech segments. Questions/rephrase prompts listen for an answer
without another wake. Recognition pauses during replies to avoid self-triggering.

This is software recognition, not a low-power hardware hotword engine. Wake
latency, accents, false activations, interruptions and OriginOS battery behaviour
need measurement on the Vivo. Errors stop listening; restart is explicit.

Man/woman character presets set deeper/lighter pitch. A picker exposes all
installed offline English base voices; Android does not universally label gender.
A preset is a voice effect, not a cloned/trained male voice. Strength zero bypasses
DSP while character pitch remains. Optional voice/greeting.mp3 plays unchanged.

## Actions

App opening scans visible current-profile launcher apps. Exact app labels,
known package shortcuts and unique word matches work; ambiguity never selects
the first app silently. No unrestricted installed-package permission is needed.

Alarm parsing accepts explicit AM/PM and unambiguous 24-hour times, including
spoken numbers and alaram. Missing times ask a question. ACTION_SET_ALARM uses
EXTRA_SKIP_UI=true without Ghost confirmation. Showing the alarm list is optional;
Ghost cannot verify that the clock saved the alarm.

WhatsApp resolves phone contacts, then opens the numbered chat with a wa.me link.
Unsaved WhatsApp-only contacts and groups are unsupported. Ambiguous short names
ask for a full name or ordinal. Without Send assistance, the draft opens directly
and the user taps Send. With user-enabled assistance, the visible unique full
name (or full phone number), exact composer text and Send button must match.
The service rechecks after 400 ms, clears the armed task before one click, and
never retries. Layout mismatch leaves a draft. It reports the tap, never delivery.

A resumed unlocked Ghost activity or permitted visible overlay may launch supported
actions. Locked/background requests wait for unlock and launch eligibility, with
a continuation notification. App/alarm expiry is ten minutes; automatic Send
launch expiry is two minutes. An armed WhatsApp task expires after 30 seconds.
Cancel/Stop/disabling assistance clears pending Send. Android authentication is
required for unlock; no PIN entry or permission bypass is implemented.

## Privacy and lifecycle

Microphone samples stay in memory. Generated speech uses private temporary WAV
files deleted after playback/cancellation; stale speech cache is cleaned at startup.
Contacts, audio and transcripts are never uploaded. Voice settings are private;
Android backup is disabled. History is memory-only and capped at 50 records.
Pending choices/actions are transient. Killing the process ends the session;
listening is not restarted silently after force-stop or reboot.

## Extension path

Keep native state authoritative. Introduce a separate action registry as adapters
grow, with explicit permissions, launch requirements and verification outcomes.
Custom wake names, multilingual models, assistant-role integration, a more efficient
wake engine and repeatable device reliability measurements remain future work.
Automation outside the narrow WhatsApp Send flow is unsupported.

See [1.3 device checks](V4_DEVICE_CHECKS.md).

## Primary references

- https://alphacephei.com/vosk/android
- https://alphacephei.com/vosk/lm
- https://developer.android.com/guide/topics/ui/accessibility/service
- https://developer.android.com/reference/android/speech/tts/Voice
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://docs.flutter.dev/platform-integration/platform-channels
