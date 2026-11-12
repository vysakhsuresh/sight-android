# Fallbacks: what Ikshana says when something fails

The owner's second rule: **it never goes silent.** Every mode has a mode below it; the floor of
the floor is Help, which needs nothing and says what to do next. This page lists every failure
the app knows about, where it lands, and the sentence spoken. It is held to the code:

- the chain and the sentences are data in `engine/.../mode/` (`ModeTable`, `Line`, `ModeMachine`);
- `FallbackChecks` walks every state (every model present, absent or failed; every camera state;
  hot and cool; every mode the user can want) through every event, about 4.7 million steps, and
  fails if any step that changes what the user is in, or answers what the user did, ends without
  a sentence (or, when the sentence would be the one just said, the "repeat" earcon);
- `SpeechTableChecks` fails if any line below has no string in `values/strings.xml`, a different
  wording, the wrong number of mode names, or no row on this page.

Last checked against the code: 12 November 2026. **None of this has been heard on a phone yet**
(`docs/PENDING_TESTS.md`).

## The chain

| Mode | Needs | Falls to | Why there |
|---|---|---|---|
| Read | camera; OCR detector and recogniser, or their lighter twins | Light level | Light needs no model |
| Money | camera; the currency model | Read | every note carries its value in numerals |
| Describe | camera; the vision model, or its lighter twin | Read | text is what the small models still find |
| Find | camera; the vision model, or its lighter twin | Describe | same model family, lighter twin first |
| Translate | camera; OCR and the translation model (full or lighter) | Read | reads the text untranslated |
| Ask | camera; the vision model, or its lighter twin | Describe | same model family |
| Light level | camera or a picked picture; no model | Help | pure arithmetic over luma (`LightLevel`) |
| Help | nothing | — | says how to allow the camera, offers the gallery |

A picked picture stands in for the camera for every mode. When the cause clears (the camera
comes back, the user chooses again), the machine climbs back to the mode the user last asked for.

## Every line, by id

Each `%1$s` / `%2$s` is a mode's name. The earcon plays before the sentence; its haptic twin
plays with it.

| Id | When | Earcon | Sentence (English reference) |
|---|---|---|---|
| `say_welcome` | the app opens | — | Ikshana. |
| `say_mode_on` | a mode comes on as asked | mode | %1$s. |
| `say_not_ready` | the asked mode's model is not in this install | fallback | %1$s is not on this phone yet. %2$s is on instead. |
| `say_could_not_start` | the asked mode's model is present but failed to load, ran out of memory, or failed its self-check | fallback | %1$s could not start. %2$s is on instead. |
| `say_lighter` | the full model failed or measured too heavy; the lighter twin runs | fallback | %1$s is on, with the lighter model this phone can carry. |
| `say_no_camera` | camera permission denied (or never granted) and no picture picked: lands in Help, both doors on screen | fallback | Ikshana cannot use the camera. To allow it, open Settings, then Apps, Ikshana, Permissions, Camera. Or pick a picture from the gallery. |
| `say_camera_busy` | another app holds the camera | fallback | Another app is using the camera. Close it, and Ikshana will look again. |
| `say_camera_back` | the camera returns from Help | fallback / mode | The camera is back. %1$s is on. |
| `say_too_dark` | the mode looked and found nothing; the frame was too dark | nothing | Nothing found. It is too dark; more light, please. |
| `say_hold_still` | found nothing; the frame was blurred or moving | nothing | Nothing found. Hold still. |
| `say_move_closer` | found nothing; the thing was too small in the frame | nothing | Nothing found. Move closer. |
| `say_move_back` | found nothing; the thing filled or overflowed the frame | nothing | Nothing found. Move back a little. |
| `say_nothing_here` | found nothing, with no better hint | nothing | Nothing found here. Move the phone slowly. |
| `say_restarted` | the app crashed and was restarted; the mode comes back where it was | — | Ikshana restarted. %1$s is on. Say it again. |
| `say_warm` | the phone crossed the thermal throttle line; the frame rate drops | nothing | The phone is warm. Ikshana is slowing down to cool it. |
| `say_cool` | it came back under | nothing | The phone has cooled. Back to full speed. |
| `say_too_long` | the mode's answer missed its time budget | nothing | %1$s is taking too long. Hold still and try again. |

When a sentence would be exactly the one just spoken, it is not said again (brief, section 4):
the **repeat** earcon (two quick ticks) and its twin answer instead, so the user still hears the
app is alive. A swipe down says the last sentence again on request.

## Failures below the mode machine

These happen in the app's doors, not in the machine, and are listed here so the page is whole.

| Failure | What happens | Status |
|---|---|---|
| The crash itself | `IkshanaApplication` records the mode, relaunches the activity once (not within 15 s of a previous crash, so a crash loop ends in the system's dialog rather than a spin), and the next start says `say_restarted`. | built, not seen |
| No text-to-speech engine, or its language missing | The sentence is shown in large type, the fallback earcon and its twin play, and `no_voice` is shown saying where to install a voice. **This is the one path where the app cannot speak**, because there is no voice to speak with; the neural voices (P2, Squish's converted Indic voices) remove the dependency on the phone's engine. | built, not seen |
| The earcon player cannot open an audio track | The sentence still goes to the voice; the haptic twin still plays. | built, not seen |
| The vibrator is absent | Earcons and speech carry on. | built, not seen |
