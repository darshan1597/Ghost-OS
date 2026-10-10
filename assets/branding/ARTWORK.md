# Home mascot

Asset: `assets/branding/ghost_hero.png`.

Created with the built-in imagegen tool using the user's home-screen screenshot
as the reference. The output retains transparency and was copied into this
workspace. Flutter renders the header, greeting, background, inputs, cards and
navigation separately, so they remain responsive and interactive.

Final prompt:

> Extract/recreate ONLY the central luminous 3D ghost mascot from the provided GhostOS phone UI reference, as a production app illustration on a genuinely transparent background. Match the reference mascot closely: curved flowing cyan-blue-violet ghost body, black oval face, two glowing white vertical oval eyes, glossy cyan rim light on left/top and purple right edge, one cyan-to-violet luminous orbital ring crossing diagonally around its body. Preserve silhouette and lighting. Center full mascot and ring, generous transparent margin, square canvas. No text, no phone, no UI, no background, no extra objects. Save output for integration into Flutter.

The three home cards use implemented actions: opening WhatsApp, asking for an
alarm time and opening Gmail. Email summaries and general reminders shown in the
design reference are not implemented. Android supplies its actual system bars;
the iPhone frame/notch in the reference is not part of the app.


## Ghost Link branding (2026-10-08)

`android/ghost-link/src/main/res/drawable-nodpi/link_logo.png` is a reference-based
asset prepared with the built-in imagegen skill from the supplied Ghost Link
logo. It is a generated adaptation, not a byte-for-byte copy of the attachment.
`link_hero.png` derives the floating phones/ghost artwork from it with alpha
transparency. The launcher uses an inset adaptive foreground plus navy background.
The native screen uses code-drawn waves, badges and controls; it is not a screenshot.
