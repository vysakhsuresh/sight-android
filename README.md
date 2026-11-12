# Ikshana

**A camera that answers, and cannot upload what it sees.**

*Ikshana* (ഈക്ഷണ, ईक्षण) is Sanskrit for seeing, and for looking after. It is an Android app
that points the camera at the world and answers out loud, on the phone, for people who cannot
see well first and everyone second. Part of the Layerbit family (Squish, Still, Sheaf, Stave,
Deja, LayerLink): every tool free, forever, with no account, no counter, no ad and no
subscription.

## Why this exists

Every tool a blind person in India can install today does its real work on a server, in English,
with an account; the one that does not (RBI's MANI) does one thing. Ikshana does the whole job on
the phone, in Malayalam, Hindi and Tamil before English, built to be used with the display off.

## Nothing leaves the phone

The app does not declare the INTERNET permission. It cannot open a socket, so nothing the camera
sees (your medicines, your money, your home) can leave the phone, whatever any library inside it
might try. You do not have to trust a policy: the Play Store's permission list shows the camera
and nothing else, and `tools/verify_manifest.py` fails the build if that ever changes.

## It never goes silent

Every mode has a mode below it, and the last one is always a spoken sentence saying what was
tried and what to do next. A missing model, a refused camera, a crash, a hot phone: each has its
sentence (`docs/FALLBACKS.md`), and a suite walks every state of the app's mode machine, 4.7
million of them, to prove none ends in silence.

## What it does

| Mode | What it answers | Status |
|---|---|---|
| Read | text in front of the camera, spoken in order, with dates, doses, amounts and numbers spoken as fields | being built (P2) |
| Money | every Indian note, both sides, folded, spoken and buzzed (a pulse per hundred) | planned (P3) |
| Describe | what is in front of me; what I saw first, then what I think | planned (P3) |
| Find | "where is the door": the phone buzzes stronger as it centres | planned (P4) |
| Translate | a sign in another Indian language, spoken in yours | planned (P5) |
| Ask | a question about what the camera sees | planned (P6) |
| Light level | dark, dim, bright or glare, for anyone who cannot tell if the lamp is on | **in the app now** |

Until a mode's model is in the app, choosing it says so and Light level answers instead.

## Which models are ours

Said plainly, as the listing will say it (`docs/MODELS.md` keeps the numbers):

- **Fully ours:** the frame gate, the field rules, reading order, the currency model, the Find
  guidance, light level. Rules in pure Kotlin with suites, and one small network trained on photos
  we take.
- **Trained by us on open data:** the text detector and recogniser for every script.
- **Fine-tuned from open weights:** the vision-language model (Describe, Find, Ask) and the
  translation model. We own the fine-tune and the evaluation; we did not pretrain them, because
  pretraining a vision model is a laboratory's job, not an app's.

## The size, and why

Capability offline matters more than download size. The app will carry the best models that run
on a phone: the Play build as large as asset packs allow, the GitHub "full" APK with everything,
and lighter twins chosen at first run for phones that cannot carry the large ones. The listing
will state the size. Today's build is small because it has no models yet.

## Building

JDK 21 to run Gradle, Android SDK 36. `CLAUDE.md` has the laptop's exact setup.

```
.\gradlew :engine:test assembleDebug lintDebug
python tools/verify_manifest.py
```

`sh tools/jvm/run.sh` runs every engine suite on any machine with a JDK and no Android SDK.

GPL-3.0. Third-party pieces and their licences: `THIRD_PARTY_NOTICES.md`.
