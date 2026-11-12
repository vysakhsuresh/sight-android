# Roadmap

Section 5 of `docs/IKSHANA_BRIEF.md`, as a checklist. Worked from the first open box; nothing is
skipped. Three marks, as Stave keeps them:

- `[x]` done: seen on a phone where the task is something a phone shows, or written and executed
  where it is a document or a tool;
- `[~]` built, not seen: compiled and its rules executed on the JVM, but not observed doing its
  job on a phone (and, for a release gate, not passed by a blind tester);
- `[ ]` not built.

Built-not-seen is not done. What is pending on a phone is in `docs/PENDING_TESTS.md`, in order.

## P0 — Foundations

- [x] **1. Repo.** Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21 as Stave; `:engine` pure Kotlin,
  `:app` Compose and CameraX; GPL-3.0; `THIRD_PARTY_NOTICES.md` from day one. Installed and run
  on the moto, 12 Nov. *Not yet:* `app/src/main/cpp`, which arrives with the first native code
  (nothing in P0 is too slow for Kotlin; the brief's native work is LiteRT and the VLM loop,
  P2–P3). Check: `.\gradlew :engine:test assembleDebug lintDebug` green on the laptop.
- [x] **2. Manifest.** CAMERA and VIBRATE, INTERNET and ACCESS_NETWORK_STATE removed with
  `tools:node="remove"`, storage, location, contacts, microphone and phone state removed too.
  `tools/verify_manifest.py` allows a closed list (CAMERA required) and was seen failing on
  INTERNET, RECORD_AUDIO, BLUETOOTH and a missing CAMERA, and on Emoji2's downloader merging back
  (12 Nov, a real catch). On the moto, `dumpsys package` lists exactly CAMERA and VIBRATE.
- [~] **3. Theme and components; the six-mode row; earcon and haptic players with a Settings
  switch each.** Ink with the Iris accent, Space Grotesk as LayerLink sets it (owner, 12 Nov), body 18 sp, 64 dp
  targets. The six modes in two rows of three (a row of six would shrink "Translate"; BUILD_NOTES
  says why). Earcons and haptic twins are made in `:engine` (`EarconChecks`). Seen: the screen and
  the six named modes on the moto; the earcons logged as played. *Not seen:* the earcons heard,
  the twins felt, the Settings switches driven. `PENDING_TESTS` 1–2.
- [x] **4. CLAUDE.md** for the next stranger, laptop section in PowerShell form.
- [x] **5. Tools from Stave**: the five `check_*.py`, `verify_manifest.py`, `tools/jvm/run.sh`
  (now `;` and `JAVA_HOME` on Windows), `Registry.kt`, `RunnerChecks.kt`, `jc.sh`,
  `run_desktop.sh`. All run on the laptop, 12 Nov.
- [~] **6. CI.** `android.yml` (engine suites with no SDK, checkers, lint, debug and release
  builds, manifest) and `release.yml` (signed when the secrets exist). *Not yet:* `native.yml`,
  which has nothing to build until the first native code; it is written with that code, not
  before. CI's first result on GitHub: see `PENDING_TESTS` 0.
- [x] **7. Docs.** `README.md`, `ARCHITECTURE.md`, `BUILD_NOTES.md`, `docs/COMPETITORS.md`,
  this file, `docs/DEVICE_FINDINGS.md`, `docs/PENDING_TESTS.md`, `docs/MODELS.md`, fastlane.
