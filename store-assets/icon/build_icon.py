#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the beam into the black hole (owner, 12 Nov).

Second redraw the same day. The owner rejected the almond eye ("I don't want the eyelid") and
asked for the iris and the black hole as one thing, "a deep immersive beam going to the universe
when looked from the centre of its flight". So the mark is seen from inside the beam: Iris-blue
streaks of light rush inward in a slight spiral (the iris's fibres and starlight stretched by
speed at once), rings recede down the tunnel in perspective, and at its end the pupil is a black
hole with a white-hot photon ring. No outline, no lid: the whole tile is the eye.
Generated, not traced. The same geometry, drawn once here, becomes:

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
simplified mark (rings down the beam, the horizon) that reads at 24 dp.
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

BLUES = ["#E6F7FF", "#BDE9FF", "#6FD3FF", "#2FA8FF", "#1E7FE0"]
DEEP = ["#0B57B8", "#083A7A", "#1E7FE0"]
R = 30     # outer edge of the accretion disk (the iris)
H = 11     # event horizon (the pupil), small: far down the beam


def swirl(n, r0, r1, cols, seed, twist=0.9, wmin=.35, wmax=.9):
    """Spiral streams from the disk's edge falling toward the horizon: each a quadratic curve
    turning by `twist` radians as it falls, with opacity following (1 - cos a) / 2 so the left
    side, coming toward us, is brighter than the right."""
    random.seed(seed)
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n + random.uniform(-.03, .03)
        ra = r1 - random.uniform(0, 3.5)
        rb = r0 + random.uniform(0, 1.5)
        tw = twist * random.uniform(.7, 1.15)
        am = a + tw * .55
        rm = (ra + rb) / 2 + random.uniform(.5, 2.5)
        x0, y0 = 54 + ra * math.cos(a), 54 + ra * math.sin(a)
        x1, y1 = 54 + rb * math.cos(a + tw), 54 + rb * math.sin(a + tw)
        cx, cy = 54 + rm * math.cos(am), 54 + rm * math.sin(am)
        beam = .25 + .75 * (1 - math.cos(a + tw / 2)) / 2
        op = random.uniform(.45, .95) * beam
        out.append(f'<path d="M{x0:.2f},{y0:.2f} Q{cx:.2f},{cy:.2f} {x1:.2f},{y1:.2f}" fill="none" '
                   f'stroke="{random.choice(cols)}" stroke-width="{random.uniform(wmin, wmax):.2f}" '
                   f'opacity="{op:.2f}" stroke-linecap="round"/>')
    return "".join(out)


def stars(n, seed):
    random.seed(seed)
    out = []
    for _ in range(n):
        x, y = random.uniform(4, 104), random.uniform(4, 104)
        if (x - 54) ** 2 + ((y - 54) * 2.2) ** 2 < 48 ** 2:
            continue   # none inside the eye
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{random.uniform(.25, .6):.2f}" fill="#CFEFFF" opacity="{random.uniform(.25, .7):.2f}"/>')
    return "".join(out)


def streaks(n, seed):
    """Light rushing inward: each streak runs from far out toward the horizon, turning slightly
    (the spiral of a vortex), brighter and wider at its outer end, so the eye reads depth."""
    random.seed(seed)
    out = []
    for i in range(n):
        a = 2 * math.pi * random.random()
        r_out = random.uniform(24, 62)
        r_in = max(H + 2.5, r_out * random.uniform(.28, .6))
        tw = .35 * (r_out - r_in) / 40
        x0, y0 = 54 + r_out * math.cos(a), 54 + r_out * math.sin(a)
        x1, y1 = 54 + r_in * math.cos(a + tw), 54 + r_in * math.sin(a + tw)
        g = f"s{i}"
        col = random.choice(BLUES)
        op = random.uniform(.35, .95)
        out.append(f'<linearGradient id="{g}" gradientUnits="userSpaceOnUse" x1="{x0:.2f}" y1="{y0:.2f}" x2="{x1:.2f}" y2="{y1:.2f}">'
                   f'<stop offset="0" stop-color="{col}" stop-opacity="{op:.2f}"/><stop offset="1" stop-color="{col}" stop-opacity="0"/></linearGradient>'
                   f'<path d="M{x0:.2f},{y0:.2f} L{x1:.2f},{y1:.2f}" stroke="url(#{g})" stroke-width="{random.uniform(.3, 1.3) * r_out / 40:.2f}" stroke-linecap="round"/>')
    return "".join(out)


