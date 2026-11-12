#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the blue eye with HUD arcs (concept G2).

The eye is generated, not traced: 320 iris fibres in two layers, a collarette round the
pupil, a dark limbal rim, a glass gloss, two catches of light, a cyan aura, and two pairs
of arcs around it. The same geometry, drawn once here, becomes:

  app/src/main/res/mipmap-*/ic_launcher_foreground.png   adaptive foreground (the art,
                                                          scaled into the 66 dp safe zone,
                                                          transparent around it)
  app/src/main/res/mipmap-*/ic_launcher_background.png   adaptive background (the field)
  app/src/main/res/mipmap-*/ic_launcher.png, _round.png  legacy rasters, 48 dp
  app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml    the adaptive icon declarations
  app/src/main/res/drawable/ic_launcher_monochrome.xml   Android 13+ themed icon (vector)
  app/src/main/res/drawable/ic_notification.xml          status-bar glyph (vector)
  store-assets/icon/ikshana-icon.svg                     master artwork, 108 dp canvas
  store-assets/icon_512.png                              Play Store icon
  store-assets/feature_graphic_1024x500.png              Play Store feature graphic

Run from the repo root:  python3 store-assets/icon/build_icon.py
The SVG and XML need only Python. The PNGs need Node with Playwright and a Chromium
(render.js beside this file does the screenshots).

