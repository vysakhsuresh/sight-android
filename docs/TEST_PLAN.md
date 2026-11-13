# Test plan: the next phone session (written 13 Nov)

For the moto g84 (ZY32J8HF2S), the build installed at 13:07 on 13 Nov (commit bd1fd61: the
lens icon, the reading-loop fix, Ask and Find wired). About 75 minutes. Two people: **the owner**
holds the phone and listens; **the session** works over adb and reads the log. Every result goes
into `docs/DEVICE_FINDINGS.md` with the date, as one of: *heard*, *felt*, *seen on screen*,
*logged only*, or *failed* (with what happened instead). `PENDING_TESTS` items move there when
seen.

Log tags, read by the session throughout:
`IkshanaSay` (every line handed to the voice), `IkshanaRead`, `IkshanaMoney`,
`IkshanaDescribe`, `IkshanaAsk`, `IkshanaFind` (each with milliseconds per frame), `Ikshana`
(errors, the voice chosen).

## 0. Before anything (session)

1. Ask the Stave session whether it is finished with the phone. Nothing starts until it says so,
   and until the owner says the phone is free (13 Nov 13:05: Stave done; owner in Photos and
   WhatsApp).
2. `dumpsys telephony.registry | grep mCallState` is 0 (not on a call).
3. Note battery %, temperature (`dumpsys battery`), and free memory (`dumpsys meminfo | head`).
4. Follow the log from now: `logcat -T 1 -v time` filtered to the tags above. The shared log
   buffer is never cleared, because the Stave session's logs are in it too.

## 1. The icon (owner, 2 min)

| Step | Pass |
|---|---|
| Look at the home screen and the app drawer | The medallion: a gold and amber eye in flowing gold and teal waves on deep navy, filling the round icon with no navy ring or cut-off edge |
| If themed icons are on (wallpaper and style, then themed icons) | The one-colour mark: an open eye, its two lids and a solid iris with the pupil cut out |
| Notification shade while Ikshana runs, if a notification shows | The same small mark |

## 2. Start (owner, 2 min)

| Step | Pass |
|---|---|
| Open Ikshana from its icon | "Ikshana." heard once, then the last mode's name |
| Session: `dumpsys activity activities` | Ikshana is the resumed activity; from here on, every tap the session asks for is checked against this |

## 3. Read and the loop the owner found (owner, 15 min). The most important section.

Use a medicine strip, then a printed page, then a sign with a number in it (a bus or platform
number, a room number).

| # | Step | Pass | Fail looks like |
|---|---|---|---|
| 3.1 | Read on. Hold the strip still, a hand's length away, for 30 s | Each line said **once**, then quiet: no "nothing found" while the strip is still in view (text is there, just already said) | The same line again and again |
| 3.2 | Move the strip away for 5 s, then back | Not read again (it is remembered) | Read from the top again |
| 3.3 | Turn the strip over / show a new strip | The new text is read | Silence |
| 3.4 | Hold a full printed page so that many lines are read, and while it is still speaking, swipe left (or tap Money) | Speech stops **within about a second**; "Money." is heard; nothing from the page after that | The page goes on being read |
| 3.5 | Back in Read, with a long text being read: tap once | Speech stops; "Paused" shows | Goes on |
| 3.6 | Tap again | Says the cut line from its start, then goes on | Skips it, or repeats everything |
| 3.7 | Swipe down | The last line again | Nothing |
| 3.8 | Two signs that differ only in a number (write "Platform 4" and "Platform 5" on two papers) | Both are read | The second is skipped as "the same" |
| 3.9 | Point at a blank wall for 15 s | "Nothing found here. Move the phone slowly." about 4 s in, then no more than every 10 s | Said every second, or never |

Session: for 3.1 to 3.4, count the `IkshanaRead` lines between `IkshanaSay` lines. Reading should
pause while the voice speaks: no `IkshanaRead` lines between one spoken line and the next.

## 4. Money (owner, 8 min)

| # | Step | Pass |
|---|---|---|
| 4.1 | Choose Money and point at nothing | No "nothing found" before about 4 s (it used to come 2 s in) |
| 4.2 | Hold a ₹10, ₹100 and ₹500 note flat, a hand's length away, one at a time | Each value said **once**, and each note's buzz pattern felt different |
| 4.3 | Hold a note folded so the numerals hide | No value; "nothing found" at its pace; never a wrong value |
| 4.4 | Hold the same note again after another one | Said again (a new note in hand) |

## 5. The vision model onto the phone (session, about 5 to 10 min over wireless adb)