- [x] **7a. docs/FALLBACKS.md** (the owner's rule): every failure, the mode it falls to, the
  sentence spoken. Held to the code by `SpeechTableChecks`.
- [x] **7b. FallbackChecks** over the mode machine: every state through every event, 4,723,920
  steps, none silent; seen failing on four separate faults (a silent repeat, a cycle in the
  chain, a failed model still used, Help without its doors). `SpeechTableChecks` seen failing on
  a reworded line.
- [~] **P0 check:** CI green on an empty app that opens on the camera, speaks "Ikshana" once,
  shows the six modes to TalkBack with names and states, and declares only CAMERA. Seen on the
  moto: opens on the camera; "Ikshana." logged once as handed to the voice; the six modes named
  in `uiautomator dump`, zero NAF; requested permissions CAMERA and VIBRATE (VIBRATE is normal
  level, allowed by the owner). *Not yet:* the word heard; TalkBack driven over the row to hear
  the states; CI's first run. `PENDING_TESTS` 0–2.

Brought forward from P3 because the owner's rule needs a floor that works with no model: **light
level** (`LightLevel`, `LightChecks`), the mode Read falls to. Seen on the moto (logged, not
heard): "Dark." during auto-exposure, then "Bright light." in a lit room. The 1.5 s settle that
fixes the false "Dark." is built and not yet seen (`PENDING_TESTS` 1).

## P1 — The test set and the harness, before any model

- [ ] **8. The Ikshana set:** 500 photos on the moto (notes both sides and folded, strips, bus
  boards, menus, signs, notices, chits, pages; ml, en, hi, ta, kn; a third bad on purpose), the
  truth typed beside each with fields marked; 50 scenes with a line and three testers'
  questions. Release asset `eval-v1`, index in the repo. *Needs the owner and testers.*
- [ ] **9. The harness** in `tools/jvm`: CER per script, field exact match, currency top-1,
  Describe rubric, latency and peak memory per model per phone; scores into `docs/MODELS.md`.
  Check: a deliberately wrong transcript scores at the rate it was given, a perfect one at zero.
- [ ] **10. The frame gate** in `:engine`: blur (Laplacian variance), exposure (histogram),
  motion (frame difference), tilt (gravity); thresholds from the set's bad third.
  Check: `FrameGateChecks` on synthetic frames, negative-tested with the thresholds moved.

## P2 — Read (v0.1)

**Interim, at the owner's request (12 Nov):** live Read runs now on a borrowed recogniser
(Tesseract fast, Apache-2.0; `docs/MODELS.md`), with the engine's rules in front of the voice
(`ReadTextChecks`: junk dropped, a line never said twice, the sideways sensor plane turned upright,
each line in its script's voice, "nothing found" paced). Seen on the moto: the reader opens and
reads a frame in 350-530 ms. *Not seen:* text read correctly, heard. The items below stand: our own
OCR replaces it by score.

- [ ] 11. The camera frame path (YUV to the model with no UI-thread copy; frame skip; the gate in
  front). Check: 30 fps preview with the gate running, UI thread under 4 ms (`dumpsys gfxinfo`).
- [ ] 12. The synthetic text pipeline under `tools/models/ocr/`, every real set's licence pinned first.
- [ ] 13. Detector (~15M) and recogniser (~10M, a head per script), exported to LiteRT, checked
  against the reference run on a held-out image. Gate: Malayalam CER under 8% on the clean two
  thirds, in `docs/MODELS.md`.
- [~] 14. Reading order and script identification (script to voice built: `Scripts.localeOf`, in `ReadTextChecks`; reading order is Tesseract's for now) (`ReadingOrderChecks`, `ScriptChecks`).
- [ ] 15. Field rules: expiry, dose, MRP, amount, phone, PIN, bus, platform (`FieldChecks`).
- [ ] 16. TTS: Squish's Indic voices, system TTS fallback, normalisation (`SpeakChecks` under
  ml-IN, hi-IN, ta-IN).
- [ ] 17. The Read screen. **Release gate:** a blind tester reads a strip, a board and a chit
  with the display off, on the moto and the 4 GB phone.

## P3 — Money and Describe (v0.2)

**Interim Money, at the owner's request (12 Nov):** the note's printed value read by the borrowed
reader (English digits only), spoken only when two of the last three frames agree, and felt as a
buzz pattern that differs for every note (`MoneyChecks`, each rule seen failing). Not a currency
model: it reads numerals, so a folded note that hides them gives nothing, and it never judges a note
genuine. Built, **not seen on a phone** (the phone left with the owner before it could be installed).
Describe, Find, Ask and Translate now say what they will do when chosen (`say_about_*`).

- [ ] 18. The currency set and CNN. Check: top-1 above 99% held out, every miss listed.
- [ ] 19. The vision-language model, chosen by the harness, fine-tuned, converted (LiteRT or
  llama.cpp by measurement), checked against the reference run; the OCR/VLM swap designed for 4 GB.
  **Started 12 Nov.** [~] The rules on either side of the model are built in `engine/see`
  (`SeeChecks`, each of six faults seen failing): the frame gate (no black or smeared frame is
  shown to the model, the listener is told why), the cut of a description to two sentences
  without preambles, Find's box-to-direction (no box, no direction), Ask's ready questions and
  the prompts. Decided by measurement-to-come rather than taste: llama.cpp is the first runtime
  tried (a vision model's LiteRT conversion is the long road); three openly licensed candidates
  of 0.5, 1 and 2 billion parameters are downloaded for the laptop comparison on 60 VizWiz
  photos (photos taken by blind people, with the question each asked). [~] Describe is wired end
  to end: llama.cpp b12319 as a submodule built into `libikshana_vision.so` (arm64), the JNI
  bridge (`app/src/main/cpp/vision.cpp`), `VisionModel`, colour frames from `CameraDoor` only
  while Describe runs, `DescribePacer` (speaks again only when the scene changes), "Looking."
  once on entering. The model is found in the app's external files `models/vision/` (pushed by
  adb for now; the asset pack and "full" APK later). Built; **not seen on the phone**.
- [ ] 20. Colour (`ColourChecks` from Still's table). Light is done above.
- [ ] 21. Money and Describe screens. **Release gate:** a blind tester counts a mixed bundle with
  the display off; describes a kitchen and a street; ten minutes without thermal throttle.

## P4 — Find (v0.3)

- [ ] 22. Localisation, `GuidanceChecks` (axes swapped as the negative test), the throttle.
  **Release gate:** a door, a cup and a named bottle found in under 20 s each, display off.

## P5 — Translate (v0.4)

- [ ] 23. IndicTrans2 distilled, quantised, checked against the reference translation.
  Check: BLEU within a stated margin; a Kannada board spoken in Malayalam on the moto.

## P6 — Ask (v0.5)

- [ ] 24. Questions over the vision model with the seen/thought split; voice input through the
  phone's own speech input. **Release gate:** ten testers' questions.
  Owner, 12 Nov ("everything is needed"): Ask must take a spoken question even where the phone
  has no offline speech input, so an own offline recogniser comes with it, and with it
  RECORD_AUDIO, the first permission beyond CAMERA. Until then the six ready questions in
  `engine/see` (`Question`) are chosen by tap or swipe.

## P7 — Polish and release (v1.0)

- [ ] 25. Display-off sweep by two blind testers; locale sweep; low-vision mode.
- [ ] 26. Performance: UI under 4 ms with every model; 0 frames at rest; 30-minute heat and battery.
- [ ] 27. Store assets, F-Droid metadata, reproducible release, the "full" APK, checksums.
- [ ] 28. `DEVICE_FINDINGS` filled; `PENDING_TESTS` emptied or honest.

## After v1.0

The scripts and languages beyond the first five, one per release, each with its number in
`docs/MODELS.md`: every script of the 22 scheduled languages for Read, then the world's major
scripts; every Indian language with an open voice.