Why rasters for the adaptive layers and not a vector: a VectorDrawable with 320 gradient
strokes is slow to inflate on every launcher redraw, and the fibres are the icon. The
themed icon and the notification glyph are vectors because they must be, and they carry a
simplified mark (rim, pupil, arcs) that reads at 24 dp.
"""
import json, math, os, random, subprocess, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# The art is designed on a 108 canvas with the outer arcs at radius 44. Under a launcher
# mask only a 66 dp circle (radius 33) is guaranteed visible, so the adaptive foreground
# scales the art by SAFE about the centre. The Play icon and the legacy rasters, which the
# store and the launcher crop themselves, use the art at full size.
SAFE = 33 / 46

BLUES = ["#E6F7FF", "#9FDBFF", "#4FB8FF", "#1E7FE0", "#0B4FA8", "#C9ECFF"]
DEEP = ["#083A7A", "#0B57B8", "#1E7FE0", "#062A5C"]
R = 27  # iris radius


def fibres(n, r0, r1, cols, seed, wmin=.4, wmax=.9, jitter=.012, omin=.3, omax=.9):
    random.seed(seed)
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n + random.uniform(-jitter, jitter)
        ra = r0 + random.uniform(0, 2.0)
        rb = r1 - random.uniform(0, 4.0)
        w = random.uniform(wmin, wmax)
        op = random.uniform(omin, omax)
        c = random.choice(cols)
        out.append(f'<line x1="{54 + ra * math.cos(a):.2f}" y1="{54 + ra * math.sin(a):.2f}" '
                   f'x2="{54 + rb * math.cos(a):.2f}" y2="{54 + rb * math.sin(a):.2f}" '
                   f'stroke="{c}" stroke-width="{w:.2f}" opacity="{op:.2f}" stroke-linecap="round"/>')
    return "".join(out)


def collarette(r, amp, n, col, w, op):
    pts = []
    for i in range(n * 2 + 1):
        a = 2 * math.pi * i / (n * 2)
        rr = r + (amp if i % 2 == 0 else -amp)
        pts.append(f"{54 + rr * math.cos(a):.2f},{54 + rr * math.sin(a):.2f}")
    return (f'<polygon points="{" ".join(pts)}" fill="none" stroke="{col}" stroke-width="{w}" '
            f'opacity="{op}" stroke-linejoin="round"/>')


def arc_d(r, a0, a1):
    x0, y0 = 54 + r * math.cos(a0), 54 + r * math.sin(a0)
    x1, y1 = 54 + r * math.cos(a1), 54 + r * math.sin(a1)
    large = 1 if (a1 - a0) > math.pi else 0
    return f"M{x0:.2f},{y0:.2f} A{r},{r} 0 {large} 1 {x1:.2f},{y1:.2f}"


def arc(r, a0, a1, col, w, op):
    return (f'<path d="{arc_d(r, math.radians(a0), math.radians(a1))}" fill="none" stroke="{col}" '
            f'stroke-width="{w}" opacity="{op}" stroke-linecap="round"/>')


DEFS = f'''<defs>
<radialGradient id="bg" cx="50%" cy="50%" r="72%"><stop offset="0" stop-color="#0A1830"/><stop offset=".6" stop-color="#050B18"/><stop offset="1" stop-color="#02050C"/></radialGradient>
<radialGradient id="iris" cx="40%" cy="36%" r="70%"><stop offset="0" stop-color="#7FD6FF"/><stop offset=".38" stop-color="#1F86E8"/><stop offset=".72" stop-color="#0A3F96"/><stop offset="1" stop-color="#041C4A"/></radialGradient>
<radialGradient id="pupil" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#000"/><stop offset=".78" stop-color="#010913"/><stop offset="1" stop-color="#0A3F96"/></radialGradient>
<radialGradient id="aura" cx="50%" cy="50%" r="50%"><stop offset=".58" stop-color="#2FA8FF" stop-opacity="0"/><stop offset=".72" stop-color="#2FA8FF" stop-opacity=".38"/><stop offset=".86" stop-color="#2FA8FF" stop-opacity=".12"/><stop offset="1" stop-color="#2FA8FF" stop-opacity="0"/></radialGradient>
<radialGradient id="gloss" cx="35%" cy="28%" r="60%"><stop offset="0" stop-color="#fff" stop-opacity=".55"/><stop offset=".5" stop-color="#fff" stop-opacity=".06"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>
<clipPath id="clip"><circle cx="54" cy="54" r="{R}"/></clipPath>
<filter id="blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2.4"/></filter>
</defs>'''

BACKGROUND = '<rect width="108" height="108" fill="url(#bg)"/>'

ART = (
    f'<circle cx="54" cy="54" r="50" fill="url(#aura)"/>'
    f'<circle cx="54" cy="54" r="{R + 3}" fill="none" stroke="#4FC3FF" stroke-width="3" opacity=".55" filter="url(#blur)"/>'
    + arc(40, 200, 340, "#7FD6FF", 1.6, .8) + arc(40, 20, 160, "#7FD6FF", 1.6, .8)
    + arc(44, 230, 310, "#2FA8FF", .8, .5) + arc(44, 50, 130, "#2FA8FF", .8, .5)
    + '<circle cx="54" cy="12.5" r="1.3" fill="#BFE9FF"/><circle cx="54" cy="95.5" r="1.3" fill="#BFE9FF"/>'
    f'<circle cx="54" cy="54" r="{R}" fill="url(#iris)"/>'
    f'<g clip-path="url(#clip)">{fibres(230, 9, R, BLUES, 3, .35, .8)}{fibres(90, 15, R, DEEP, 9, .6, 1.1, .02, .35, .7)}</g>'
    + collarette(14.5, 1.3, 22, "#BFE9FF", .7, .75)
    + f'<circle cx="54" cy="54" r="{R}" fill="none" stroke="#031B46" stroke-width="2.6"/>'
    f'<circle cx="54" cy="54" r="{R - 1.6}" fill="none" stroke="#5FC8FF" stroke-width=".6" opacity=".5"/>'
    '<circle cx="54" cy="54" r="11" fill="url(#pupil)"/>'
    f'<circle cx="54" cy="54" r="{R}" fill="url(#gloss)"/>'
    '<ellipse cx="44.5" cy="43.5" rx="5.6" ry="3.6" fill="#fff" opacity=".95" transform="rotate(-34 44.5 43.5)"/>'
    '<circle cx="62.5" cy="63" r="2.1" fill="#fff" opacity=".75"/>'
)


def svg(body):
    return f'<svg viewBox="0 0 108 108" width="108" height="108" xmlns="http://www.w3.org/2000/svg">{DEFS}{body}</svg>'


def scaled(body, k):
    return f'<g transform="translate(54 54) scale({k:.5f}) translate(-54 -54)">{body}</g>'


MASTER = svg(BACKGROUND + ART)
FOREGROUND = svg(scaled(ART, SAFE))          # transparent field
BACKGROUND_ONLY = svg(BACKGROUND)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


# ---- vectors: the simplified mark for the themed icon and the status bar ----------------
def mark_paths(k, colour):
    """Rim, pupil and the two inner arcs, scaled by k about the centre, in one colour."""
    def s(v):
        return 54 + (v - 54) * k
    r_rim, r_pupil, r_arc = R * k, 11 * k, 40 * k
    rim = (f'<path android:pathData="M{s(54 - R):.2f},54 a{r_rim:.2f},{r_rim:.2f} 0 1,0 {2 * r_rim:.2f},0 '
           f'a{r_rim:.2f},{r_rim:.2f} 0 1,0 {-2 * r_rim:.2f},0" android:strokeColor="{colour}" '
           f'android:strokeWidth="{4.5 * k:.2f}" android:fillColor="#00000000"/>')
    pupil = (f'<path android:pathData="M{s(54 - 11):.2f},54 a{r_pupil:.2f},{r_pupil:.2f} 0 1,0 {2 * r_pupil:.2f},0 '
             f'a{r_pupil:.2f},{r_pupil:.2f} 0 1,0 {-2 * r_pupil:.2f},0" android:fillColor="{colour}"/>')
    arcs = ""
    for a0, a1 in ((200, 340), (20, 160)):
        d = arc_d(r_arc, math.radians(a0), math.radians(a1)).replace("M54", "M54")
        # arc_d draws about (54,54) at radius r_arc already scaled
        arcs += (f'<path android:pathData="{d}" android:strokeColor="{colour}" '
                 f'android:strokeWidth="{3.2 * k:.2f}" android:strokeLineCap="round" android:fillColor="#00000000"/>')
    return rim + pupil + arcs


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: the eye reduced to its rim, pupil and two arcs, in one colour the\n"
              "         launcher tints. Built by store-assets/icon/build_icon.py; do not edit by hand. -->\n"
              + mark_paths(SAFE, "#FF000000") + "\n</vector>\n")

NOTIFICATION = (VECTOR_HEAD.format(w=24)
                + "    <!-- Status-bar glyph: white on transparent, the same reduced mark, filling the canvas.\n"
                "         Built by store-assets/icon/build_icon.py. -->\n"
                + mark_paths(1.0, "#FFFFFFFF") + "\n</vector>\n")

ADAPTIVE = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            '</adaptive-icon>\n')

FEATURE = f'''<!doctype html><html><head><meta charset="utf-8"><style>
body{{margin:0;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 50%,#0A1830 0%,#050B18 55%,#02050C 100%);
font-family:"Space Grotesk",Inter,system-ui,sans-serif;color:#F4F1EA}}
.icon{{position:absolute;left:84px;top:94px;width:312px;height:312px;border-radius:84px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.6)}}
.icon svg{{width:100%;height:100%;display:block}}
h1{{position:absolute;left:460px;top:150px;margin:0;font-size:92px;letter-spacing:-3px;font-weight:700}}
p{{position:absolute;left:464px;top:262px;margin:0;font-size:31px;color:#A8C8E8;max-width:520px;line-height:1.3}}
.ml{{position:absolute;left:466px;top:352px;font-size:26px;color:#6FB7F0}}
</style></head><body><div class="icon">{MASTER}</div><h1>Ikshana</h1>
<p>A camera that answers, and cannot upload what it sees.</p><div class="ml">ഈക്ഷണ · ईक्षण</div></body></html>'''


def main():
    write(os.path.join(HERE, "ikshana-icon.svg"), MASTER)
    write(os.path.join(HERE, "foreground.svg"), FOREGROUND)
    write(os.path.join(HERE, "background.svg"), BACKGROUND_ONLY)
    write(os.path.join(HERE, "feature.html"), FEATURE)
    write(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), MONOCHROME)
    write(os.path.join(RES, "drawable", "ic_notification.xml"), NOTIFICATION)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), ADAPTIVE)
    write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), ADAPTIVE)

    jobs = []
    for d, k in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{d}")
        jobs.append(dict(svg="foreground.svg", size=int(108 * k), out=os.path.join(folder, "ic_launcher_foreground.png"), transparent=True))
        jobs.append(dict(svg="background.svg", size=int(108 * k), out=os.path.join(folder, "ic_launcher_background.png")))
        # legacy icons show the art at full size, cropped: the launcher masked these before API 26
        jobs.append(dict(svg="ikshana-icon.svg", size=int(48 * k), out=os.path.join(folder, "ic_launcher.png"), radius=0.2, transparent=True))
        jobs.append(dict(svg="ikshana-icon.svg", size=int(48 * k), out=os.path.join(folder, "ic_launcher_round.png"), radius=0.5, transparent=True))
    jobs.append(dict(svg="ikshana-icon.svg", size=512, out=os.path.join(ROOT, "store-assets", "icon_512.png")))
    jobs.append(dict(html="feature.html", width=1024, height=500, out=os.path.join(ROOT, "store-assets", "feature_graphic_1024x500.png")))
    with open(os.path.join(HERE, "jobs.json"), "w") as f:
        json.dump(jobs, f, indent=1)
    if "--no-png" in sys.argv:
        print("vectors and SVGs written; PNGs skipped")
        return
    subprocess.check_call(["node", os.path.join(HERE, "render.js"), os.path.join(HERE, "jobs.json")])
    os.remove(os.path.join(HERE, "jobs.json"))
    print("done")


if __name__ == "__main__":
    main()