1. Close Ikshana from recents (owner) or `am force-stop com.layerbit.ikshana` (session).
2. `adb push` the two files of the chosen 1B vision model (about 675 MB and 333 MB) into
   `/sdcard/Android/data/com.layerbit.ikshana/files/models/vision/`, nothing else in that
   folder. This is Ikshana's own folder; nothing of the owner's is touched.
3. Check both sizes on the phone match the laptop's to the byte.
4. Reopen Ikshana (owner). The machine now counts Describe, Ask and Find as ready.

## 6. Describe (owner, 15 min)

| # | Step | Pass | Session reads |
|---|---|---|---|
| 6.1 | Choose Describe, point at a table with a few things | "Describe." then "Looking." once, then at most two short sentences that are **true** | First load time (model open) and ms per description in `IkshanaDescribe` |
| 6.2 | Hold still on the same scene for 20 s | Not described again | No new `IkshanaDescribe` lines |
| 6.3 | Turn to another wall or a doorway | Described again (not sooner than 6 s after the last) | |
| 6.4 | Cover the lens with a hand | "Nothing found. It is too dark; more light, please." | |
| 6.5 | Shake the phone while it looks | Not described while shaking; "hold still" or nothing | |
| 6.6 | A person in view (owner's consent) | Said as a person, with **no** guess at identity, age or mood | |
| 6.7 | Switch to Read while Describe is still thinking | The late description is **not** spoken after the switch | |
| 6.8 | Leave Describe on for 10 minutes, pointing around a room | Keeps working; phone warm but no "warm" slow-down line, or if there is one, note the time | `dumpsys meminfo com.layerbit.ikshana` peak; `dumpsys battery` temperature at start and end |

**Truth check for 6.1 and 6.3:** the owner says, for each description, *right*, *partly right*
or *wrong* (and what was invented). Two wrong things said with confidence in ten descriptions is a
fail for this model on this phone.

## 7. Ask (owner, 10 min)

| # | Step | Pass |
|---|---|---|
| 7.1 | Choose Ask while pointing at an object | "Ask." then "What is this? Hold the phone towards it. Swipe up for another question." then an answer **once** |
| 7.2 | Wait 15 s | No second answer, and no "nothing found" chatter |
| 7.3 | Swipe up | "What colour is this?" and then an answer once |
| 7.4 | Tap the question button at the top of the screen instead of swiping | Steps the same way |
| 7.5 | Medicine strip, "What is the expiry date?" | The date, or a clear "turn it over / move closer" |
| 7.6 | Phone or laptop screen, "What does the screen say?" | The main text on it |
| 7.7 | Light on and off, "Is a light on?" | Right both times |
| 7.8 | Six questions all the way round | After "How do I take this medicine?" it wraps to "What is this?" |

The session times every answer from `IkshanaAsk`.

## 8. Find (owner, 10 min)

| # | Step | Pass |
|---|---|---|
| 8.1 | Choose Find | "Find." then "Finding a door. Turn slowly. Swipe up to find something else." |
| 8.2 | Stand with a door to your left, then ahead, then right | "To your left.", "Straight ahead.", "To your right.": each correct, with a short tick |
| 8.3 | Walk up close to the door | "... within reach." with a long pulse |
| 8.4 | Face a wall with no door | Paced "Nothing found", and **never** a direction |
| 8.5 | Swipe up to "a cup"; put a cup left, then right, on a table | Correct sides |
| 8.6 | Swipe up through all eight | Door, chair, table, cup, bottle, bag, phone, person, then door again |

**Safety rule for 8.2 to 8.5:** one wrong direction counts as a fail. Note every one,
with the photo position, because a wrong direction moves a blind user's hand or feet.

## 9. TalkBack (owner, 8 min, if time allows)

| # | Step | Pass |
|---|---|---|
| 9.1 | Turn TalkBack on; swipe through the screen | Each of the six modes says its name and On / Off / Not on this phone yet |
| 9.2 | On the camera area, open TalkBack's actions menu | Repeat, Next mode, Previous mode; in Ask also "Next question", in Find "Find something else" |
| 9.3 | In Ask, the question button | Read aloud as the question; double-tap steps it |

## 10. After (session)

1. Message the Stave session that the phone is free.
2. Write every result into `DEVICE_FINDINGS.md`; move what was seen out of `PENDING_TESTS.md`;
   mark the roadmap (`[x]` only for what was seen).
3. Put latency, memory and temperature for the 1B model into `docs/MODELS.md`. If Describe takes
   over 10 s or makes things up, the next step is the smaller model for speed or a stricter prompt,
   decided by those numbers.
4. Fix every failure found, each with a check written first.
