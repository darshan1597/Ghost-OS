# Alarm, unlock and installed-app update

> Historical checks for an earlier version. For current behaviour and setup, use [V4_DEVICE_CHECKS.md](V4_DEVICE_CHECKS.md).


## Required real-phone checks

1. Unlocked with Ghost visible: `set an alarm for six am`. No Ghost approval
   card. Clock receives ACTION_SET_ALARM with EXTRA_SKIP_UI=true, followed by
   ACTION_SHOW_ALARMS if supported. Check the alarm actually exists; vendor
   clock apps may interpret the standard intents differently.
2. Enable Hands-free app launches; grant Display over other apps; enable listening.
   Confirm a visible G bubble. While another app is open, request an alarm and
   open several installed apps. No Ghost confirmation for either.
3. Disable overlay permission or the setting. Background launches must defer to
   a notification rather than claiming the action happened.
4. Lock phone; request alarm. No alarm dispatch while locked. Unlock within
   10 minutes: with the bubble enabled/listening, continue automatically; otherwise
   tap the continuation notification or reopen Ghost. No second alarm confirmation.
5. Cancel while waiting; unlock. The cancelled alarm must not execute. Deferred
   actions expire after 10 minutes; process death discards them.
6. `unlock phone` while locked: tap the unlock notification if needed. The native
   generic prompt requests Android keyguard dismissal; Android still requires
   credentials on an untrusted secure lock screen. Cancelling authentication must
   not execute the pending action. Ghost never captures or enters credentials.
7. Settings > Installed apps: search and open non-hardcoded installed apps. Try
   exact labels, You Tube, a unique word, an ambiguous word, and unknown names.
   Duplicate labels should ask for explicit selection, never pick arbitrarily.
8. Install/uninstall an app and rescan. The list refreshes when Ghost resumes and
   before open commands. Only visible launchable apps in the current profile are
   included; hidden components and other profiles are not enumerated.
9. Stop listening: the bubble and microphone must disappear. The bubble is hidden
   with the screen off; it does not expose history or contact data over keyguard.

## Implementation boundaries

App matching uses local package-manager data; no app list leaves the device.
Permission is optional and explicit. The bubble is a visible, small assistant
window, not a hidden window or a bypass. MainActivity is never shown over keyguard.
UnlockActivity shows only a generic prompt. Deferred alarms/launches resume once
the phone is unlocked and launch permission exists; message drafts never auto-run.

The existing first-build acceptance checklist and architecture describe the
original version. These checks and the current README supersede their alarm
confirmation, exact-name-only, and background-launch details.

References:
- https://developer.android.com/reference/android/provider/AlarmClock
- https://developer.android.com/reference/android/app/KeyguardManager
- https://developer.android.com/guide/components/activities/secure-bal
