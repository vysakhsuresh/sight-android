# Architecture

## Modules

```
:engine   pure Kotlin, no android.*; executed on the JVM by tools/jvm (run.sh, jc.sh, :engine:test)
  mode/       Modes.kt (the modes, what each needs, the one below), Lines.kt (every sentence by id),
              ModeMachine.kt (state + event -> state + effects)
  light/      LightLevel.kt (mean luma, bands, hysteresis, settle time)
  gesture/    Gestures.kt (tap / swipe classification in dp and ms)
  feedback/   Feedback.kt (earcon PCM and the haptic twin of each)
:app      Compose, CameraX, the voice, the doors
  Session.kt          carries out the machine's effects; the only place events are sent from
  MainActivity.kt     permission, photo picker, thermal listener -> events
  IkshanaApplication  the crash catcher and relaunch
  camera/CameraDoor   preview + analysis stream; camera state -> CameraState
  voice/Voice         system TextToSpeech, queued, pause = stop-and-remember
  feedback/FeedbackPlayer  static AudioTracks for earcons, VibrationEffect for twins
  text/Labels         engine ids -> string resources
  ui/                 Theme (ink, Iris, Space Grotesk as LayerLink sets it), HomeScreen
```

`:app` depends on `:engine` and never the reverse. `run.sh` and `RunnerChecks` both refuse an
`android.*` import under `engine/`. Native code (`app/src/main/cpp`) arrives with the first thing
Kotlin cannot do fast enough, with a Kotlin twin executed by a suite wherever the arithmetic can
be mirrored (Stave's LiveTune lesson).

## The mode state machine and its fallbacks

The owner's rule, **it never goes silent**, is a property of one pure function:
`ModeMachine.step(State, Event): Step(State, effects)`.

- **State**: the mode the user wants, the mode and tier actually running, the camera state
  (granted, denied, busy, picture), the models on the shelf, the models that failed this session,
  hot or not, and the last sentence said.
- **Events**: Started (with the last mode and whether it crashed), Choose, ModelFailed, Camera,
  NothingFound (with the frame gate's hint), TimedOut, Thermal.
- **Effects**: Say (a line by id with the modes it names), Sound (an earcon; the twin goes with
  it), Run (a mode at a tier), OfferCameraDoors.

Landing: from the wanted mode, walk down `ModeTable` until a mode whose camera need is met and
one of whose tiers has every model present and not failed. The walk ends at Help, which needs
nothing. When the cause clears, the machine climbs back to the wanted mode.

```
Money ──► Read ──► Light level ──► Help
Describe ─► Read          ▲
Translate ─► Read         │ (pure arithmetic, camera or picture)
Find ──► Describe ─► Read
Ask ───► Describe ─► Read
```

`FallbackChecks` walks every state through every event (4.7 million steps) and holds the
invariants; `docs/FALLBACKS.md` lists every sentence; `SpeechTableChecks` holds the two together.

## The frame path (P0, and where it goes)

CameraX binds a Preview (for the sighted half) and an ImageAnalysis at about 640 x 480 with
KEEP_ONLY_LATEST on its own thread. In P0 the analyser reads only the Y plane's mean, twice a
second, after a 1.5 s settle, for Light level. P1 puts the frame gate (blur, exposure, motion,
tilt) in front of every model; P2 hands gated frames to the OCR at its own resolution with no
copy on the UI thread (item 11 measures it).

## Where the models live, and the 4 GB phone

Models are read from `assets/models/<id>.tflite`. The Play build carries them in an install-time
asset pack, the GitHub "full" APK inside the APK; both land at that path (Stave's ModelStore
pattern). The app never downloads anything: it has no INTERNET permission. `Session.shelf()`
lists what this install carries; the mode machine runs only what is on the shelf and not failed.

On a 4 GB phone the OCR and the vision model cannot both stay loaded. The swap is designed in P3
(item 19) from measured peak memory, not discovered on the phone: one model resident at a time,
loaded by the mode that needs it, with a failed or refused load reported as `ModelFailed`, which
the machine turns into the lighter twin or the mode below, with its sentence. The lighter twins
are chosen at first run by measurement (load, run one reference input, time and peak memory
against a budget), never by refusing to try.
