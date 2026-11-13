# Ikshana — the camera that answers, and cannot upload what it sees

Written 12 November 2026, from the sibling repositories (Squish, Still, Sheaf, Stave,
Deja, LayerLink, Kshana) and the five competitors' own listings, help centres and press.
**Nothing in the competitor sections was seen by driving those apps**; it was read. Where
a claim is marketing language it is repeated as marketing language, and where sources
disagree both are given.

Six parts:

1. The name
2. What the five do, and where each is weak
3. What Ikshana builds, pillar by pillar, and why each beats them
4. The design language, which for this app is mostly sound
5. The task audit, in build order, each with the check that proves it
6. The command for the laptop CLI

Then: what to expect, honestly.

---

## 1. The name: **Ikshana**

*Ikshana* (ईक्षण) is Sanskrit for seeing, looking, and also looking after, caring for.
The second sense is the app: it is built first for people who cannot see well, and it
looks after what it sees by never letting it leave the phone. It rhymes with Kshana, the
family's other Sanskrit name, the instant and the seeing. "Sight" was the English
candidate and "Drishti" the obvious Sanskrit one; the first is a word many apps carry,
and the second belongs on Play to a coaching brand with millions of installs. A search for
"Ikshana" found only the dictionary.

| | |
|---|---|
| App name | Ikshana |
| Package | `com.layerbit.ikshana` |
| Repo | `vysakhsuresh/ikshana-android` |
| Tagline | A camera that answers, and cannot upload what it sees. |
| Icon | The owner's artwork (13 Nov): a round medallion on deep navy, a gold and amber eye held in flowing gold and teal waves (`store-assets/icon/source/ikshana.jpeg`; `store-assets/icon/build_icon.py` fits it to every launcher and store asset). |

Runners-up if Ikshana is ever blocked: **Pratyaksha** (direct perception, nothing in
between; long), **Nokku** (Malayalam, "look"; local).

---

## 2. The five, read off their own pages

### Google Lookout, and Gemini's Guided Vision — free, Google account, cloud
- Lookout: text, documents, labels, currency, images, explore. Available in more than
  twenty languages; the help centre's list includes Bengali, Tamil and Telugu.
- Image descriptions and the questions you can ask about an image are **English only**
  per the help centre; an older Play listing had them in the US, UK and Canada only.
- Guided Vision, launched 1 October 2026 inside Gemini Live, describes the camera's view
  live; Google says it was tested with blind users across India. It runs where Gemini
  Live runs, on Google's servers, signed in.

**Weak where we are strong:** the useful half (describe, ask) is cloud and English; it
needs an account and a signal; every frame of your home goes to Google.

### Microsoft Seeing AI — free, Azure cloud
- Android since December 2023. Short text, documents, products, people, scenes,
  currency, colour, handwriting in some languages; generative descriptions and questions
  about a document.
- Eighteen languages at launch with thirty-six promised for 2024; whether the promise
  landed was not confirmed. No Indian language was named at launch.
- Reporting from the launch describes the app as relying on Azure; one unofficial FAQ
  says short text works offline and the rest needs a connection. Not verified.

**Weak where we are strong:** cloud for everything that matters; no Malayalam, Hindi or
Tamil; the privacy answer is a policy, not a manifest.

### Be My Eyes — free, volunteers and "Be My AI" on OpenAI's servers
- A video call to a sighted volunteer, or a photo described by a cloud model with
  follow-up questions. Free, and generous with it.

**Weak where we are strong:** every photo leaves the phone, to a volunteer or to a
server; nothing works without a signal; a stranger sees your prescription.

### Envision — freemium, cloud
- Reads, describes, finds objects, recognises faces and colours. The free tier is ten
  captures a month per a comparison site; the paid tier is listed by third parties at
  about $34 a year, unconfirmed against Envision's own page. The smart glasses cost more
  than a phone.

**Weak where we are strong:** a counter on the one tool a blind person uses all day;
cloud; priced for Europe.

### RBI MANI — free, offline, one job
- The Reserve Bank's note identifier, January 2020: point the camera at a note and hear
  the denomination in Hindi or English, or feel it as vibration. **Works with no
  internet.** Mahatma Gandhi series only; does not judge a note genuine.

**Weak where we are strong:** it proves the point and stops there. One job, two
languages, last updated 2022 as far as the sources show.

