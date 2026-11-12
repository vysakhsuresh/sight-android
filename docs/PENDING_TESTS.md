# Pending tests

Built and not seen, in the order to run them when the phone and a tester are free. Each says
what would count as seen. Move an item to `DEVICE_FINDINGS.md` with the date when it is.

Before any tap: `dumpsys telephony.registry | grep mCallState` is 0 and Ikshana is the resumed
activity (`dumpsys activity activities | grep topResumedActivity`). Never sweep in the owner's
own data (Ikshana keeps none yet).

0. **CI's first run on GitHub** (`android.yml`) after the push of 12 Nov. Seen when both jobs
   are green, or the failure is named here.
0a. **Read a real page and a medicine strip** with the interim reader: Malayalam and English
   (default), then each other language from Settings, Reading. Seen when the right words are
   heard in the right voice and no line is said twice while the phone sweeps. Log
   `IkshanaRead` for the time per frame. Then a bus board outdoors.
0b. **Money, interim:** hold a ₹10, ₹20, ₹50, ₹100, ₹200, ₹500 and ₹2000 flat at a hand's length,
   front and back, in room light. Seen when each is said once, correctly, with its buzz, and a
   card with "500" in a price on it is not called a note (it will be: numerals are all it reads;
   write down how often). Log `IkshanaMoney`.
0c. **The new voice and words:** `Ikshana: voice for …` in the log names the voice chosen per
   language; choosing Translate says what Translate will do.
1. **Hear the start and the light level** on the moto: "Ikshana.", the fallback sentence, then
   one light band and **no** "Dark." in a lit room (the 1.5 s settle). Cover the lens: "Dark."
   once; uncover: one band, no flicker between bands while the phone is still.
2. **TalkBack over the home screen, display off** (the P0 check): one swipe per mode, each
   read with its name and its state ("not on this phone yet" for all six in this build); the
   camera surface's actions (Repeat, Next mode, Previous mode) in the actions menu; double-tap
   pauses and resumes. Settings: both switches named with their state; turning Sounds off
   silences the earcons and leaves the vibration; turning Vibration off the reverse.
3. **The crash path.** A debug-only way to throw is not built yet: build one (a broadcast the
   debug build listens for), crash once, and check the app comes back by itself saying
   "Ikshana restarted. Light level is on. Say it again." Then crash twice within 15 s: the second
   must go to the system dialog, not loop. Unknown until seen: whether Android 14 lets the
   relaunch through after the process is killed.
4. **Camera denied.** `pm revoke com.layerbit.ikshana android.permission.CAMERA`, launch, answer
   the dialog "Don't allow": hear the Help sentence; both doors on screen and in TalkBack; "Allow
   the camera" asks again or, after a second refusal, opens the app's Settings page; granting
   there and coming back says "The camera is back. Light level is on." (Light, because Read
   still falls to it in this build).
5. **Camera busy.** Open the system camera app over Ikshana's split screen or start a video call
   app, then return: hear "Another app is using the camera…", then the camera comes back.
6. **Pick a picture** from Help: a dark photo says "Dark.", a bright one "Bright light.".
7. **Gestures with TalkBack off**: tap pauses and resumes mid-sentence, swipe down repeats,
   left and right step through the modes; a slow drag does nothing.
8. **No voice engine**: disable the phone's TTS engine (or pick one with no language data) and
   launch: the banner shows the line and where to install a voice; the fallback earcon and its
   twin play.
9. **Thermal**: hard to force; note the first time `THERMAL_STATUS_MODERATE` is crossed in a
   long session and what was said.
10. **Money** (interim, the borrowed reader): a ₹10, ₹100 and ₹500 note held flat a hand's
    length away, each said once when two of three frames agree (`IkshanaMoney`, `IkshanaSay`),
    and each note's buzz felt different. A folded note that hides its numerals: no value, then
    "Nothing found" paced, never a guess.
11. **Describe** (the 1B vision model pushed by adb to the app's external files
    `models/vision/`, two `.gguf` files only): "Looking." once, then two sentences within a
    stated time (`IkshanaDescribe` gives the milliseconds); the same wall not described again;
    turning to another wall described after 6 s; a hand over the lens says it is too dark.
    Peak memory and warmth over ten minutes noted.
12. **Ask**: entering says the first question and asks it of the next clear frame at once;
    swipe up (or the button, or TalkBack's "Next question") says the next question and asks it
    once (`IkshanaAsk`); a medicine strip under "What is the expiry date?" answered or told
    where to move; no "nothing found" while no question waits.
13. **Find**: "Finding a door." on entering; a door left, ahead and right of the camera said as
    such (`IkshanaFind`), a long pulse within reach and a tick further; a room without one gives
    paced "Nothing found", never a direction; swipe up steps to a chair, a cup and so on.
