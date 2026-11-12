# Device findings

What was seen on a phone, dated. "Logged" means the app wrote it to logcat (what the machine
handed to the voice), not that anyone heard it. Nothing here says "works for a blind user": no
blind tester has used the app yet.

## 12 November 2026 — moto g84 (ZY32J8HF2S), Android 14, first install, debug build 0.0.1

Installed with `adb install -r`, CAMERA granted with `pm grant` (so the permission dialog was not
seen), launched with `am start`. No taps were sent by this session.

- **Opens on the camera.** Screenshot: the back camera's preview fills the frame inside the Iris
  edge; the settings button top right; the six modes in two rows of three below.
- **The start, logged:** `Ikshana.` then `Read is not on this phone yet. Light level is on
  instead.` (correct: no model is in this build, so Read falls to Light).
- **Light level, logged:** `Dark.` about three seconds after start, then `Bright light.` a second
  later, in a lit room. The first was false: the camera's first frames come before
  auto-exposure has opened up. Fixed in the same session by not reading frames for 1.5 s after
  the first (`LightLevel.SETTLE_MS`); the fix itself is not yet seen.
- **Accessibility tree** (`uiautomator dump`): zero NAF nodes. The camera surface reads "Camera.
  Light level is on."; the six modes carry their names; Settings has its name. Found: the
  settings glyph "≡" was exposed as text beside the name, which TalkBack would read; cleared.
  The mode states (`stateDescription`) do not appear in a dump and were not heard.
- **Permissions on the phone** (`dumpsys package`): requested CAMERA and VIBRATE, nothing else.
- **Read, interim (later build, 16:06):** the reader opened with Malayalam and English and read
  each upright frame (960 x 1280) in 350-530 ms on the analysis thread. The lens was covered
  (black preview), so no text was found; after 4 s it said "Nothing found. It is too dark; more
  light, please." once, as paced. Choosing Money, Translate or Ask now says "… is not on this
  phone yet. Read is on instead." (logged). Text read from a real page: **not yet seen.**
- **Someone used it.** Between 15:36:09 and 15:36:49 the log shows Describe, Find, Translate, Ask,
  Read, Money, Describe and Ask chosen in turn, each answered by the fallback earcon and its
  sentence (`<mode> is not on this phone yet. Light level is on instead.`). These were not this
  session's taps; the owner (or whoever held the phone) was trying the row. What they heard is
  theirs to say.
