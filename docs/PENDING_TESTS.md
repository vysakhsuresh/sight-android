# Pending tests

Built and not seen, in the order to run them when the phone and a tester are free. Each says
what would count as seen. Move an item to `DEVICE_FINDINGS.md` with the date when it is.

Before any tap: `dumpsys telephony.registry | grep mCallState` is 0 and Ikshana is the resumed
activity (`dumpsys activity activities | grep topResumedActivity`). Never sweep in the owner's
own data (Ikshana keeps none yet).

0. **CI's first run on GitHub** (`android.yml`) after the push of 12 Nov. Seen when both jobs
   are green, or the failure is named here.
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
