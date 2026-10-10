# English recognition and monster voice (1.2)

> Historical checks for an earlier version. For current behaviour and setup, use [V4_DEVICE_CHECKS.md](V4_DEVICE_CHECKS.md).


## Automated coverage

The alarm regression includes `set an alaram for 6pm`, polite prefixes, `six p.
m.`, spaced PM, spoken minute values, and an explicit evening phrase. Invalid
times and missing/ambiguous periods are still rejected. Message payloads remain
unchanged. PCM checks cover exact bypass at zero, silent input, bounded output,
stereo isolation and malformed/unsupported WAV formats.

## Check on your Vivo

1. Type `set an alaram for 6pm`. Expect an alarm request, not the old generic
   capability-list response. Check the clock app result.
2. Say `Hey Ghost set an alarm for six PM` as one sentence. The wake recognizer
   preserves the tail and dispatches it after its greeting. This requires endpoint
   detection, so greeting latency differs from the original partial wake detector.
3. Say `Hey Ghost`, wait for its reply, then give the same command. Watch Heard
   text to distinguish speech transcription errors from command-parsing errors.
4. Try a brief pause within a command. Completed segments are joined with a
   900 ms grace period; long pauses can still end the request. The command window
   remains bounded at 12 seconds.
5. Ask `set an alarm`. Ghost asks for a time; wake it again and say `six PM`.
   Cancel discards the clarification. Unknown requests repeat what was heard and
   ask for a rephrase rather than listing all capabilities.
6. Preview the voice at strengths 0, 0.5 and 1. Verify every generated reply uses
   the selected effect and remains understandable. Zero uses direct natural TTS.
   A provided greeting MP3 is intentionally played unchanged.
7. Test speech interrupted by Stop, a new command, voice Off, and phone calls.
   Verify the microphone resumes after speech and clears after Stop. Device audio
   routing, focus and sound quality are not proven by pure signal-processing tests.
8. Confirm temporary `ghost_speech_*.wav` and `ghost_effect_*.wav` files are removed
   after playback/cancellation. Microphone audio is not saved. An unexpected process
   kill can leave temporary cache files; the next app startup removes them.
9. Test background/screen-off operation and battery again. Full wake recognition
   improves inline commands but is not a low-power hardware wake engine.

## Logo asset

The attached chat image was visible to the assistant but not available as a local
file. The exact image can be bundled when provided at
`assets/branding/ghost_logo.png`. Until then the app and launcher use a provisional
code-drawn ghost. Colours are derived visually from the supplied image: cyan,
electric blue, purple, and near-black. No claim that this fallback is the exact logo.
