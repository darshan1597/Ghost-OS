# Offline cinematic voice delivery - Ghost OS 1.7.0

## Implementation

The primary male preset is **Ghost Cinematic Monster Male**. It uses the official
sherpa-onnx 1.12.20 Kotlin JNI interface, ONNX Runtime 1.17.1 and converted
vits-piper-en_US-ryan-medium, mono 22050 Hz. The model is a real bundled asset,
not a runtime download. A fresh installation can synthesize new text without
network access. No API, server, API key, networking library or external-storage
permission was added for speech. Debug Flutter builds retain their pre-existing
network permission for development tooling.

Ryan exists in the official sherpa model release, which supplies the converted
ONNX, tokens, JSON metadata and eSpeak data. We use that package, not a raw Piper
ONNX assumed to work directly. Its model card identifies RyanSpeech training
data as CC BY-NC-SA 4.0 and fine-tuning from Lessac medium. Lessac's model card
was also inspected; it points to separate dataset terms and is not asserted to
be an unrestricted replacement. Ryan is retained for this personal development
build, with its redistribution constraints recorded in THIRD_PARTY_NOTICES.md.
eSpeak is GPL-3.0-or-later; sherpa's Apache license does not supersede it.

Pinned sources:
- https://github.com/k2-fsa/sherpa-onnx/tree/v1.12.20
- https://github.com/k2-fsa/sherpa-onnx/blob/v1.12.20/sherpa-onnx/kotlin-api/Tts.kt
- https://github.com/k2-fsa/sherpa-onnx/blob/v1.12.20/android/SherpaOnnxTts/app/src/main/java/com/k2fsa/sherpa/onnx/MainActivity.kt
- https://k2-fsa.github.io/sherpa/onnx/tts/all/English/vits-piper-en_US-ryan-medium.html
- https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/ryan/medium/MODEL_CARD

## Runtime and lifecycle

All existing replies still flow through GhostRuntime.say -> SpeechOutput.
Recorded voice/greeting.mp3 takes priority for greeting requests if supplied.
Otherwise the selected neural/Android engine handles the greeting dynamically.
Monster Woman continues to use the existing Android offline voice and effects.
Off/Brief/Full modes and Android offline-voice selection remain available.

NeuralVoiceEngine owns one serial synthesis/playback worker and one reusable
model. The model and tokens are read directly through AssetManager; only eSpeak
requires extraction into private files/ghost-voice/espeak-v1. A completion marker
avoids repeated extraction. No second ONNX model copy is stored on the phone.

AudioTrack plays float PCM at the original 22050 Hz. Sentence callbacks allow
playback before a multi-sentence synthesis completes; the bounded audio buffer
provides backpressure. A generation counter cancels obsolete jobs and clears
queued work. Native inference cancellation is observed at sentence boundaries;
playback writes poll cancellation every few milliseconds. It cannot preempt a
single ONNX inference in progress. Callback exceptions are contained before
returning to JNI. Stop, focus loss and disposal release playback resources.

Audio focus uses transient assistant speech attributes. GhostRuntime continues
to pause Vosk during its own speech and resume after completion. This retains
the existing echo-avoidance behavior, not full voice barge-in/AEC. Typed requests
and Speak now can interrupt a reply. Warm model retention follows app foreground
or listening state; after 90 idle seconds outside an active session, native model
and cache can be released. Explicit disposal shuts down TTS/worker resources.

The memory-only raw speech cache holds at most eight short texts / 500,000 float
samples (2 MB). Generated neural replies create no persistent audio files.
Existing Android effect WAV files are still private and removed after playback.

The JNI callback uses a concrete PcmCallback class because Kotlin 2.4 indy
lambdas do not expose the typed boxed invoke(float[]): Integer method required
by this sherpa release. The device test caught this mismatch; it was fixed and
retested. R8 keep rules preserve JNI configuration/callback members.

## DSP

Tunable values are in CinematicEffects.Preset:
- Pitch: -3.5 semitones, two cross-faded variable-delay grains (45 ms span).
- Speed: 0.92x in the neural synthesizer, independent of output sample rate.
- Saturation mix: 12% at full strength.
- Echo: 85 ms, 9% at full strength.
- Bass: RBJ low shelf at 180 Hz, +3 dB at full strength.
- Limiter ceiling: -1 dBFS (whole-chunk gain reduction, not hard clipping).

The existing Monster strength slider scales bass/saturation/echo. Pitch and
speed remain preset properties, including at strength zero. The lightweight
pitch shifter preserves duration apart from its short padded delay tail and
has a tested frequency shift. It can color consonants; this is not a studio
formant-preserving algorithm. **Independent formant shifting is not implemented.**
No heavyweight DSP library was added. Final cinematic sound quality needs human
listening and tuning; mathematical clipping tests cannot judge that preference.

## Files

Created:
- android/app/src/main/kotlin/com/ghostos/ghost_os/voice/NeuralVoiceEngine.kt
- android/app/src/main/kotlin/com/ghostos/ghost_os/voice/CinematicEffects.kt
- android/app/src/main/kotlin/com/k2fsa/sherpa/onnx/Tts.kt (upstream, unmodified)
- android/app/src/main/assets/voice/ryan/ (complete model package)
- android/app/src/main/assets/voice/licenses/ (license and model-card copies)
- android/app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/ (JNI + ONNX only)
- android/app/proguard-rules.pro
- android/app/src/test/kotlin/com/ghostos/ghost_os/voice/CinematicEffectsTest.kt
- android/app/src/androidTest/kotlin/com/ghostos/ghost_os/voice/VoiceInstrumentation.kt
- scripts/test_voice_offline.py
- docs/VOICE_ASSET_MANIFEST.json, docs/VOICE_STORAGE.json and this report

