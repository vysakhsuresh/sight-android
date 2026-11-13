# Working on Ikshana

Read this first. Every session starts with no memory of the last one. The brief is
`docs/IKSHANA_BRIEF.md`; the work order is `docs/ROADMAP.md`, worked from its first open box;
the command that starts a session is `docs/LAPTOP_COMMAND.txt`.

Ikshana is a camera that answers, offline, built for the blind first. The owner's three rules
sit above everything: **dependable enough to be honoured; it never goes silent; size is not a
constraint, capability offline is.** No INTERNET permission, ever.

## Where you are decides what "verified" means

**Never write "verified" for something this session could not observe.** The laptop build and the
JVM suites settle a build; only the phone settles a feature; only a blind tester settles a
feature for a blind user. "Logged" (the app wrote what it handed to the voice) is not "heard".
`docs/PENDING_TESTS.md` is what is built and not seen, in run order; `docs/DEVICE_FINDINGS.md`
is what was seen, dated.

## The laptop (Windows, PowerShell)

Sessions run in PowerShell. Set the environment once per session:

```powershell
$env:JAVA_HOME="$HOME\.jdks\jbr-21.0.11"; $env:ANDROID_HOME="C:\Users\vysak\AppData\Local\Android\Sdk"; $env:PYTHONUTF8="1"
```

- JDK 21 (`jbr-21.0.11`), never Android Studio's bundled JBR 25 (Gradle cannot run on it).
- Python 3.12 is `python`; `python3` is the Store stub. The checkers need `PYTHONUTF8=1`.
- `.sh` files run through Git's bash: `& "C:\Program Files\Git\bin\bash.exe" tools/jvm/jc.sh`.
  `MSYS_NO_PATHCONV` is a Git Bash matter only; from PowerShell adb paths like `/sdcard` pass
  through unchanged. From Git Bash, prefix adb with `MSYS_NO_PATHCONV=1`.
- adb: `C:\Users\vysak\AppData\Local\Android\Sdk\platform-tools\adb.exe`. The phone is the
  owner's moto g84, serial ZY32J8HF2S (attached over wireless adb on 12 Nov). A 4 GB phone joins
  from P2; the owner names it.
- No Node on this laptop. The icon builder renders with Edge headless (`render_edge.py`).
- Never touch a source file while a Gradle build runs; never edit a runner script while it runs.
- **Before any tap:** `dumpsys telephony.registry | grep mCallState` is 0 and Ikshana is the
  resumed activity. **The owner uses the phone while sessions run** (12 Nov: they were choosing
  modes in the app minutes after the first install). Read the log before assuming a change in
  state was yours. Never delete the owner's media; never sweep in the owner's data.

## Verifying, in order of what it costs

```
sh tools/jvm/jc.sh                 # every engine suite, ~15 s, Gradle cache's compiler
sh tools/jvm/jc.sh fallback light  # some suites
sh tools/jvm/run_desktop.sh        # suites + the three source checkers
sh tools/jvm/run.sh                # the portable runner CI uses (fetches its compiler)
.\gradlew :engine:test assembleDebug lintDebug   # the gate that settles a build
python tools/verify_manifest.py    # after a build: CAMERA, and nothing it may not have
```

Push gate: `.\gradlew :engine:test assembleDebug assembleRelease lintDebug` and the manifest
check. CI (`android.yml`) runs the same.

Every pure rule gets a suite in `tools/jvm` (an `object XChecks : Suite`, named in `Registry.kt`;
`RunnerChecks` fails on a file no runner names). **Every suite must have been seen failing**:
copy `engine/src/main/kotlin` to the scratchpad, put the fault in, run
`IKSHANA_ENGINE_SRC=<copy> IKSHANA_JVM_OUT=<own out> sh tools/jvm/jc.sh <suite>`, read the
failure. Check the fault is actually in the file first (`cmp` against the original): a pattern
that matched nothing reports a pass. When a mistake escapes, write the check that would have
caught it before the fix.

## The shape

- `engine/` pure Kotlin: `mode/` (the modes, the fallback chain, every sentence by id, the
  machine), `light/`, `gesture/`, `feedback/` (earcon PCM and haptic twins).
- `app/`: `Session` carries out the machine's effects and is the only sender of events;
  `MainActivity` owns the doors (permission, picker, thermal); `CameraDoor`, `Voice`,
  `FeedbackPlayer`, `Labels`, `ui/`.
