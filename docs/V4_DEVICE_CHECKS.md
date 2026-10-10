# Ghost 1.3: listening, visual design and WhatsApp

> Version 1.3.2 defaults to the automation flavor for the selected personal-device prototype. Accessibility and automatic Send exist only in that flavor. The standard flavor remains available for WhatsApp drafts. See [AUTOMATION_SETUP.md](AUTOMATION_SETUP.md).


## Setup on the Vivo

Install the updated APK over the previous Ghost installation. No server is needed.
Enable microphone/notifications, then wait for the bundled model to finish loading.
Allow phone contacts in Settings > WhatsApp contacts. Contacts stay on the device;
the app does not upload them or keep a second saved address book.

For automatic Send, turn on **Send through WhatsApp**, then enable **Ghost WhatsApp
Send** in Android Accessibility. If Android blocks enabling a service for a
sideloaded APK, review Ghost's app-info **Allow restricted settings** option on
the device. Without this service, Ghost opens a draft and you press Send.

## Listening acceptance

1. Test ten attempts in a quiet room, at normal speaking volume: `Hey Ghost`, then
   wait for the greeting and say `open WhatsApp`. Record successful wake/command
   counts separately. Also test screen off, background, and moderate room noise.
2. Try `Hey Ghost set an alarm for six PM` in one sentence. A dedicated grammar
   recognizer and a free decoder share one microphone; the grammar confirms wake
   even if the free decoder transcribes the name differently. Inline commands
   skip a separate greeting to avoid talking over the command.
3. Tap **Speak now** while listening. This starts a command window directly and
   helps distinguish a missed wake phrase from a missed command. Watch **Heard**.
4. Ask `set an alarm`; after the time question, say `six PM` without waking again.
   Commands allow a 20-second window, with a 650 ms segment-joining grace period.
5. Test interruptions, voice Off, preview, repeated commands and Stop. Confirm
   the microphone releases on Stop and resumes after speech. AudioRecord uses
   VOICE_RECOGNITION at 16 kHz. Decoder/model cleanup stays ordered across restarts.
6. Recheck OriginOS battery controls and battery consumption. These are software
   recognizers, not a low-power hardware hotword engine. Real-device accuracy is
   not proven by compilation or text parser tests.

## Bubble and voice

- Enable Hands-free app launches and overlay permission. While listening, the
  orbiting neon ghost is visible outside Ghost and hidden while Ghost is open.
  Tap it to open with a scale-up transition; hold to stop. Leaving Ghost restores
  the bubble. Screen off/locked hides it. Android may alter the launch animation.
- Settings > Spoken responses has Monster man / Monster woman characters and a
  picker for installed offline English voices. Man lowers pitch more; Woman
  preserves a lighter pitch. Android's generic voice metadata does not guarantee
  a gender label. Preview installed voices to select an actual male/female base
  where available. These effects do not turn any engine into a cloned male voice.
- Preview checks generated speech instead of the optional fixed greeting file.
  Supplied greeting audio still plays unchanged. Strength zero removes DSP effects
  while the chosen character controls pitch. Check clarity at low/mid/high strength.

## WhatsApp acceptance

Use two consenting test contacts or your own spare numbers before normal use.

1. A unique phone-contact name opens its chat directly via WhatsApp's number link;
   no manual aliases or repeated search taps are required. Only phone address-book
   contacts are looked up; WhatsApp-only unsaved names/groups are unsupported.
2. Two different full names containing `Harsh` prompt for a choice. Say the full
   name, `first`, or `the second one`, or tap a result. No second Ghost confirmation
   is needed. Multiple numbers for one full name remain distinct choices.
3. With automatic Send enabled, verify the selected chat and exact body before
   the one Send tap. The service checks visible header/composer/button IDs and
   rechecks after a 400 ms settling interval. A non-unique full name requires
   the full phone number to be visible in the header; otherwise use the draft.
4. Test a wrong chat title, edited message, old draft, no WhatsApp, unregistered
   number, locked phone, cancelled request, disabled service, and a layout mismatch.
   None may produce an unverified Send tap. It never retries Send automatically.
5. Automatic Send requests awaiting launch expire after two minutes; an armed
   UI task expires after 30 seconds. Cancel/Stop/disabling automatic Send clears it.
   Normal app/alarm deferred requests retain their ten-minute expiry.
6. A successful click says **I tapped Send**, not **Delivered**. Delivery/network
   status remains WhatsApp's responsibility. Unsupported WhatsApp layouts fall
   back to manual sending. WhatsApp Business is not currently supported.

## References

- [Android Accessibility services](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Android offline voice metadata](https://developer.android.com/reference/android/speech/tts/Voice)
- [Vosk grammar adaptation](https://alphacephei.com/vosk/lm)

No Android device was attached for this implementation. The image shown in chat
is still not a local asset; the interface uses the provisional ghost with neon
cyan/blue/purple styling and animated rings.