Modified:
- SpeechOutput.kt: engine routing, audio focus, cancellation, fallback, cleanup.
- GhostRuntime.kt: voice state/settings and warm lifecycle.
- MainActivity.kt: initializeVoice, stopVoice and setVoicePreset channel actions.
- lib/features/assistant/ghost_settings.dart: engine selector and explanatory text.
- android/app/build.gradle.kts: asset compression, ABI packaging, test runner/R8.
- test/widget_test.dart: engine-selection regression test.
- pubspec.yaml, README.md, THIRD_PARTY_NOTICES.md.

No command parser, Vosk decoder, Ghost Link source or unrelated screen was changed.
The workspace had no Git repository. Recoverable pre-change source backup:
build/checkpoints/before-neural-voice.zip (large existing model assets excluded).

## Build and checks

Configuration: Android min SDK 26, compile/target 36; Gradle 9.3.1;
AGP 9.1.0; Kotlin 2.4.0; NDK 28.2.13676358; Java target 17.
ARM64 is packaged for the Vivo. ARMv7/x86_64 native sources remain for other
Flutter target builds. JNI libraries were inspected for 16 KB ELF alignment.

Passed:
- Flutter analysis: no issues.
- Flutter tests: 21.
- Kotlin tests: 42, including existing command/contacts/Link protocol checks and
  new DSP frequency, duration, neutral-processing, echo and peak-bound checks.
- Android debug build and device test APK build.
- Actual Vivo V30 / Android 16 airplane-mode test, Wi-Fi disabled and mobile data
  disabled. Original connectivity restored by the runner's finally block.
- Fresh dynamically constructed speech; neural model reused (load count one).
- Interruption of a long reply; twelve rapid requests without a crash.
- Generated greeting when no recording is bundled.
- Explicit Android offline voice selection and actual fallback when the model
  path is intentionally missing in the test instance.
- Vosk microphone service initialization, coordinated spoken reply, continued
  listening state, and background listening-service survival.

Recorded device run: docs/VOICE_DEVICE_TEST.txt.
Initialization: 1372 ms with phonemizer files already extracted. Warm uncached
playback start: 718 ms for a short greeting; 636 ms for fresh dynamic text.
Cached greeting start: 73 ms. These measure request-to-AudioTrack.play(), not
acoustic output measured with a microphone, and are samples, not percentiles.
The 500 ms uncached target is NOT established; approximately one second was met
in this run. First-ever initialization/extraction was not separately benchmarked.

Still requires human/device acceptance:
- Subjective monster sound quality, consonant intelligibility and comfort.
- User's actual recorded MP3 (none was supplied), including playback priority.
- Spoken wake accuracy across noise/distances and extended screen-off sessions.
- End-to-end alarm saving, WhatsApp delivery and two-phone Bluetooth operations
  (their existing code/regression tests were preserved; not all apps retested).
- Long-running memory/battery profiling, acoustic latency percentiles and other
  phone/ABI testing. Rapid-request checks do not prove absence of all leaks.

## Storage measurements

Final ARM64 debug APK: **224,770,116 bytes** (224.77 MB / 214.36 MiB).
Previous installed universal debug APK: 150,944,391 bytes. Net APK change is
73,825,725 bytes, but this is not an equal-ABI comparison: extra architectures
were removed while the voice was added. The voice's own stored APK entries total
93,319,066 bytes. Native ARM64 ONNX library: 15,988,224 bytes; sherpa JNI:
4,420,720 bytes. The ONNX model is 63,149,198 bytes; all model assets total
81,146,959 bytes. Only JNI and ONNX libraries were packaged, not unused C/C++ APIs.

On-device `du -sk files/ghost-voice` measured 18,822 KiB of extracted phonemizer
storage. No ONNX copy is extracted. APK voice payload plus this allocation is
approximately **112.59 MB** additional installed voice storage, excluding
platform-dependent dex optimization, metadata and cache. This meets the ~120 MB
payload target; Android Settings total allocation was not benchmarked before/after.
Neural generated audio is kept only in RAM, with a bounded 2 MB text cache.
Measurements and archive hashes are in VOICE_STORAGE.json. The final archive
passed `zipalign -c -P 16 4`. This is a debug APK, not a measured production AAB.

## Try on the phone

1. Open Ghost OS -> Settings -> Voice character.
2. Select Ghost Cinematic Monster Male and Monster man.
3. Set Spoken responses to Brief or Full; tap Preview voice.
4. Adjust Monster voice strength. Try 50-60% first; maximum uses the listed mix.
5. Enable listening, say Hey Ghost, then open an app or request an alarm.
6. Repeat with airplane mode enabled; speech/local commands should work.
7. Switch to Android offline TTS or Monster woman to compare the existing voice.
8. To test a recording, place your MP3 at
   android/app/src/main/assets/voice/greeting.mp3 and rebuild.

Build main APK:
flutter build apk --debug --flavor automation --target-platform android-arm64 --no-pub

Build device checks with the Android Studio JDK:
android/gradlew.bat -p android -Ptarget-platform=android-arm64 :app:assembleAutomationDebugAndroidTest

Install the main APK and build/app/outputs/apk/androidTest/automation/debug/app-automation-debug-androidTest.apk.
Run: python scripts/test_voice_offline.py <path-to-adb.exe> <device-serial>
This test speaks aloud and temporarily changes connectivity, restoring it afterward.
