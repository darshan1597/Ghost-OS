# Vivo V30 acceptance checks

> Historical checks for an earlier version. For current behaviour and setup, use [V4_DEVICE_CHECKS.md](V4_DEVICE_CHECKS.md).


Run only on your own phone and use a test recipient for messaging.

1. Install the debug APK; open Ghost. Text `help` must work with Wi-Fi/mobile
   data disabled and the laptop shut down.
2. Enable listening. Reject permission first: expect an explanation, no active
   microphone, and working text controls. Grant permissions through settings
   and enable again. Expect a visible notification with Stop.
3. Say `Hey Ghost`, wait for the greeting, then `open settings`. With Ghost
   visible and the phone unlocked, expect a launch request. Check the actual app.
4. Repeat the wake phrase in quiet and noisy rooms and with your normal accent.
   Record missed wakes, false wakes, and time to acknowledgement. With the voice
   mode Off, wait approximately one second after the wake phrase before speaking.
5. Set voice mode Brief, Full, and Off. Off must not play greeting or TTS.
   Voice engine should be an installed offline English voice; airplane mode must
   not cause cloud speech calls. Missing voice should produce a visible warning.
6. Save a test alias with an international number. Request `message Harsh on
   WhatsApp with hi`. Review name, number, exact text. Cancel and verify no draft
   opened. Repeat, approve, inspect WhatsApp. Sending remains manual.
7. Try an unknown alias, uninstalled app, ambiguous alarm time, malformed number,
   missing message body, unsupported request. Expect an explanation, no guess.
8. Request `set an alarm for six am`, review, open clock. Check whether the clock
   actually saved an alarm. Remove test alarms yourself after checking.
9. Lock screen while listening. After 1, 10, and 30 minutes say the wake phrase.
   Observe response and notification. App launches should require unlock/review.
   Do not consider the screen-off requirement verified until this passes on Vivo.
10. Start playback/call, connect Bluetooth, mute microphone globally, revoke
    microphone permission. Check safe failure and ability to stop/restart. Audio
    routing and interruption handling may require follow-up work.
11. Press Stop in the app and in the notification while loading, speaking, and
    listening. Verify Android's microphone indicator clears and no new command
    executes. Reject a stale review action after cancellation.
12. Restart phone or force-stop Ghost. Listening must stay off until explicitly
    enabled. Reopen: aliases and mode persist; session history does not.
13. Measure battery during 1-hour idle, listening, and screen-off listening runs.
    Adjust OriginOS background settings only with the user's awareness. The
    partial wake lock trades battery for screen-off processing; measure before
    leaving listening enabled all day.

There is no connected Android device during initial implementation. Automated
tests and APK compilation are not evidence that these device checks passed.
