# Build notes

Decisions that are not obvious from the code, with why.

## Toolchain

As Stave (12 Nov 2026): Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, compileSdk and targetSdk 36,
minSdk 26, JDK 21 to run Gradle (never Android Studio's bundled JBR 25), Java 17 bytecode.
`androidx.startup` is declared though nothing imports it: the manifest names its
InitializationProvider to strip Emoji2's font downloader, and lint's MissingClass needs the class
on the path. Taking the manifest block out instead lets the downloader merge back
(`verify_manifest.py` caught exactly that on 12 Nov).

## CameraX

- **1.4.2**, not Squish's 1.3.4 and not 1.6. 1.4 is in the laptop's Gradle cache, builds against
  compileSdk 36 and AGP 8.13, and its ResolutionSelector is the non-deprecated way to ask for an
  analysis size. 1.6 offers nothing P0 needs, and nothing in the family has built against it.
- **Preview + ImageAnalysis**, bound to the activity's lifecycle. Analysis at about 640 x 480
  (`FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`), `STRATEGY_KEEP_ONLY_LATEST` on a single analysis
  thread: a slow consumer drops frames, never queues them, and the UI thread never sees a frame.
- **The camera's own state** (`cameraInfo.cameraState`) is mapped to the mode machine's
  CameraState: in use or too many cameras open is BUSY, disabled by policy is DENIED, OPEN is
  GRANTED. A bind failure (no back camera) is DENIED, which lands in Help with its sentence.
- **Settle**: frames are not read for 1.5 s after the first (`LightLevel.SETTLE_MS`). The moto's
  first frames are dark while auto-exposure opens; the first launch said "Dark." in a lit room.

## The six modes on the screen

Two rows of three, not the brief's one row of six. At 411 dp a row of six is 68 dp a button,
and "Translate" at 18 sp does not fit without shrinking; a grid never shrinks a label (Stave).
Each mode is still one TalkBack swipe, `selectable` with `Role.Tab` (the tint is the only mark of
the chosen one), with a state: on, off, or "not on this phone yet".

## Voice

The phone's TextToSpeech, queued so nothing said before the engine binds is lost. Android's
engine cannot pause, so a tap stops the line and keeps it, and the second tap says it again from
its start. With no engine at all the line is shown and the fallback earcon and twin play: the one
path where the app cannot speak, until Squish's neural voices arrive in P2.

## Earcons and haptics

Tones are made in `:engine` (`Feedback.pcm`), played from one static AudioTrack per earcon with
accessibility usage, so they follow the volume TalkBack users already set and start in
milliseconds. Twins are VibrationEffect waveforms of the same length and shape; on a motor without
amplitude control the rhythm is kept and the shape is lost.

## LiteRT delegates (from P2)

LiteRT 1.4.0, never TensorFlow Lite 2.16.1 (Squish: it ran converted models wrong without
refusing them). The delegate is chosen per model by measurement on each phone (CPU XNNPACK first,
GPU where it is measured faster and its output matches the CPU's on the reference input), and
written down here with the numbers. Stave found BATCH_MATMUL 4-10x slower than FULLY_CONNECTED in
LiteRT: attention is exported as fully-connected products.

## Asset packs (from P2)

An install-time pack for Play (`models_pack`, Stave's wiring) and a "full" flavour carrying the
same files in its assets for GitHub and F-Droid; both read as `assets/models/<file>`. The bundle
is built from the play flavour only (bundletool refuses the models twice). Listing states the
size and why: the best models that run on the phone, not the smallest that fit.
