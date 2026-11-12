#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the black-hole eye (owner, 12 Nov 2026).

The owner asked for the iris redrawn as "a black hole iris combination": the earlier blue eye
with HUD arcs read as a local app. The mark is now a black hole seen as an eye. The pupil is an
event horizon (true black) inside a white-hot photon ring; the iris is the accretion disk, 260
spiral streams of Iris blue falling inward, brighter on the left as a real disk is (Doppler
beaming); the almond of the eye is two luminous streams meeting at the corners; and a thin front
disk crosses the lower edge of the hole, the signature every picture of a black hole carries.
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
simplified mark (almond, photon ring, horizon) that reads at 24 dp.
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
H = 15.5   # event horizon (the pupil)


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


def arc_d(r, a0, a1, ry=None):
    ry = r if ry is None else ry
    x0, y0 = 54 + r * math.cos(a0), 54 + ry * math.sin(a0)
    x1, y1 = 54 + r * math.cos(a1), 54 + ry * math.sin(a1)
    large = 1 if (a1 - a0) > math.pi + 1e-9 else 0
    return f"M{x0:.2f},{y0:.2f} A{r},{ry} 0 {large} 1 {x1:.2f},{y1:.2f}"


ALMOND = "M7,54 C26,22 82,22 101,54 C82,86 26,86 7,54 Z"

DEFS = f"""<defs>
<radialGradient id="bg" cx="50%" cy="50%" r="72%"><stop offset="0" stop-color="#071226"/><stop offset=".55" stop-color="#03070F"/><stop offset="1" stop-color="#010205"/></radialGradient>
<radialGradient id="sclera" cx="50%" cy="50%" r="50%"><stop offset=".3" stop-color="#0B2A55" stop-opacity=".55"/><stop offset="1" stop-color="#0B2A55" stop-opacity="0"/></radialGradient>
<radialGradient id="disk" cx="50%" cy="50%" r="50%"><stop offset=".5" stop-color="#1E7FE0" stop-opacity="0"/><stop offset=".62" stop-color="#2FA8FF" stop-opacity=".55"/><stop offset=".8" stop-color="#0B57B8" stop-opacity=".35"/><stop offset="1" stop-color="#062A5C" stop-opacity="0"/></radialGradient>
<linearGradient id="beam" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#FFFFFF"/><stop offset=".35" stop-color="#BDE9FF"/><stop offset=".7" stop-color="#2FA8FF"/><stop offset="1" stop-color="#0B57B8"/></linearGradient>
<linearGradient id="lid" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#6FD3FF" stop-opacity=".2"/><stop offset=".3" stop-color="#BDE9FF"/><stop offset=".7" stop-color="#6FD3FF"/><stop offset="1" stop-color="#2FA8FF" stop-opacity=".2"/></linearGradient>
<clipPath id="eye"><path d="{ALMOND}"/></clipPath>
<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.6"/></filter>
<filter id="haze" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3.2"/></filter>
</defs>"""

BACKGROUND = '<rect width="108" height="108" fill="url(#bg)"/>' + stars(26, 11)

ART = (
    f'<path d="{ALMOND}" fill="url(#sclera)"/>'
    f'<path d="{ALMOND}" fill="none" stroke="#2FA8FF" stroke-width="3" opacity=".45" filter="url(#haze)"/>'
    f'<path d="{ALMOND}" fill="none" stroke="url(#lid)" stroke-width="1.5"/>'
    '<g clip-path="url(#eye)">'
    f'<circle cx="54" cy="54" r="{R + 4}" fill="url(#disk)"/>'
    f'<path d="{arc_d(40, math.pi, 2 * math.pi, 7)}" fill="none" stroke="url(#beam)" stroke-width="2" opacity=".55"/>'
    + swirl(190, H + .5, R, BLUES, 3) + swirl(70, H + 2, R - 2, DEEP, 9, 1.3, .6, 1.2) +
    f'<circle cx="54" cy="54" r="{H + 1.6}" fill="none" stroke="#9FDBFF" stroke-width="3.2" opacity=".6" filter="url(#glow)"/>'
    f'<circle cx="54" cy="54" r="{H + .9}" fill="none" stroke="#F2FBFF" stroke-width="1.1"/>'
    f'<circle cx="54" cy="54" r="{H}" fill="#000"/>'
    f'<path d="{arc_d(40, 0, math.pi, 7)}" fill="none" stroke="#6FD3FF" stroke-width="3.4" opacity=".35" filter="url(#glow)"/>'
    f'<path d="{arc_d(40, 0, math.pi, 7)}" fill="none" stroke="url(#beam)" stroke-width="1.6"/>'
    '</g>'
    '<ellipse cx="45.5" cy="44" rx="3.2" ry="1.9" fill="#fff" opacity=".85" transform="rotate(-30 45.5 44)"/>'
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
    """The almond, the photon ring and the horizon, scaled by k about the centre, one colour."""
    def s(v):
        return 54 + (v - 54) * k

    def pt(x, y):
        return f"{s(x):.2f},{s(y):.2f}"
    almond = (f'<path android:pathData="M{pt(7, 54)} C{pt(26, 22)} {pt(82, 22)} {pt(101, 54)} '
              f'C{pt(82, 86)} {pt(26, 86)} {pt(7, 54)} Z" android:strokeColor="{colour}" '
              f'android:strokeWidth="{4 * k:.2f}" android:fillColor="#00000000"/>')
    rr = (H + 3.5) * k
    ring = (f'<path android:pathData="M{54 - rr:.2f},54 a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 '
            f'a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0" android:strokeColor="{colour}" '
            f'android:strokeWidth="{3 * k:.2f}" android:fillColor="#00000000"/>')
    rh = H * .8 * k
    hole = (f'<path android:pathData="M{54 - rh:.2f},54 a{rh:.2f},{rh:.2f} 0 1,0 {2 * rh:.2f},0 '
            f'a{rh:.2f},{rh:.2f} 0 1,0 {-2 * rh:.2f},0" android:fillColor="{colour}"/>')
    return almond + ring + hole


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
