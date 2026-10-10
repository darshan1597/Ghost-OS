# Third-party components

- Flutter: BSD-3-Clause; https://github.com/flutter/flutter/blob/master/LICENSE
- Vosk Android: Apache-2.0; https://github.com/alphacep/vosk-api
- Vosk small English US model 0.15: Apache-2.0, as listed at
  https://alphacephei.com/vosk/models. Source archive:
  https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
  Bundled under android/app/src/main/assets/model-en-us.
  Downloaded archive SHA-256:
  `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`.
- JNA: LGPL-2.1-or-later / Apache-2.0 dual license; use under Apache-2.0.
  https://github.com/java-native-access/jna
- llama.cpp (commit `e60eff95fda7b6e46bf402a8e9f1e86d88ab29c8`): MIT.
  https://github.com/ggml-org/llama.cpp
- Qwen3-0.6B and the optional Q4_0 GGUF weights: Apache-2.0.
  https://huggingface.co/Qwen/Qwen3-0.6B and
  https://huggingface.co/ggml-org/Qwen3-0.6B-GGUF

The license texts and attribution should be reviewed before external release.
Custom voice assets must be provided with rights to distribute them.

Included license texts: `licenses/APACHE-2.0.txt`,
`licenses/FLUTTER-LICENSE.txt`.


## Bundled neural voice (Ghost OS 1.7)

- sherpa-onnx **v1.12.20**, Apache-2.0. Unmodified Kotlin `Tts.kt` and official
  Android JNI release libraries. Source: https://github.com/k2-fsa/sherpa-onnx/tree/v1.12.20
- ONNX Runtime **1.17.1**, MIT; version verified in the supplied native binary.
  Source: https://github.com/microsoft/onnxruntime/tree/v1.17.1
- eSpeak NG phonemizer is statically included in sherpa JNI: **GPL-3.0-or-later**.
  The pinned runtime build references source commit
  `f6fed6c58b5e0998b8e68c6610125e2d07d595a7` at
  https://github.com/csukuangfj/espeak-ng . Its COPYING and runtime build recipe
  are recorded in this project. Apache-2.0 on sherpa does NOT remove the
  phonemizer's GPL obligations. An external binary release needs the applicable
  corresponding source and license compliance; this is a personal development build.
- Voice: converted **vits-piper-en_US-ryan-medium**, one speaker, 22050 Hz.
  https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-ryan-medium.tar.bz2
  Original card: https://huggingface.co/rhasspy/piper-voices/blob/main/en/en_US/ryan/medium/MODEL_CARD
  The card identifies RyanSpeech data under **CC BY-NC-SA 4.0**, fine-tuned from
  Lessac medium. Preserve attribution and noncommercial/share-alike restrictions;
  no unrestricted commercial-model license is asserted. Do not treat this model
  as cleared for a commercial release. No model weights were modified.
- Additional voice/DSP dependency: none. CinematicEffects is project code.

Bundled license/card copies: `android/app/src/main/assets/voice/licenses/`.
The CC license text is mirrored from the Creative-Commons-4.0-Markdown repository;
canonical license: https://creativecommons.org/licenses/by-nc-sa/4.0/ .
Model and library byte sizes and SHA-256: `docs/VOICE_ASSET_MANIFEST.json`.