### The gap, in one line
Every tool a blind person in India can install does its real work on a server, in
English, with an account, and the one that does not does one thing. None has a manifest
that proves what it cannot do.

---

## 3. What Ikshana builds

The rule from Squish's competitor doc holds: **do not chase parity, chase the thing they
cannot do.** Four things none of the five can do: everything on the phone with no
INTERNET permission; Malayalam, Hindi and Tamil as first languages, not translations of
an English app; built for the blind first, with the display off, and only then for
everyone; and every rule executed and checked, every model measured against its
reference run.

Everything below runs on the phone. The APK does not declare INTERNET.

### Pillar A — Read (the tool used a hundred times a day)
- Point at anything with words: a bus board, a medicine strip, a notice, a menu, a
  shop sign, a handwritten chit, a page. Text found, ordered as a person would read it,
  and spoken at once in the language the user chose.
- **Indic scripts as a first-class thing:** Malayalam, Hindi (Devanagari), Tamil,
  Kannada, Telugu and English in the first release, the rest of the 22 by script as the
  recogniser grows. Detector and recogniser are ours, trained on synthetic text drawn
  with the fonts we bundle, over the open scene-text sets.
- **Fields pulled out and spoken as fields:** expiry date ("EXP 03/27", "Best before six
  months from manufacture", "MFD"), dose, amount, phone number, PIN code, bus number,
  platform. Pure rules with suites, because these are where a wrong answer hurts.
- Live mode: hold the phone and sweep; it speaks a line as soon as it is sure and never
  repeats a line it has spoken. Snapshot mode: one frame, the whole page, read in order,
  with a pause and resume by a tap anywhere.
- A frame gate in front of every model: blur, exposure, motion, tilt. A smeared frame is
  never sent to a model, and the gate's answer is spoken ("too dark", "hold still",
  "move back") so the person can fix it. The gate is pure arithmetic with a suite.
- Spoken through the Indic-TTS voices Squish converted (Malayalam, Hindi, and the rest
  as the voices release grows), the phone's own TTS as the fallback, numbers and dates
  normalised per language by rules with suites.

### Pillar B — Money and things (beats MANI, matches Seeing AI, offline)
- **Currency:** every denomination of the Mahatma Gandhi and the New series, both
  sides, folded, in bad light, in the hand; spoken and buzzed (one pulse per hundred,
  MANI's own convention, so the habit carries). A small CNN of ours trained on a few
  thousand photos we take with the moto. Genuine or not is never claimed.
- **Describe:** what is in front of me, how many people, is there a step, is the road
  clear, is the gas knob off, which cable is this. A vision-language model of about 500M
  parameters under Apache-2.0, fine-tuned by us on Indian scenes and on the questions
  blind testers actually ask, converted to run on the phone and checked against the
  reference run on an input the conversion never saw.
- **Colour** of a thing under the finger, named in the user's language (Still's
  `ColourName` table); **light** level, for a person who cannot tell if the lamp is on.
- Labels and packets: product name, expiry, MRP, weight, read as fields where the
  rules find them and as plain text where they do not.

### Pillar C — Find (the hand on the door)
- "Where is the door", "which is the blue bottle", "find my cup": the camera swept
  slowly, the phone buzzing stronger and the tone rising as the thing enters the frame
  and centres. A small detector or the vision model's own grounding; the guidance from
  box position to haptic pattern is pure arithmetic with a suite.
- Throttled on purpose: a continuous camera plus a model is heat; the rate drops as the
  phone warms and the user is told.

### Pillar D — Translate (the sign in your language)
- Any text Read finds, translated on the phone between English and the Indic languages
  with IndicTrans2's distilled models (MIT), quantised to fit. Spoken in the target
  language. The first use for a Malayali is a Kannada board in Bengaluru.

### Pillar E — Ask (last, because it is the feature everyone else has)
- Free-form questions about the frame, answered by the same vision model. Honest limit
  written into the UI: a 500M model describes well and reasons poorly, so the answer
  always says what it saw before it says what it thinks.

### Pillar F — Trust, shell and finish (the family's edge)
- **It never goes silent.** The owner's rule, written here as the first law of the app: there
  is nothing Ikshana stops responding to. Every mode has a fallback below it, and the
  fallback below the last one is a spoken sentence that says what was tried and what to do
  next. The vision model fails to load: Read and Money still work. OCR finds nothing: the
  app says so and says "move closer" or "more light", never nothing. The camera is denied:
  the app speaks how to allow it and offers the gallery. The phone is too small for a model:
  the smaller model in the pack takes over and says it is the lighter one. A crash is caught,
  spoken ("Ikshana restarted; say it again"), and the mode comes back where it was. A blind
  person holding a phone that has gone quiet is the one outcome this app is built to make
  impossible, and `docs/FALLBACKS.md` lists every failure and what is spoken for it.