- `ARCHITECTURE.md` draws the machine; `docs/FALLBACKS.md` lists every sentence.

## The never-silent rule, mechanically

A new failure is a new `Event` or `Hint` and, if it needs words, a new `Line`. A new `Line` needs
`say_<id>` in `values/strings.xml` with exactly the engine's English, and a row in
`docs/FALLBACKS.md`; `SpeechTableChecks` fails until all three agree. `FallbackChecks` then walks
it through every state. A new mode needs a row in `ModeTable` with a mode below it; the chain
must end at Help.

## Conventions worth not rediscovering (carried from Squish and Stave)

- Comments say why, not what; where a line looks odd, the comment names the bug that made it so.
- `String.format(Locale.ROOT)` for anything machine-readable; a localised string is never cut
  with an ASCII literal. Count code points, not chars; three letters is a Latin rule.
- `selectable`/`toggleable` where a tint is the only mark (the mode row); `clickable` for a plain
  button (selectable on a button announces "not selected"). A control whose content is a glyph
  needs a name, and the glyph's own text cleared (`clearAndSetSemantics`).
- Compose compares unstable parameters by identity: `app/compose_stability.conf`; never mutate a
  list you have passed down. `pointerInput(Unit)` keeps its first lambdas: read through
  `rememberUpdatedState`.
- Every float into a file goes through `putFinite`; a native handle can be negative.
- Engine tables reach the screen by id through `text/Labels`, with a translation per language.
- Letter-spacing is for Latin capitals only; it pulls Indic conjuncts apart.
- LiteRT 1.4.0, never TensorFlow Lite 2.16.1. No converter works as delivered: one flat input,
  one flat output, every state explicit, the converted model checked on an input the conversion
  never saw. In LiteRT, BATCH_MATMUL is 4-10x slower than FULLY_CONNECTED.
- Type is LayerLink's (the owner's choice, 12 Nov): Space Grotesk for every word, set in the
  window theme too; numbers in the platform monospace with `tnum`.

## Things the first day taught (12 November), for the next stranger

- **The owner tests on the phone as soon as a build lands and judges by what is spoken.** A mode
  that only says "not ready" reads as broken; every unbuilt mode now says what it will do. The
  owner rejected the almond-eye icon ("no eyelid"); the mark is the beam into a black hole.
- **The owner wants decisions, not menus.** Decide for the blind user, build it, say what was
  decided. Stop only for what a permission or the owner's phone and data require.
- **Running or building code fetched from outside (llama.cpp) is refused by this laptop's
  session permissions** until the owner allows it; say so plainly, never route around it.
- **Interim models are allowed when the owner asks for function now**, if labelled borrowed and
  placed as the last tier so ours replace them by score.

- **The setup command ran `git init` in the home folder**, because the `cd` into an empty
  project folder did not hold. `C:\Users\vysak\.git` (with this repo's remote) was moved into the
  project. If `git rev-parse --show-toplevel` ever says `C:/Users/vysak`, stop and look.
- **`core.autocrlf` is on here**: `.gitattributes` keeps `*.sh` and `gradlew` LF.
- **Lint's MissingClass on `androidx.startup.InitializationProvider`** means declare
  `androidx.startup`, not delete the manifest block: deleting it merged Emoji2's font downloader
  back, and `verify_manifest.py` caught it.
- **`state_on`/`state_off` are private Compose resource names**; ours are `mode_state_*`.
- **The camera's first frames are dark** while auto-exposure opens: the first launch said
  "Dark." in a lit room. Anything that reads brightness waits `LightLevel.SETTLE_MS`.
- **Bash heredocs containing `'` or `'''` broke the Bash tool's parser twice.** Write a script
  file with the Write tool and run it instead; check every scripted edit landed (grep or cmp).
- **Run the icon builder from the Bash tool, not the PowerShell tool.** From PowerShell, headless
  Edge and Chrome both exit writing no screenshot (13 Nov), and leave hung headless processes on
  the icon profile; the same script from Git Bash renders all 22 files.
- **`msedge.exe --headless --screenshot` returns before the file exists**, and a running Edge
  swallows the job unless `--user-data-dir` is its own; `render_edge.py` waits for the file.
- **`jc.sh` looks for jars in the Gradle cache, then `~/.ikshana-kt`, then `~/.stave-kt`**: the
  cache lacks kotlin-reflect 2.2.21, and under `pipefail` a `find` over a missing folder used to
  end the script with no message.
