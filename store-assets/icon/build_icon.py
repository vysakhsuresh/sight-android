#!/usr/bin/env python3
"""Builds every Ikshana icon asset from one drawing: the black hole (owner, 12 Nov; third drawing 13 Nov,
see "Third drawing" below, which replaced the rings and spiral described next).

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

# The Play icon and the legacy rasters, which the store and the launcher crop themselves, use
# the art at full size; the adaptive foreground scales it by FG_SCALE (below).


# ---- Third drawing (owner, 13 Nov): "less cosmic rays, less powerful, no wow; the spiral white
# transparent lines make it awful." So: no rings and no spiral at all. The light now bursts
# OUT of the hole, hundreds of tapered rays white-hot at the root, through Iris to violet; a
# blazing accretion disk crosses the horizon, its near half in front, its far half lensed
# into the photon ring; behind, a violet and blue nebula with stars and four glints. -----------
HB = 13      # the horizon in the third drawing: bigger, the black has to hold its own
TILT = -14   # the disk's tilt, degrees
RAY_COLS = ["#6FD3FF", "#6FD3FF", "#2F6BFF", "#9F7BFF", "#BDE9FF"]


def burst(n, seed, r_min, r_max, w_root, hero=False):
    """Tapered rays from just outside the photon ring: a wedge, widest at its root, filled with
    a gradient white-hot at the root, coloured in the middle, gone at the tip. Lengths are
    heavy-tailed (most short, a few to the tile's edge), which reads as power, not hair."""
    random.seed(seed)
    out = []
    for i in range(n):
        a = 2 * math.pi * random.random()
        r0 = HB + random.uniform(1.5, 4.5)
        r1 = r0 + (r_max - r0) * (random.random() ** (1.4 if hero else 2.6))
        r1 = max(r1, r0 + r_min)
        w = w_root * random.uniform(.5, 1.2) * (1.5 if hero else 1)
        px, py = -math.sin(a), math.cos(a)
        x0, y0 = 54 + r0 * math.cos(a), 54 + r0 * math.sin(a)
        x1, y1 = 54 + r1 * math.cos(a), 54 + r1 * math.sin(a)
        g = f"{'h' if hero else 'r'}{i}"
        col = random.choice(RAY_COLS)
        op = random.uniform(.55, 1) if hero else random.uniform(.3, .85)
        out.append(
            f'<linearGradient id="{g}" gradientUnits="userSpaceOnUse" x1="{x0:.2f}" y1="{y0:.2f}" x2="{x1:.2f}" y2="{y1:.2f}">'
            f'<stop offset="0" stop-color="#FFFFFF" stop-opacity="{op:.2f}"/><stop offset=".25" stop-color="{col}" stop-opacity="{op * .9:.2f}"/>'
            f'<stop offset="1" stop-color="{col}" stop-opacity="0"/></linearGradient>'
            f'<path d="M{x0 + px * w / 2:.2f},{y0 + py * w / 2:.2f} L{x1:.2f},{y1:.2f} L{x0 - px * w / 2:.2f},{y0 - py * w / 2:.2f}Z" fill="url(#{g})"/>')
    return "".join(out)


def starfield(n, seed):
    random.seed(seed)
    out = []
    for _ in range(n):
        x, y = random.uniform(2, 106), random.uniform(2, 106)
        out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{random.uniform(.15, .5):.2f}" fill="#E6F2FF" opacity="{random.uniform(.2, .8):.2f}"/>')
    # four glints, the kind a lens makes of a bright star
    for x, y, s in ((19, 22, 3.2), (88, 17, 2.4), (84, 88, 2.8), (16, 84, 2.0)):
        out.append(f'<g opacity=".9"><path d="M{x - s},{y} L{x + s},{y} M{x},{y - s} L{x},{y + s}" stroke="#DDF3FF" stroke-width=".35" stroke-linecap="round"/>'
                   f'<circle cx="{x}" cy="{y}" r=".7" fill="#FFFFFF"/></g>')
    return "".join(out)


DEFS = f"""<defs>
<radialGradient id="bg" cx="50%" cy="50%" r="75%"><stop offset="0" stop-color="#0E1D55"/><stop offset=".4" stop-color="#060B26"/><stop offset="1" stop-color="#010205"/></radialGradient>
<radialGradient id="neb1" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#7A3CFF" stop-opacity=".55"/><stop offset="1" stop-color="#7A3CFF" stop-opacity="0"/></radialGradient>
<radialGradient id="neb2" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#1E7FE0" stop-opacity=".5"/><stop offset="1" stop-color="#1E7FE0" stop-opacity="0"/></radialGradient>
<radialGradient id="bloom" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#FFFFFF" stop-opacity=".95"/><stop offset=".3" stop-color="#9FE0FF" stop-opacity=".7"/><stop offset=".6" stop-color="#2F6BFF" stop-opacity=".3"/><stop offset="1" stop-color="#2F6BFF" stop-opacity="0"/></radialGradient>
<linearGradient id="disk" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#2F6BFF" stop-opacity="0"/><stop offset=".22" stop-color="#6FD3FF" stop-opacity=".85"/><stop offset=".42" stop-color="#FFFFFF"/><stop offset=".6" stop-color="#BDE9FF" stop-opacity=".95"/><stop offset=".82" stop-color="#7A5CFF" stop-opacity=".6"/><stop offset="1" stop-color="#7A3CFF" stop-opacity="0"/></linearGradient>
<filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation=".6"/></filter>
<filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.8"/></filter>
<filter id="haze" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="5"/></filter>
<clipPath id="near"><rect x="-10" y="54" width="128" height="60" transform="rotate({TILT} 54 54)"/></clipPath>
</defs>"""

BACKGROUND = (
    '<rect width="108" height="108" fill="url(#bg)"/>'
    + '<ellipse cx="30" cy="36" rx="40" ry="26" fill="url(#neb1)" transform="rotate(-25 30 36)"/>'
    + '<ellipse cx="80" cy="76" rx="42" ry="24" fill="url(#neb2)" transform="rotate(-20 80 76)"/>'
    + '<ellipse cx="86" cy="30" rx="22" ry="14" fill="url(#neb1)" opacity=".5"/>'
    + starfield(110, 11)
)


def disk(blur):
    """The accretion disk, edge-on and tilted: a hazy body and a sharp white-hot core line."""
    t = f'transform="rotate({TILT} 54 54)"'
    if blur:
        return (f'<g {t}><ellipse cx="54" cy="54" rx="48" ry="7.5" fill="url(#disk)" filter="url(#haze)"/>'
                f'<ellipse cx="54" cy="54" rx="44" ry="4.2" fill="url(#disk)" filter="url(#glow)"/>'
                f'<ellipse cx="54" cy="54" rx="40" ry="1.8" fill="url(#disk)" filter="url(#soft)"/></g>')
    # in front of the horizon only the thin hot band: the haze over the black read as a lit ball
    return (f'<g {t}><ellipse cx="54" cy="55" rx="40" ry="2.4" fill="url(#disk)" filter="url(#soft)"/>'
            f'<ellipse cx="54" cy="55" rx="34" ry=".9" fill="#FFFFFF" opacity=".9"/></g>')


ART = (
    f'<circle cx="54" cy="54" r="40" fill="url(#bloom)" opacity=".55" filter="url(#haze)"/>'
    + burst(360, 7, 3, 58, .9)
    + f'<g filter="url(#soft)">{burst(18, 23, 18, 64, 2.2, hero=True)}</g>'
    + f'<circle cx="54" cy="54" r="{HB * 2.1:.2f}" fill="url(#bloom)" filter="url(#glow)"/>'
    # the far half of the disk, behind the hole
    + disk(True)
    # the photon ring: the far disk lensed into a halo, white-hot at its inner edge
    + f'<circle cx="54" cy="54" r="{HB + 2.2}" fill="none" stroke="#6FD3FF" stroke-width="5" opacity=".9" filter="url(#glow)"/>'
    + f'<circle cx="54" cy="54" r="{HB + 1.1}" fill="none" stroke="#DDF5FF" stroke-width="2.2" filter="url(#soft)"/>'
    + f'<circle cx="54" cy="54" r="{HB + .5}" fill="none" stroke="#FFFFFF" stroke-width="1"/>'
    # the horizon, true black
    + f'<circle cx="54" cy="54" r="{HB}" fill="#000"/>'
    # the near half of the disk crosses in front of the horizon
    + f'<g clip-path="url(#near)">{disk(False)}</g>'
)

# The adaptive foreground: the launcher shows about a 72 dp circle of the 108; the rays may run
# past it (they fade), the disk and the ring must not.
FG_SCALE = .84


def svg(body):
    return f'<svg viewBox="0 0 108 108" width="108" height="108" xmlns="http://www.w3.org/2000/svg">{DEFS}{body}</svg>'


def scaled(body, k):
    return f'<g transform="translate(54 54) scale({k:.5f}) translate(-54 -54)">{body}</g>'


MASTER = svg(BACKGROUND + ART)
FOREGROUND = svg(scaled(ART, FG_SCALE))      # transparent field
BACKGROUND_ONLY = svg(BACKGROUND)


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


# ---- vectors: the simplified mark for the themed icon and the status bar ----------------
def mark_paths(k, colour):
    """The reduced mark in one colour, scaled by k about the centre: the horizon filled, the
    photon ring around it, and the disk as a tilted bar through both (no rings down a tunnel,
    which the owner disliked on 13 Nov)."""
    def circle(r):
        rr = r * k
        return f"M{54 - rr:.2f},54 a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0"
    out = (f'<path android:pathData="{circle(HB + 6)}" android:strokeColor="{colour}" '
           f'android:strokeWidth="{3.2 * k:.2f}" android:fillColor="#00000000"/>')
    out += f'<path android:pathData="{circle(HB)}" android:fillColor="{colour}"/>'
    half, h = 44 * k, 2.6 * k
    out += (f'<group android:pivotX="54" android:pivotY="54" android:rotation="{TILT}">'
            f'<path android:pathData="M{54 - half:.2f},{54 - h / 2:.2f} h{2 * half:.2f} v{h:.2f} h{-2 * half:.2f}z" android:fillColor="{colour}"/></group>')
    return out


VECTOR_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="{w}dp" android:height="{w}dp" android:viewportWidth="108" android:viewportHeight="108">\n')

MONOCHROME = (VECTOR_HEAD.format(w=108)
              + "    <!-- The themed icon: the eye reduced to its rim, pupil and two arcs, in one colour the\n"
              "         launcher tints. Built by store-assets/icon/build_icon.py; do not edit by hand. -->\n"
              + mark_paths(FG_SCALE * .9, "#FF000000") + "\n</vector>\n")

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