- **Size is not a constraint; capability offline is.** The owner's decision: the app carries
  the best models that run on the phone, not the smallest that fit a download. The base pack
  is what Play's asset-pack limits allow, the GitHub "full" APK carries everything, and the
  listing says the size plainly and why. A larger OCR for every one of the 22 scheduled
  scripts, a larger vision model where the phone can run it, and the smaller twins beside them
  for phones that cannot, chosen at first run by measurement.
- No account, no counter, no subscription, no ads, **no INTERNET permission**;
  `tools/verify_manifest.py` fails CI if it ever appears. Camera is the one permission
  the app asks for; no location, no contacts, no storage.
- Nothing is saved unless the user asks: a Read can be kept as text, a Describe as a
  note; the frame itself is never written to disk by default.
- Models arrive by Play asset pack (and inside the GitHub "full" APK), never by a
  download the app performs. Every model's licence, size and source in
  `THIRD_PARTY_NOTICES.md` the day it is added.
- **The display-off gate:** every screen is driven with TalkBack and the screen dark.
  Zero NAF nodes in `uiautomator dump` is the floor, not the ceiling.
- A low-vision mode for the sighted half of the audience: the family's ink with huge
  type, the one accent at full contrast, and the camera's frame drawn with a thick edge.

### Pillar G — what "in-house" means here, tier by tier
The listing should say which is which, and `docs/MODELS.md` keeps it current.

| Tier | What | How it is ours |
|---|---|---|
| Fully ours | The frame gate; the currency CNN; the field rules (expiry, dose, amount, phone, PIN, bus and platform); reading order; script identification; TTS text normalisation; the Find guidance | Pure Kotlin in `:engine` with suites seen failing, plus one small CNN on photos we took |
| Trained by us on open data | OCR: a text detector of about 15M parameters and a recogniser of about 10M per script | Synthetic lines from Wikipedia text in the bundled fonts, warped, blurred, lit badly, over the real sets (IndicSTR12, Bharat Scene Text, ICDAR MLT), each licence pinned before training |
| Fine-tuned from open weights | Translation (IndicTrans2 distilled, MIT); the vision-language model (SmolVLM2-class, Apache-2.0) | We own the fine-tune, the evaluation set, and the conversion checked against the reference run |

Pretraining a vision-language model is a lab-scale job and the README says so plainly.
OCR is different: every OCR in the world is trained on synthetic text, the models are
small, and that one is fully ours. That is why Read ships first.

---

## 4. The design language

For this app the design is mostly **sound and touch**; the screen is for the sighted
half and for the tester watching over a blind user's shoulder.

- **Voice first.** Every result is spoken, short, in the user's language, with the
  certain part first ("Five hundred rupees" before "probably, the note is folded").
  Nothing is spoken twice. A tap anywhere pauses, a second tap resumes, a swipe down
  repeats, a swipe up reads the next thing. The gestures are the same on every screen.
- **Earcons and haptics.** One short rising tone when the frame gate opens, one falling
  tone when it closes; a pulse per hundred rupees; a tone that rises as Find centres.
  Every earcon has a haptic twin for the deaf-blind, and Settings can turn either off.
- **The home screen is the camera**, in the mode last used. The modes are a row of six
  large buttons along the bottom, each named, each with a state, reachable by TalkBack
  in one swipe each: Read, Money, Describe, Find, Translate, Ask.
- **Type.** Space Grotesk for every word, Fira Code variable for every number, as the
  family does; but the base body size is 18 sp here, not 15, and the low-vision mode
  goes to 28.
- **Colour.** The family base: ink (`#070A12`, surface `#121828`, text `#F6F7FB`) and
  paper (`#F5F4EF`). One accent per app on the primary action alone: Sheaf magenta,
  Still amber, Squish orange, Stave lime; **Ikshana's is Iris, `#6FD3FF` on ink and
  `#0A6FA8` on paper**, spent on the active mode, the shutter and the camera frame's
  edge. The frame gate's state is the semantic trio (`#4CCB8F` open, `#F0A93D` almost,
  `#E8705F` closed). Nothing else is coloured.