def tunnel(k, ratio=.8):
    """Rings receding down the beam: each one ratio times the last, fainter as it nears the hole."""
    out, r, i = [], 52.0, 0
    while r > H + 1.5 and i < k:
        op = .08 + .32 * (r / 52)
        out.append(f'<circle cx="54" cy="54" r="{r:.2f}" fill="none" stroke="#6FD3FF" stroke-width="{.25 + .5 * r / 52:.2f}" opacity="{op:.2f}"/>')
        r *= ratio; i += 1
    return "".join(out)


DEFS = f"""<defs>
<radialGradient id="bg" cx="50%" cy="50%" r="72%"><stop offset="0" stop-color="#0D2A66"/><stop offset=".35" stop-color="#061433"/><stop offset=".75" stop-color="#020611"/><stop offset="1" stop-color="#000"/></radialGradient>
<radialGradient id="core" cx="50%" cy="50%" r="50%"><stop offset=".45" stop-color="#6FD3FF" stop-opacity="0"/><stop offset=".62" stop-color="#9FDBFF" stop-opacity=".9"/><stop offset=".75" stop-color="#2FA8FF" stop-opacity=".45"/><stop offset="1" stop-color="#1E7FE0" stop-opacity="0"/></radialGradient>
<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.6"/></filter>
<filter id="haze" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
</defs>"""

BACKGROUND = '<rect width="108" height="108" fill="url(#bg)"/>'

ART = (
    tunnel(9)
    + streaks(170, 5)
    # the light piling up at the edge of the horizon, then the white-hot photon ring
    + f'<circle cx="54" cy="54" r="{H * 1.9:.2f}" fill="url(#core)" filter="url(#haze)"/>'
    + f'<circle cx="54" cy="54" r="{H + 1.4}" fill="none" stroke="#BDE9FF" stroke-width="3" opacity=".7" filter="url(#glow)"/>'
    + f'<circle cx="54" cy="54" r="{H + .8}" fill="none" stroke="#FFFFFF" stroke-width="1.1"/>'
    # the pupil: the horizon, true black
    + f'<circle cx="54" cy="54" r="{H}" fill="#000"/>'
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
    """Three rings down the beam and the horizon, scaled by k about the centre, one colour."""
    out = ""
    for r, w in ((44, 3.2), (31, 3.0), (H + 4, 3.0)):
        rr = r * k
        out += (f'<path android:pathData="M{54 - rr:.2f},54 a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 '
                f'a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0" android:strokeColor="{colour}" '
                f'android:strokeWidth="{w * k:.2f}" android:fillColor="#00000000"/>')
    rh = H * k
    out += (f'<path android:pathData="M{54 - rh:.2f},54 a{rh:.2f},{rh:.2f} 0 1,0 {2 * rh:.2f},0 '
            f'a{rh:.2f},{rh:.2f} 0 1,0 {-2 * rh:.2f},0" android:fillColor="{colour}"/>')
    return out


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
@font-face{{font-family:"Space Grotesk";font-weight:700;src:url("../../app/src/main/res/font/space_grotesk_bold.ttf")}}
@font-face{{font-family:"Space Grotesk";font-weight:400;src:url("../../app/src/main/res/font/space_grotesk_regular.ttf")}}
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
    import shutil
    if shutil.which("node"):
        subprocess.check_call(["node", os.path.join(HERE, "render.js"), os.path.join(HERE, "jobs.json")])
    else:
        # The owner's laptop has Edge and Pillow but no Node (12 Nov 2026).
        subprocess.check_call([sys.executable, os.path.join(HERE, "render_edge.py"), os.path.join(HERE, "jobs.json")])
    os.remove(os.path.join(HERE, "jobs.json"))
    print("done")


if __name__ == "__main__":
    main()