- **Shape and motion** as the family: panels 18 dp, buttons 14 dp, press scale 0.975 on
  a medium spring, nothing moving that was not asked to move. Targets are 64 dp here,
  not 48, because the finger is the eye.
- **Words.** Plain, in the user's language, with the engineering word nowhere on the
  first level. "Too dark", "hold still", "move back", "five hundred rupees", "a door on
  the left".
- **Accessibility is the product.** The display-off gate from Pillar F is run before
  every release by a blind tester, not by us.

---

## 5. The task audit — in build order

Each phase ends with a release tag. Each task names the check that proves it, because a
check nobody has seen fail is a check nobody has tested (Squish's CLAUDE.md). The phone is
the moto g84 and, from P2 on, a 4 GB phone beside it.

### P0 — Foundations (week 1)
1. Repo: Gradle, AGP and Kotlin as Stave has them; modules `:app` (Compose, CameraX, the
   doors), `:engine` (pure Kotlin, no `android.*`), native under `app/src/main/cpp`
   (LiteRT, and the C++ inference loop if the VLM needs one). GPL-3.0 by the family's
   choice; `THIRD_PARTY_NOTICES.md` from day one.
2. Manifest strips INTERNET and ACCESS_NETWORK_STATE, declares CAMERA and nothing else;
   `tools/verify_manifest.py` copied from Stave with CAMERA moved from forbidden to
   required and location, contacts, storage and microphone forbidden.
3. Theme and components from Stave with the sizes above; the six-mode row; the earcon
   and haptic players with a Settings switch each.
4. `CLAUDE.md` for the next stranger: where you are decides what "verified" means; the
   checker list; the conventions carried from Squish and Stave; the display-off gate.
5. Tools copied and adapted from Stave: the five `check_*.py`, `tools/jvm/run.sh`,
   `Registry.kt`, `RunnerChecks.kt`, `jc.sh`, `run_desktop.sh`.
6. CI: `android.yml` (engine suites, checkers, `assembleDebug`, `lintDebug` zero errors,
   manifest), `native.yml` (LiteRT and the C++ loop for four ABIs), `release.yml`.
7. Docs: `README.md` in the family's voice, `ARCHITECTURE.md`, `docs/COMPETITORS.md`
   (section 2, dated), `docs/ROADMAP.md` (this section as checkboxes with the three
   marks), `docs/DEVICE_FINDINGS.md`, `docs/PENDING_TESTS.md`, `docs/MODELS.md`
   (Pillar G's table, kept current).
   **Check:** CI green on an empty app that opens on the camera, speaks "Ikshana" once,
   shows the six modes to TalkBack with names and states, and declares only CAMERA in
   the merged manifest report.

### P1 — The test set and the harness, before any model (weeks 2–3)
8. **The Ikshana set:** five hundred photos on the moto, by the owner and the testers:
   every note both sides and folded, medicine strips, bus boards, menus, shop signs,
   notices, handwritten chits, pages; Malayalam, English, Hindi, Tamil, Kannada; a
   third of them blurred, dark or tilted on purpose. The truth typed by hand beside each,
   with the fields (expiry, dose, amount) marked where present. Fifty scenes with a
   one-line human description and the three questions a blind tester asked of each.
   Stored as a GitHub release asset (`eval-v1`) with the index in the repo.
9. The harness in `tools/jvm`: character error rate per script for Read, exact match
   per field, top-1 for currency, a rubric score for Describe read by a person, and
   latency and peak memory per model on each phone. Every model that ever runs in the
   app, ours or borrowed, is scored by it, and the scores live in `docs/MODELS.md`.
   **Check:** the harness scores a deliberately wrong transcript at the error rate it
   was given, and a perfect one at zero; both seen.
10. The frame gate in `:engine`: blur (Laplacian variance), exposure (histogram), motion
    (frame difference), tilt (the phone's own gravity vector); thresholds learned off
    the set's bad third. **Check:** `FrameGateChecks` on synthetic frames, negative-tested
    with the thresholds moved.

### P2 — Read, first release (weeks 4–11, v0.1)
11. The camera frame path: CameraX, YUV straight into the model's input with no copy on
    the UI thread, a frame-skip that keeps TalkBack responsive, the gate in front.
    **Check:** on the moto, 30 fps preview with the gate running and the UI thread under
    4 ms a frame (`dumpsys gfxinfo`).
12. The synthetic text pipeline under `tools/models/ocr/`: Wikipedia lines per language
    rendered in the bundled fonts (Manjari, Chilanka, Gayathri, Noto Sans for each
    script), warped, blurred, lit, with backgrounds from the open sets; the real sets
    mixed in, each licence pinned in `THIRD_PARTY_NOTICES.md` before a line is read.
13. The detector (DBNet-class, about 15M) and the recogniser (CTC, about 10M, one head
    per script) trained, exported to LiteRT, the conversion checked against the
    reference run on a held-out image. **Check:** on the Ikshana set, character error
    rate per script written into `docs/MODELS.md`; the gate is a number, not a feeling:
    Malayalam under 8% on the clean two thirds before v0.1 ships.
14. Reading order and script identification in `:engine` (`ReadingOrderChecks`,
    `ScriptChecks`), negative-tested with columns swapped.
15. The field rules in `:engine`: expiry in every Indian form, dose, MRP, amount, phone,
    PIN code, bus number, platform. **Check:** `FieldChecks` over the set's marked
    fields; each rule seen failing on its own fault.
16. TTS: Squish's converted Indic voices through the family's `voices` release pattern,
    the system TTS as fallback; number, date and unit normalisation per language
    (`SpeakChecks`, executed under `ml-IN`, `hi-IN`, `ta-IN`).
17. The Read screen: live and snapshot, the gestures of section 4, the earcons, the
    "keep this text" door. **Release gate:** a blind tester reads a medicine strip, a
    bus board and a handwritten chit with the display off, on the moto and on the 4 GB
    phone; the fields are spoken as fields; nothing is spoken twice; `PENDING_TESTS.md`
    is honest about what was not seen.

### P3 — Money and Describe (weeks 12–18, v0.2)
18. The currency set: a few thousand photos of every note, both sides, folded, in the
    hand, in bad light, taken over a week with the moto; the CNN trained and exported.
    **Check:** top-1 on the held-out notes above 99% and every miss listed; a note held
    upside down still read.
19. The vision-language model: the Apache-2.0 base chosen by the harness (quality on the
    fifty scenes against latency on the 4 GB phone), fine-tuned on the scenes and the
    testers' questions, converted (LiteRT if the graph converts, llama.cpp's loop under
    MIT if it does not, decided by measurement and written down), checked against the
    reference run. **Check:** rubric score per scene in `docs/MODELS.md`; an answer in
    under five seconds on the moto; peak memory measured and the swap between the OCR
    and the vision model designed for the 4 GB phone, not discovered on it.
20. Colour and light. **Check:** `ColourChecks` from Still's table, executed.
21. The Money and Describe screens, the pulse-per-hundred haptic, the "what I saw / what
    I think" split in every spoken description. **Release gate:** a blind tester counts
    a mixed bundle of notes with the display off and gets the total right; describes a
    kitchen and a street; the phone does not exceed the thermal throttle in ten minutes.

### P4 — Find (weeks 19–22, v0.3)
22. Localisation (the vision model's grounding or a small detector, by the harness), the
    guidance arithmetic in `:engine` (`GuidanceChecks`: box position to haptic strength
    and tone, negative-tested with the axes swapped), the throttle.
    **Release gate:** a blind tester finds a door, a cup on a table and a named bottle
    among three, with the display off, in under twenty seconds each.

### P5 — Translate (weeks 23–25, v0.4)
23. IndicTrans2 distilled, quantised, converted and checked against the reference
    translation of a held-out sentence set; the Translate screen over Read.
    **Check:** BLEU against the reference run within a stated margin in
    `docs/MODELS.md`; a Kannada board read and spoken in Malayalam on the moto.

### P6 — Ask (weeks 26–28, v0.5)
24. Free-form questions over the vision model with the honest split; questions by
    voice through the phone's own speech input (the app does no speech recognition of
    its own). **Release gate:** ten questions from the testers' list answered with the
    seen part right every time and the thought part marked as thought.

### P7 — Polish and release (weeks 29–32, v1.0)
25. The display-off sweep by two blind testers over every screen; the locale sweep
    (Arabic digits, Malayalam numerals, font scale 200%); low-vision mode.
26. Performance: UI thread under 4 ms a frame with every model running; 0 frames drawn
    at rest; battery and heat over a 30-minute Read session written down.
27. Store assets, screenshots in both themes and in low-vision mode, F-Droid metadata,
    reproducible release, GitHub Release with APK, AAB, the "full" APK with models,
    checksums, `THIRD_PARTY_NOTICES.md`, and a listing that says which models are ours
    and which are fine-tuned, in those words.
28. `docs/DEVICE_FINDINGS.md` filled; `docs/PENDING_TESTS.md` emptied or honest.

---

## 6. The command for the laptop CLI

**The current command is `docs/LAPTOP_COMMAND.txt`**, which carries the owner's three
rules (dependable enough to be honoured, never silent, size is no constraint) and the
laptop's paths. The text below is the version this brief was written with; where the two
differ, the file wins.

Paste everything inside the fence into Claude Code on the laptop, from an empty folder,
after checking that `stave-android`, `squish-android`, `layerbit-image-editor-android`,
`sheaf-android` and `deja-android` are cloned beside it (the command clones them if not).

It is written to be resumed: a later session given the same text starts at the first
unchecked item in `docs/ROADMAP.md`.

```text
You are building Ikshana, an open-source Android app that points a camera at the world
and answers, on the phone, for people who cannot see well first and everyone second,
in the repository git@github.com:vysakhsuresh/ikshana-android.git. Read every word of
this before touching a file.

== WHO YOU ARE WORKING FOR ==
The owner builds a family of Android apps that do the whole job on the phone and
upload nothing: Squish (video, vysakhsuresh/squish-android), Still (pictures,
vysakhsuresh/layerbit-image-editor-android), Sheaf (PDF, vysakhsuresh/sheaf-android),
Stave (audio, vysakhsuresh/stave-android), Deja (screenshots, vysakhsuresh/deja-android),
LayerLink (screen share) and Kshana (astrology). Ikshana is the camera that answers.
Every tool is free, forever, with no account, no counter, no ad and no subscription.
The reason to believe that is the repository: the app does not declare INTERNET.

== THE MISSION ==
Beat Google Lookout and Guided Vision, Microsoft Seeing AI, Be My Eyes and Envision on
the phone, offline, in Malayalam, Hindi and Tamil before English, built for the blind
with the display off. The brief is docs/IKSHANA_BRIEF.md in this repository; read it
whole before planning. Its section 5 is the work order and docs/ROADMAP.md is its
checklist.

== SETUP, ONCE ==
1. git clone the repository and cd into it. All work lands on main and is pushed; no
   long-lived branches.
2. Beside it, make sure these exist (clone if not): ../stave-android, ../squish-android,
   ../layerbit-image-editor-android, ../sheaf-android, ../deja-android. They are
   REFERENCE and SOURCE OF COPIES. Never edit them from this session. Never depend on
   them at build time: copy what you need into this repo with a comment saying where it
   came from, and keep the file's own "why" comments.
3. Read, in this order, before writing code: ../stave-android/CLAUDE.md (every section;
   the "never write verified for what you could not observe" rule, the checker list,
   the conventions, the model-conversion lessons), ../stave-android/docs/STAVE_BRIEF.md
   section 5 (how a task audit is written), ../squish-android/CLAUDE.md (the phone
   rules and the LiteRT lesson), ../layerbit-image-editor-android/README.md and its
   Theme.kt, ../deja-android (how a screenshot is read and indexed on the device),
   ../stave-android/tools/models (how a model is converted and checked against its
   reference run), ../stave-android/models_pack (how an asset pack is wired).
4. Build environment on this laptop, from Stave's CLAUDE.md: JDK 21 at
   $HOME/.jdks/jbr-21.0.11, never Android Studio's bundled JBR 25; ANDROID_HOME at
   C:\Users\vysak\AppData\Local\Android\Sdk; Python 3.12 is "python"; the checkers need
   PYTHONUTF8=1; prefix adb with MSYS_NO_PATHCONV=1; never touch a source file while a
   Gradle build is running; never edit a runner while it runs. The phone is the owner's
   moto g84 (serial ZY32J8HF2S); a second, 4 GB phone is needed from P2 and the owner
   will name it. Before any tap confirm the phone is free and Ikshana is the resumed
   activity. Never delete the owner's media.

== NON-NEGOTIABLES ==
- Package com.layerbit.ikshana. Versions as Stave's build files unless a library forces
  otherwise; write down why if it does.
- AndroidManifest.xml removes INTERNET and ACCESS_NETWORK_STATE with tools:node="remove"
  and declares CAMERA and nothing else. tools/verify_manifest.py (from Stave, with
  CAMERA required and microphone, location, contacts and storage forbidden) fails CI if
  the merged manifest ever changes on these points. Never resolve a merge by deleting
  those lines.
- Licence GPL-3.0. Every third-party piece and every dataset in THIRD_PARTY_NOTICES.md
  the day it is added, with its licence, and every dataset's licence pinned to the page
  it was downloaded from BEFORE a single line of it is used to train anything. Where
  sources disagree on a licence, write both down and ask the owner before training.
- Modules: :engine (pure Kotlin, imports nothing from android.*, every rule and every
  piece of arithmetic that can be separated from the framework lives here and is
  executed on the JVM), :app (Compose, CameraX, the doors, LiteRT), native under
  app/src/main/cpp only for what Kotlin cannot do fast enough, with a Kotlin twin
  executed by a suite wherever the arithmetic can be mirrored (Stave's LiveTune lesson).
- Models arrive by Play asset pack and inside the GitHub "full" APK, never by a download
  the app performs. Every model has a tier in docs/MODELS.md: fully ours, trained by us
  on open data, or fine-tuned from open weights; and a score from the harness.
- A model is never trusted on the phone until its conversion has been checked against
  the reference run on an input the conversion never saw (Stave's rule: no converter
  worked as delivered; one flat input, one flat output, every state explicit). LiteRT
  1.4.0, never TensorFlow Lite 2.16.1.
- The test set and the harness (P1) exist before any model is trained. Nothing is judged
  by eye or by ear; every claim about a model is a number in docs/MODELS.md.
- Design: voice first; the gestures of the brief's section 4 on every screen; earcons
  with haptic twins; 64 dp targets; the family's ink and paper with the Iris accent;
  Space Grotesk and Fira Code; a name and a state on every control. The display-off gate:
  every screen driven with TalkBack and the display dark, by a blind tester before a
  release. Zero NAF nodes in uiautomator dump is the floor.
- Conventions carried from Squish and Stave (write them into CLAUDE.md): comments say
  why not what; String.format(Locale.ROOT) for anything machine-readable and never cut
  a localised string with an ASCII literal; count code points not chars; three letters
  is a Latin rule; selectable not clickable where a tint is the only mark; Compose
  compares unstable parameters by identity so declare stable collections and never
  mutate a list you have passed down; every float into a file goes through putFinite;
  a native handle can be negative; engine tables reach the screen by id through
  text/Labels with a translation per language the app speaks.
- Verification, in Stave's words: never write "verified" for something this session
  could not observe. In the sandbox the ceiling is "the arithmetic is executed on the
  JVM and it compiles structurally". On this laptop ./gradlew :engine:test
  assembleDebug lintDebug settles a build, and only the phone, with a blind tester,
  settles a feature. Every pure rule gets a tools/jvm suite executed with real numbers,
  and every suite is negative-tested on a copy in the scratchpad: put the fault back,
  run the suite, watch it fail, restore. When a mistake escapes, write the check that
  would have caught it before writing the fix.
- Git: commit after every task with a message that says why; push to main when the
  build passes, without asking. No model name in commits, code or docs. Never force-push.

== WHAT TO CARRY ACROSS (copy, attribute, adapt) ==
From ../stave-android: tools/verify_manifest.py, the five tools/check_*.py,
tools/jvm/run.sh, Registry.kt, RunnerChecks.kt, jc.sh, run_desktop.sh, the theme and
the shared components, models_pack wiring, tools/models conversion pattern, the CI
workflows, CLAUDE.md's structure. From ../squish-android: the converted Indic voices
and Tts/NeuralVoice/TtsRules, the voices release pattern, Wer.kt as the shape of a
scorer. From ../layerbit-image-editor-android: ColourName, the engine's histogram and
blur arithmetic for the frame gate, the manifest pattern. From ../deja-android: the
screenshot OCR door and index, as reference for the Read pipeline's shape.

== NEW PIECES (Apache/MIT first) ==
CameraX (Apache-2.0). LiteRT 1.4.0 (Apache-2.0). Our own OCR detector and recogniser
trained under tools/models/ocr with PyTorch and exported. Our own currency CNN under
tools/models/currency. A SmolVLM2-class vision-language model (Apache-2.0) for
Describe, Find and Ask, fine-tuned under tools/models/vlm; if its graph will not convert
to LiteRT, llama.cpp's loop (MIT) built in native.yml, decided by measurement and
written down. IndicTrans2 distilled (MIT) under tools/models/translate. The open
scene-text sets (IndicSTR12, Bharat Scene Text, ICDAR MLT) with licences pinned. Fonts
under OFL for the synthetic text and the UI.

== THE ORDER OF WORK ==
docs/ROADMAP.md is the checklist, written from section 5 of docs/IKSHANA_BRIEF.md with
every task as a checkbox and its check beside it, with Stave's three marks ([x] seen on
a phone, [~] built not seen, [ ] not built). Work from the first unchecked item. Do not
skip ahead: P0 foundations, P1 the test set and harness, P2 Read (v0.1), P3 Money and
Describe (v0.2), P4 Find (v0.3), P5 Translate (v0.4), P6 Ask (v0.5), P7 polish (v1.0).
Each phase ends with its release gate run with a blind tester if one is available and
written into docs/DEVICE_FINDINGS.md with the date; otherwise the gate goes into
docs/PENDING_TESTS.md in order and the item is marked built-not-seen, never done.

Within a task: write the pure rule in :engine first, write its suite, run it, break it
and watch it fail, restore; then the model or UI piece; then the gate; then commit and
push. Keep CLAUDE.md current: what took an hour to find out goes in there in one
paragraph for the next stranger.

== DOCUMENTS TO WRITE IN P0 ==
README.md in the family's voice ("A camera that answers, and cannot upload what it
sees"; why this exists; nothing leaves the phone; built for the blind first; the table
of what it does; which models are ours, in those words). CLAUDE.md. ARCHITECTURE.md
(the modules, the frame path, where every model lives and how it is swapped on a 4 GB
phone). BUILD_NOTES.md (CameraX choices, LiteRT delegates, the asset pack). docs/
COMPETITORS.md (section 2 of the brief, dated, "read, not driven"). docs/ROADMAP.md,
docs/DEVICE_FINDINGS.md, docs/PENDING_TESTS.md, docs/MODELS.md, THIRD_PARTY_NOTICES.md,
LICENSE (GPL-3.0), store-assets/, fastlane/ metadata.

== WHAT DONE LOOKS LIKE FOR THIS SESSION ==
Work until the context runs low. Before you stop: every change committed and pushed to
main; CI green or the failure named in docs/PENDING_TESTS.md; docs/ROADMAP.md ticked
honestly; docs/MODELS.md carrying a number for every model that exists; CLAUDE.md told
what you learned; a final message that says which roadmap items are done, which are
built-not-seen, and what the next session should do first. Do not describe anything as
working on a phone unless you watched it work there, and do not describe anything as
working for a blind user unless one said so. Begin with P0 item 1 unless docs/ROADMAP.md
already exists, in which case begin at its first unchecked item.
```

---

## What to expect, honestly

- Eight months of work for one person with a CLI, run session after session from the
  roadmap's first open box. The first shippable thing, Read in five scripts with the
  fields spoken, is about three months in, and it is the thing used a hundred times a
  day, so it is the right first thing.
- **Read is fully ours and the quality is in our hands.** Synthetic text is how every
  OCR is trained; the open real sets fill the gap; the Ikshana set is the judge. The
  number to beat before v0.1 is written in P2 and it is a number.
- **Describe, Find and Ask lean on a 500M-parameter open model that we fine-tune but did
  not pretrain.** It will describe a kitchen well and reason about it badly, and the UI
  says which half is which every time. Pretraining one is lab-scale work and the README
  says so rather than pretending.
- The asset pack is one to two gigabytes. On a 4 GB phone the OCR and the vision model
  cannot both stay loaded; the swap is designed in P3, measured, and written down.
- A continuous camera plus a model is heat. Find throttles on purpose and tells the
  user; a session that runs cool for thirty minutes is a P7 gate.
- The licences of the datasets conflict across the sites that host them, as the speech
  data did. Each one is pinned to its download page before training and both readings
  are written down where they differ. That is a check, not a legal opinion.
- Three to five blind testers from a users' association in Kerala are needed from P2,
  not after it. Every release gate from v0.1 on is theirs to pass, and nothing in
  `DEVICE_FINDINGS.md` says "works for a blind user" unless one said so.
- The claim that goes on the listing is the one none of the five can copy: your camera,
  your notes, your prescriptions and your home stay on your phone, and the Play Store's
  own permission list is the proof.
